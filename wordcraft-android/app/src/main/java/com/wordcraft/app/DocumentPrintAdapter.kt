package com.wordcraft.app

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.FileOutputStream

/**
 * Prints the exported PDF through the Android print service.
 *
 * The document is rendered once with [PdfExport] and the resulting bytes are handed to the
 * print framework, so the print dialog shows the real document.
 */
class DocumentPrintAdapter(private val bytes: ByteArray) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: PrintDocumentAdapter.LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("DevWord document")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: PrintDocumentAdapter.WriteResultCallback,
    ) {
        try {
            FileOutputStream(destination.fileDescriptor).use { it.write(bytes) }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message ?: "Could not write the document")
        }
    }
}
