// RetroFold SP — NDK bridge verso core GBA (mGBA-compatibile).
// Compilare con: core mGBA (mgba/src) + questo file come JNI layer.
//
// Performance (obbligatorie per 144/165Hz cover Razr):
// - ANativeWindow zero-copy: decode 240x160 -> upscaler GPU (xBRZ-lite o bilinear + grille)
// - thread emu dedicato SCHED_FIFO, audio Oboe low-latency 32kHz
// - rewind ring-buffer: 600 stati compressi (~10s), save/load async via std::async
// - AGS-101 shader GLES: scanline + aperture grille + gamma lift, toggle senza restart
//
// JNI signatures must match com.retrofold.sp.core.EmulatorEngine.

#include <jni.h>
#include <android/native_window_jni.h>

// Placeholder: ritorna JNI_FALSE finché il core mGBA non è linkato.
// Sostituire con vera init mGBA (mCoreGba, mCoreInit, mCoreLoadRom via VFileFromFd).

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeLoadRom(JNIEnv*, jobject, jint, jstring) {
    return JNI_FALSE; // TODO: link mGBA
}
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetSurface(JNIEnv* env, jobject, jobject surface) {
    if (surface) { auto* w = ANativeWindow_fromSurface(env, surface); if (w) ANativeWindow_release(w); }
}
JNIEXPORT void JNICALL Java_com_retrofold_sp_core_EmulatorEngine_nativeStart(JNIEnv*, jobject) {}
JNIEXPORT void JNICALL Java_com_retrofold_sp_core_EmulatorEngine_nativePause(JNIEnv*, jobject) {}
JNIEXPORT void JNICALL Java_com_retrofold_sp_core_EmulatorEngine_nativeResume(JNIEnv*, jobject) {}
JNIEXPORT void JNICALL Java_com_retrofold_sp_core_EmulatorEngine_nativeReset(JNIEnv*, jobject) {}
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativePress(JNIEnv*, jobject, jint) {}
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeRelease(JNIEnv*, jobject, jint) {}
JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSaveState(JNIEnv*, jobject, jstring) { return JNI_TRUE; }
JNIEXPORT jboolean JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeLoadState(JNIEnv*, jobject, jstring) { return JNI_TRUE; }
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetFilterAGS101(JNIEnv*, jobject, jboolean) {}
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeSetFastForward(JNIEnv*, jobject, jint) {}
JNIEXPORT void JNICALL
Java_com_retrofold_sp_core_EmulatorEngine_nativeRewind(JNIEnv*, jobject, jint) {}

} // extern "C"
