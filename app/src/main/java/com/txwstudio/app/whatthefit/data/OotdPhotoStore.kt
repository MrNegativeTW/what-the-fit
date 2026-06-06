package com.txwstudio.app.whatthefit.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A FileProvider Uri the camera writes into, paired with the internal file path it maps to. */
data class CameraOutput(val uri: Uri, val path: String)

/**
 * Owns OOTD photo files under `filesDir/ootd`. Gallery picks are copied in so the record keeps a
 * stable file even if the source image is later removed; camera captures are written straight to an
 * internal file shared through [FileProvider]. Coil loads the stored absolute path.
 */
@Singleton
class OotdPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File
        get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /** Copies the content at [source] into internal storage; returns the new file's absolute path. */
    suspend fun copyIntoInternal(source: Uri): String = withContext(Dispatchers.IO) {
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        val input = context.contentResolver.openInputStream(source)
            ?: error("Cannot open photo source: $source")
        input.use { stream -> target.outputStream().use(stream::copyTo) }
        target.absolutePath
    }

    /** Creates an internal target file and a FileProvider Uri the camera can write the capture to. */
    fun newCameraOutput(): CameraOutput {
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
        return CameraOutput(uri = uri, path = target.absolutePath)
    }

    /** Deletes a stored photo file, ignoring a missing or already-removed file. */
    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).delete() }
    }

    private companion object {
        const val DIR_NAME = "ootd"
    }
}
