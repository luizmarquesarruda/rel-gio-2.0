package com.luizmarquesarruda.relogio20.data.intelligence

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException

/** App-private AtomicFile adapter; Android manages the previous-file recovery copy. */
internal class DailyActivityHistoryAtomicFile(context: Context) : DailyActivityHistoryStorage {
    private val atomicFile = AtomicFile(
        File(context.applicationContext.filesDir, FILE_NAME)
    )

    @Synchronized
    override fun readBytes(): ByteArray? = try {
        atomicFile.openRead().use { it.readBytes() }
    } catch (_: FileNotFoundException) {
        null
    }

    @Synchronized
    override fun writeAtomically(bytes: ByteArray): Boolean {
        val stream = try {
            atomicFile.startWrite()
        } catch (_: Exception) {
            return false
        }

        return try {
            stream.write(bytes)
            atomicFile.finishWrite(stream)
            true
        } catch (_: Exception) {
            atomicFile.failWrite(stream)
            false
        }
    }

    private companion object {
        const val FILE_NAME = "daily-activity-history-v1.bin"
    }
}
