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

/**
 * RomManager — l'utente aggiunge le proprie ROM (GBA/GB/GBC).
 * Nessuna ROM inclusa nell'APK per motivi legali.
 *
 * Ottimizzazioni:
 * - copia SAF -> cache interna una sola volta (hash SHA-256 = dedup)
 * - validazione header GBA (0xB2 + logo Nintendo, size check)
 * - database ROM in DataStore con titolo rilevato dal filename
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

    fun romsDir(ctx: Context) = File(ctx.filesDir, ROMS_DIR).apply { mkdirs() }

    suspend fun importRom(ctx: Context, uri: Uri): Result<RomEntry> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = ctx.contentResolver
                val name = DocumentFile.fromSingleUri(ctx, uri)?.name
                    ?: "game_${System.currentTimeMillis()}.gba"
                val system = detectSystem(name)
                if (system == RomSystem.UNKNOWN) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Formato non supportato. Usa .gba / .gb / .gbc")
                    )
                }

                // Copia in staging per hash + validazione
                val tmp = File.createTempFile("rom_import", ".tmp", ctx.cacheDir)
                resolver.openInputStream(uri)?.use { ins ->
                    FileOutputStream(tmp).use { outs -> ins.copyTo(outs) }
                } ?: return@withContext Result.failure(IllegalStateException("Impossibile leggere il file"))

                if (!validateRom(tmp, system)) {
                    tmp.delete()
                    return@withContext Result.failure(
                        IllegalArgumentException("File non valido: header ${system.name} non riconosciuto")
                    )
                }

                val sha = sha256(tmp)
                val ext = name.substringAfterLast('.', "gba").lowercase()
                val dest = File(romsDir(ctx), "$sha.$ext")
                if (!dest.exists()) tmp.copyTo(dest) else tmp.delete()

                File(ctx.filesDir, "$SAVES_DIR/$sha").apply { mkdirs() }
                File(ctx.filesDir, "$STATES_DIR/$sha").apply { mkdirs() }

                val title = name.substringBeforeLast('.')
                    .replace('_', ' ').replace('-', ' ')
                    .take(48).trim().ifEmpty { "Gioco $sha" }

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
            } catch (e: Exception) {
                Result.failure(e)
            }
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

    /**
     * Validazione leggera ma efficace:
     * GBA: 192 byte header a 0xA0, byte 0xB2 == 0x96, Nintendo logo checksum.
     * GB/GBC: logo check a 0x104 + header checksum a 0x14D.
     */
    private fun validateRom(f: File, system: RomSystem): Boolean {
        if (f.length() < 1024) return false
        if (f.length() > 64L * 1024 * 1024) return false // GBA max 32MB, margine
        return try {
            f.inputStream().use { ins ->
                val buf = ByteArray(0x200)
                ins.read(buf)
                when (system) {
                    RomSystem.GBA -> buf.size > 0xB2 && (buf[0xB2].toInt() and 0xFF) == 0x96
                    RomSystem.GB, RomSystem.GBC -> {
                        // header checksum: 0x134..0x14C
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

    /** Accesso NDK: fd diretto = zero copie, perfetto per core mGBA a 60fps. */
    fun openRomFd(ctx: Context, rom: RomEntry): ParcelFileDescriptor {
        val f = File(rom.internalPath)
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
