package com.retrofold.sp.core

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * RomManager — l'utente aggiunge le proprie ROM (GBA/GB/GBC),
 * anche dentro file .zip (es. "Pokemon - Fire Red Version (U) (Squirrels).zip").
 * Nessuna ROM inclusa nell'APK per motivi legali.
 *
 * Errori come codici (la UI li traduce nella lingua scelta):
 * ERR_UNSUPPORTED, ERR_INVALID, ERR_READ, ERR_ZIP_EMPTY, ERR_ZIP_LARGE
 */
data class RomEntry(
    val id: String,          // sha256
    val title: String,
    val system: RomSystem,
    val sizeBytes: Long,
    val internalPath: String,
    val saveDir: String
)

enum class RomSystem { GBA, GB, GBC, UNKNOWN }

object RomManager {

    private const val ROMS_DIR = "roms"
    private const val SAVES_DIR = "saves"
    private const val STATES_DIR = "states"

    /** Limiti anti zip-bomb: max 1 ROM estratta, 64MB, 50 voci. */
    private const val MAX_ROM_BYTES = 64L * 1024 * 1024
    private const val MAX_ZIP_ENTRIES = 50

    fun romsDir(ctx: Context) = File(ctx.filesDir, ROMS_DIR).apply { mkdirs() }

    suspend fun importRom(ctx: Context, uri: Uri): Result<RomEntry> =
        withContext(Dispatchers.IO) {
            try {
                val docName = DocumentFile.fromSingleUri(ctx, uri)?.name
                    ?: "game_${System.currentTimeMillis()}.gba"

                // 1) ZIP? -> estrai la ROM contenuta
                val staged: File
                val displayName: String
                if (docName.substringAfterLast('.', "").lowercase() == "zip") {
                    val extracted = extractRomFromZip(ctx, uri)
                        ?: return@withContext Result.failure(
                            IllegalArgumentException("ERR_ZIP_EMPTY")
                        )
                    staged = extracted.first
                    displayName = extracted.second
                } else {
                    val tmp = File.createTempFile("rom_import", ".tmp", ctx.cacheDir)
                    ctx.contentResolver.openInputStream(uri)?.use { ins ->
                        FileOutputStream(tmp).use { outs -> ins.copyTo(outs) }
                    } ?: return@withContext Result.failure(
                        IllegalArgumentException("ERR_READ")
                    )
                    staged = tmp
                    displayName = docName
                }

                val system = detectSystem(displayName)
                if (system == RomSystem.UNKNOWN) {
                    staged.delete()
                    return@withContext Result.failure(
                        IllegalArgumentException("ERR_UNSUPPORTED")
                    )
                }
                if (!validateRom(staged, system)) {
                    staged.delete()
                    return@withContext Result.failure(
                        IllegalArgumentException("ERR_INVALID")
                    )
                }

                val sha = sha256(staged)
                val ext = displayName.substringAfterLast('.', "gba").lowercase()
                val dest = File(romsDir(ctx), "$sha.$ext")
                if (!dest.exists()) staged.copyTo(dest)
                staged.delete()

                File(ctx.filesDir, "$SAVES_DIR/$sha").apply { mkdirs() }
                File(ctx.filesDir, "$STATES_DIR/$sha").apply { mkdirs() }

                val title = displayName.substringBeforeLast('.')
                    .substringAfterLast('/')
                    .replace('_', ' ').replace('-', ' ')
                    .trim().take(48).ifEmpty { "Gioco $sha" }

                Result.success(
                    RomEntry(
                        id = sha,
                        title = title,
                        system = system,
                        sizeBytes = dest.length(),
                        internalPath = dest.absolutePath,
                        saveDir = File(ctx.filesDir, "$SAVES_DIR/$sha").absolutePath
                    )
                )
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            } catch (_: Exception) {
                Result.failure(IllegalArgumentException("ERR_READ"))
            }
        }

    /**
     * Estrae la prima ROM valida da uno ZIP (preferenza: .gba > .gbc > .gb).
     * Ritorna (file staging, nome entry) oppure null se nessuna ROM dentro.
     */
    private fun extractRomFromZip(ctx: Context, uri: Uri): Pair<File, String>? {
        val candidates = mutableListOf<Pair<String, ByteArray>>()
        ctx.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entries = 0
                var totalBytes = 0L
                while (true) {
                    val entry = try { zip.nextEntry } catch (_: Exception) { break }
                        ?: break
                    entries++
                    if (entries > MAX_ZIP_ENTRIES) break
                    val name = entry.name.substringAfterLast('/').trim()
                    if (entry.isDirectory || name.isEmpty()) { zip.closeEntry(); continue }
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext != "gba" && ext != "gb" && ext != "gbc") { zip.closeEntry(); continue }
                    if (entry.compressedSize > MAX_ROM_BYTES) { zip.closeEntry(); continue }
                    val out = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    while (true) {
                        val n = zip.read(buf)
                        if (n <= 0) break
                        totalBytes += n
                        if (totalBytes > MAX_ROM_BYTES) return null
                        out.write(buf, 0, n)
                    }
                    zip.closeEntry()
                    candidates += name to out.toByteArray()
                    if (candidates.size >= 5) break
                }
            }
        } ?: return null
        if (candidates.isEmpty()) return null
        val rank = mapOf("gba" to 0, "gbc" to 1, "gb" to 2)
        val best = candidates.minByOrNull {
            rank[it.first.substringAfterLast('.', "gba").lowercase()] ?: 9
        } ?: return null
        val tmp = File.createTempFile("rom_zip", ".tmp", ctx.cacheDir)
        tmp.writeBytes(best.second)
        return tmp to best.first
    }

    fun listRoms(ctx: Context): List<RomEntry> {
        val dir = romsDir(ctx)
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && detectSystem(it.name) != RomSystem.UNKNOWN }
            ?.map {
                val sha = it.nameWithoutExtension
                RomEntry(
                    id = sha,
                    title = sha.take(8).uppercase() + " • " + it.name,
                    system = detectSystem(it.name),
                    sizeBytes = it.length(),
                    internalPath = it.absolutePath,
                    saveDir = File(ctx.filesDir, "$SAVES_DIR/$sha").absolutePath
                )
            } ?: emptyList()
    }

    fun stateFile(ctx: Context, romId: String, slot: Int): File {
        val d = File(ctx.filesDir, "$STATES_DIR/$romId").apply { mkdirs() }
        return File(d, "slot$slot.state")
    }

    fun batterySave(ctx: Context, romId: String): File {
        val d = File(ctx.filesDir, "$SAVES_DIR/$romId").apply { mkdirs() }
        return File(d, "battery.sav")
    }

    private fun detectSystem(name: String): RomSystem = when (
        name.substringAfterLast('.', "").lowercase()
    ) {
        "gba" -> RomSystem.GBA
        "gb" -> RomSystem.GB
        "gbc" -> RomSystem.GBC
        else -> RomSystem.UNKNOWN
    }

    private fun validateRom(f: File, system: RomSystem): Boolean {
        if (f.length() < 1024) return false
        if (f.length() > MAX_ROM_BYTES) return false
        return try {
            f.inputStream().use { ins ->
                val buf = ByteArray(0x200)
                ins.read(buf)
                when (system) {
                    RomSystem.GBA -> buf.size > 0xB2 && (buf[0xB2].toInt() and 0xFF) == 0x96
                    RomSystem.GB, RomSystem.GBC -> {
                        var x = 0
                        for (i in 0x134..0x14C) x = x - (buf[i].toInt() and 0xFF) - 1
                        (x and 0xFF) == (buf[0x14D].toInt() and 0xFF)
                    }
                    else -> false
                }
            }
        } catch (_: Exception) { false }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { ins ->
            val buf = ByteArray(8192)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun openRomFd(ctx: Context, rom: RomEntry): ParcelFileDescriptor {
        val f = File(rom.internalPath)
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
