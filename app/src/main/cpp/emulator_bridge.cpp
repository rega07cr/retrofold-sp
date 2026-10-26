// RetroFold SP — bridge JNI verso il core mGBA 0.10.5 (compilato da sorgente).
// BIOS integrato di mGBA: nessuna BIOS esterna richiesta.
//
// Threading: un thread emu dedicato chiama runFrame in loop con pacing
// 59.73fps; video su ANativeWindow (swizzle BGRx->RGBA), audio via AAudio
// 48kHz (decimazione 2:1 dal blip a 96kHz), input via setKeys atomico,
// stati + rewind sotto mutex. I VFile ROM/save restano aperti per sessione.

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <aaudio/AAudio.h>
#include <pthread.h>
#include <atomic>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <vector>

#include <mgba/core/core.h>
#include <mgba/core/blip_buf.h>
#include <mgba/internal/gba/input.h>

#define LOG_TAG "RetroFoldGBA"

static const unsigned GBA_FPS_US = 16743; // 59.7275fps
static const int AUDIO_RATE = 48000;
static const int RING_FRAMES = 48000; // 1s stereo

struct AudioRing {
    int16_t buf[RING_FRAMES * 2];
    std::atomic<int> head{0};
    std::atomic<int> tail{0};
    void push(int16_t l, int16_t r) {
        int h = head.load(std::memory_order_relaxed);
        int t = tail.load(std::memory_order_acquire);
        int next = (h + 1) % RING_FRAMES;
        if (next == t) return; // pieno: scarta (meglio un glitch che un blocco)
        buf[h * 2] = l;
        buf[h * 2 + 1] = r;
        head.store(next, std::memory_order_release);
    }
    int pop(int16_t* out, int frames) {
        int t = tail.load(std::memory_order_relaxed);
        int h = head.load(std::memory_order_acquire);
        int avail = (h - t + RING_FRAMES) % RING_FRAMES;
        int n = avail < frames ? avail : frames;
        for (int i = 0; i < n; ++i) {
            out[i * 2] = buf[t * 2];
            out[i * 2 + 1] = buf[t * 2 + 1];
            t = (t + 1) % RING_FRAMES;
        }
        tail.store(t, std::memory_order_release);
        return n;
    }
};

struct Emu {
    mCore* core = nullptr;
    color_t* pixels = nullptr;
    unsigned vw = 240, vh = 160;
    pthread_t thread{};
    bool threadOn = false;
    std::atomic<bool> running{false};
    std::atomic<bool> paused{true};
    std::atomic<int> fastForward{1};
    std::atomic<uint32_t> keys{0};
    pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
    ANativeWindow* window = nullptr;
    pthread_mutex_t winLock = PTHREAD_MUTEX_INITIALIZER;
    // rewind: snapshot ogni 10 frame, max ~12s
    std::vector<uint8_t*> rewind;
    size_t stateSize = 0;
    int frameNo = 0;
    AudioRing ring;
    AAudioStream* astream = nullptr;
    VFile* romVf = nullptr;
    VFile* saveVf = nullptr;
};

static Emu g;

static void blitLocked() {
    pthread_mutex_lock(&g.winLock);
    ANativeWindow* win = g.window;
    if (win) ANativeWindow_acquire(win);
    pthread_mutex_unlock(&g.winLock);
    if (!win) return;
    ANativeWindow_Buffer buf;
    if (ANativeWindow_lock(win, &buf, nullptr) == 0) {
        // Nearest-neighbor + swizzle BGRx(0x00BBGGRR) -> RGBA8888
        auto* dst = static_cast<uint32_t*>(buf.bits);
        int32_t bw = buf.stride;
        int32_t bh = buf.height;
        for (int32_t y = 0; y < bh; ++y) {
            unsigned sy = (unsigned)((int64_t)y * g.vh / (bh ? bh : 1));
            for (int32_t x = 0; x < bw; ++x) {
                unsigned sx = (unsigned)((int64_t)x * g.vw / (bw ? bw : 1));
                color_t c = g.pixels[sy * g.vw + sx];
                uint32_t r = c & 0xFFu;
                uint32_t gg = (c >> 8) & 0xFFu;
                uint32_t b = (c >> 16) & 0xFFu;
                dst[y * bw + x] = 0xFF000000u | (r << 16) | (gg << 8) | b;
            }
        }
        ANativeWindow_unlockAndPost(win);
    }
    ANativeWindow_release(win);
}

static void drainAudio() {
    if (!g.core) return;
    blip_t* bl = g.core->getAudioChannel(g.core, 0);
    blip_t* br = g.core->getAudioChannel(g.core, 1);
    if (!bl || !br) return;
    int avail = blip_samples_avail(bl);
    int availR = blip_samples_avail(br);
    if (availR < avail) avail = availR;
    static int16_t tmpL[2048], tmpR[2048];
    while (avail > 0) {
        int n = avail > 2048 ? 2048 : avail;
        blip_read_samples(bl, tmpL, n, 0);
        blip_read_samples(br, tmpR, n, 0);
        for (int i = 0; i < n; i += 2) g.ring.push(tmpL[i], tmpR[i]); // 96k -> 48k
        avail -= n;
    }
}

static void* emuLoop(void*) {
    struct timespec ts;
    while (g.running.load()) {
        if (g.paused.load() || !g.core) {
            struct timespec s = {0, 10 * 1000 * 1000};
            nanosleep(&s, nullptr);
            continue;
        }
        clock_gettime(CLOCK_MONOTONIC, &ts);
        int64_t t0 = (int64_t)ts.tv_sec * 1000000 + ts.tv_nsec / 1000;
        pthread_mutex_lock(&g.lock);
        int n = g.fastForward.load();
        if (n < 1) n = 1;
        if (n > 8) n = 8;
        for (int i = 0; i < n; ++i) {
            g.core->setKeys(g.core, g.keys.load());
            g.core->runFrame(g.core);
        }
        drainAudio();
        // snapshot rewind ogni 10 frame
        if (++g.frameNo % 10 == 0 && g.stateSize > 0) {
            if (g.rewind.size() >= 72) {
                free(g.rewind.front());
                g.rewind.erase(g.rewind.begin());
            }
            uint8_t* s = static_cast<uint8_t*>(malloc(g.stateSize));
            if (s && g.core->saveState(g.core, s)) g.rewind.push_back(s);
            else free(s);
        }
        pthread_mutex_unlock(&g.lock);
        blitLocked();
        clock_gettime(CLOCK_MONOTONIC, &ts);
        int64_t dt = (int64_t)ts.tv_sec * 1000000 + ts.tv_nsec / 1000 - t0;
        int64_t wait = (int64_t)GBA_FPS_US - dt;
        if (wait > 0 && n == 1) {
            struct timespec s = {(time_t)(wait / 1000000), (long)((wait % 1000000) * 1000)};
            nanosleep(&s, nullptr);
        }
    }
    return nullptr;
}

static aaudio_data_callback_result_t audioCb(
    AAudioStream*, void*, void* audioData, int32_t numFrames) {
    auto* out = static_cast<int16_t*>(audioData);
    int got = g.ring.pop(out, numFrames);
    if (got < numFrames) {
        memset(out + got * 2, 0, (size_t)(numFrames - got) * 4);
    }
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

static void audioStart() {
    if (g.astream) return;
    AAudioStreamBuilder* b = nullptr;
    if (AAudio_createStreamBuilder(&b) != AAUDIO_OK) return;
    AAudioStreamBuilder_setSampleRate(b, AUDIO_RATE);
    AAudioStreamBuilder_setChannelCount(b, 2);
    AAudioStreamBuilder_setFormat(b, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setUsage(b, AAUDIO_USAGE_GAME);
    AAudioStreamBuilder_setPerformanceMode(b, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    AAudioStreamBuilder_setDataCallback(b, audioCb, nullptr);
    AAudioStream* st = nullptr;
    if (AAudioStreamBuilder_openStream(b, &st) == AAUDIO_OK) {
        g.astream = st;
        AAudioStream_requestStart(st);
    }
    AAudioStreamBuilder_delete(b);
}

static void audioStop() {
    if (!g.astream) return;
    AAudioStream_requestStop(g.astream);
    AAudioStream_close(g.astream);
    g.astream = nullptr;
}

static void unloadLocked() {
    g.running.store(false);
    if (g.threadOn) {
        pthread_join(g.thread, nullptr);
        g.threadOn = false;
    }
    audioStop();
    if (g.core) {
        g.core->unloadROM(g.core);
        g.core->deinit(g.core);
        // NB: mCoreCreate alloca il core; non esiste mCoreFree pubblico,
        // la struct viene ricreata a ogni load (leak di pochi KB solo
        // cambiando gioco più volte nella stessa sessione).
        g.core = nullptr;
    }
    if (g.romVf) { g.romVf->close(g.romVf); g.romVf = nullptr; }
    if (g.saveVf) { g.saveVf->close(g.saveVf); g.saveVf = nullptr; }
    free(g.pixels);
    g.pixels = nullptr;
    for (auto* s : g.rewind) free(s);
    g.rewind.clear();
    g.frameNo = 0;
    g.keys.store(0);
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeLoadRom(
    JNIEnv* env, jobject, jstring romPath_, jint platform, jstring batteryPath_) {
    const char* romPath = env->GetStringUTFChars(romPath_, nullptr);
    const char* batteryPath = env->GetStringUTFChars(batteryPath_, nullptr);
    pthread_mutex_lock(&g.lock);
    unloadLocked();
    bool ok = false;
    mCore* core = mCoreCreate(platform == 0 ? mPLATFORM_GBA : mPLATFORM_GB);
    if (core) {
        mCoreInitConfig(core, "retrofold");
        mCoreLoadConfig(core);
        if (core->init(core)) {
            unsigned w = 240, h = 160;
            core->desiredVideoDimensions(core, &w, &h);
            g.vw = w;
            g.vh = h;
            g.pixels = static_cast<color_t*>(malloc((size_t)w * h * sizeof(color_t)));
            if (g.pixels) {
                memset(g.pixels, 0, (size_t)w * h * sizeof(color_t));
                core->setVideoBuffer(core, g.pixels, w);
                VFile* rom = VFileOpen(romPath, O_RDONLY);
                if (rom) {
                    if (core->loadROM(core, rom)) {
                        g.romVf = rom; // resta aperto per la sessione
                        VFile* save = VFileOpen(
                            batteryPath, O_RDWR | O_CREAT);
                        if (save) {
                            // Carica il battery save solo se non vuoto
                            if (save->size(save) > 0) core->loadSave(core, save);
                            g.saveVf = save; // resta aperto: le scritture persistono
                        }
                        mRTCGenericSourceInit(&core->rtc, core);
                        core->reset(core);
                        g.stateSize = core->stateSize(core);
                        g.core = core;
                        ok = true;
                    } else {
                        rom->close(rom);
                    }
                }
            }
        }
        if (!ok) {
            // pulizia parziale in caso di errore
            if (g.romVf) { g.romVf->close(g.romVf); g.romVf = nullptr; }
            if (g.saveVf) { g.saveVf->close(g.saveVf); g.saveVf = nullptr; }
            free(g.pixels);
            g.pixels = nullptr;
        }
    }
    pthread_mutex_unlock(&g.lock);
    env->ReleaseStringUTFChars(romPath_, romPath);
    env->ReleaseStringUTFChars(batteryPath_, batteryPath);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetSurface(
    JNIEnv* env, jobject, jobject surface) {
    pthread_mutex_lock(&g.winLock);
    if (g.window) {
        ANativeWindow_release(g.window);
        g.window = nullptr;
    }
    if (surface) {
        g.window = ANativeWindow_fromSurface(env, surface);
        if (g.window && g.vw && g.vh) {
            ANativeWindow_setBuffersGeometry(
                g.window, (int32_t)g.vw, (int32_t)g.vh, WINDOW_FORMAT_RGBA_8888);
        }
    }
    pthread_mutex_unlock(&g.winLock);
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeStart(JNIEnv*, jobject) {
    if (!g.core || g.threadOn) {
        if (g.core) g.paused.store(false);
        return;
    }
    g.paused.store(false);
    g.running.store(true);
    audioStart();
    if (pthread_create(&g.thread, nullptr, emuLoop, nullptr) == 0) {
        g.threadOn = true;
    } else {
        g.running.store(false);
    }
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativePause(JNIEnv*, jobject) {
    g.paused.store(true);
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeResume(JNIEnv*, jobject) {
    if (g.core) {
        if (!g.astream) audioStart();
        g.paused.store(false);
    }
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeReset(JNIEnv*, jobject) {
    pthread_mutex_lock(&g.lock);
    if (g.core) {
        g.keys.store(0);
        for (auto* s : g.rewind) free(s);
        g.rewind.clear();
        g.core->reset(g.core);
    }
    pthread_mutex_unlock(&g.lock);
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativePress(JNIEnv*, jobject, jint mask) {
    uint32_t k = g.keys.load(std::memory_order_relaxed);
    g.keys.store(k | (uint32_t)mask, std::memory_order_relaxed);
    // Applica subito se possibile (il loop lo rilegge comunque ogni frame)
    if (g.core && !g.paused.load()) g.core->addKeys(g.core, (uint32_t)mask);
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeRelease(JNIEnv*, jobject, jint mask) {
    uint32_t k = g.keys.load(std::memory_order_relaxed);
    g.keys.store(k & ~(uint32_t)mask, std::memory_order_relaxed);
    if (g.core && !g.paused.load()) g.core->clearKeys(g.core, (uint32_t)mask);
}

static VFile* openStateFile(const char* path, bool write) {
    if (write) {
        // VFileOpen non garantisce O_CREAT: tocca prima il file via stdio.
        FILE* f = fopen(path, "ab");
        if (f) fclose(f);
        return VFileOpen(path, O_RDWR);
    }
    return VFileOpen(path, O_RDONLY);
}

JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSaveState(
    JNIEnv* env, jobject, jstring path_) {
    const char* path = env->GetStringUTFChars(path_, nullptr);
    bool ok = false;
    pthread_mutex_lock(&g.lock);
    if (g.core) {
        VFile* vf = openStateFile(path, true);
        if (vf) {
            ok = mCoreSaveStateNamed(g.core, vf, SAVESTATE_ALL);
            vf->close(vf);
        }
    }
    pthread_mutex_unlock(&g.lock);
    env->ReleaseStringUTFChars(path_, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeLoadState(
    JNIEnv* env, jobject, jstring path_) {
    const char* path = env->GetStringUTFChars(path_, nullptr);
    bool ok = false;
    pthread_mutex_lock(&g.lock);
    if (g.core) {
        VFile* vf = openStateFile(path, false);
        if (vf) {
            ok = mCoreLoadStateNamed(g.core, vf, SAVESTATE_ALL);
            vf->close(vf);
        }
    }
    pthread_mutex_unlock(&g.lock);
    env->ReleaseStringUTFChars(path_, path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetFilterAGS101(
    JNIEnv*, jobject, jboolean) {
    // Lo shader AGS-101 è l'overlay Compose sopra la SurfaceView.
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetFastForward(
    JNIEnv*, jobject, jint mult) {
    int m = (int)mult;
    if (m < 1) m = 1;
    if (m > 8) m = 8;
    g.fastForward.store(m);
}

JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeRewind(
    JNIEnv*, jobject, jint seconds) {
    pthread_mutex_lock(&g.lock);
    if (g.core && !g.rewind.empty()) {
        // Snapshot ogni 10 frame = ~6/s: torna indietro di sec*6 snapshot.
        int back = (int)seconds * 6;
        int idx = (int)g.rewind.size() - 1 - back;
        if (idx < 0) idx = 0;
        g.core->loadState(g.core, g.rewind[(size_t)idx]);
        g.rewind.resize((size_t)idx);
    }
    pthread_mutex_unlock(&g.lock);
}

} // extern "C"
