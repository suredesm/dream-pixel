package lk.sure.dream.utils

import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile


fun String.isValidFileName(): Boolean {
    return isNotBlank() &&
            !contains('/') &&
            !contains('\\') &&
            !contains(':') &&
            !contains('*') &&
            !contains('?') &&
            !contains('"') &&
            !contains('<') &&
            !contains('>') &&
            !contains('|')
}

fun Uri.toHumanReadable(): String {
    val id = DocumentsContract.getTreeDocumentId(this)
    val parts = id.split(":")

    return if (parts.first() == "primary") {
        "Internal:" + parts[1]
    } else {
        "External:" + parts[1]
    }
}

val DocumentFile.nameWithoutExtension get() = name?.substringBeforeLast(".")