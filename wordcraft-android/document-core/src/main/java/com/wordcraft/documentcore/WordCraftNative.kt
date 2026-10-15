/**
 * JNI boundary to libwordcraft_core.so (rust-core). All strings cross as UTF-8 byte arrays —
 * JNI's String uses modified UTF-8, which would corrupt supplementary characters (emoji).
 *
 * These must stay top-level functions in a file named WordCraftNative.kt: the generated class
 * WordCraftNativeKt matches the native symbols (Java_com_wordcraft_documentcore_WordCraftNativeKt_*).
 */
package com.wordcraft.documentcore

private val nativeLibLoaded: Boolean = try {
    System.loadLibrary("wordcraft_core")
    true
} catch (_: UnsatisfiedLinkError) {
    // Unit tests on the JVM never load the Android library.
    false
}

/** New blank document session handle (never 0). */
external fun nativeSessionNew(): Long

/** Open a document from bytes; 0 on failure (see [nativeLastError]). */
external fun nativeOpenBytes(name: ByteArray, data: ByteArray): Long

/** Drop a session. */
external fun nativeCloseSession(handle: Long)

/** Serialize the session's document; null on error (see [nativeLastError]). */
external fun nativeSaveBytes(handle: Long, name: ByteArray): ByteArray?

/** Run an engine command; UTF-8 JSON {"ok": ...} or {"error": ...}. */
external fun nativeRunCommand(handle: Long, id: ByteArray, params: ByteArray): ByteArray?

/** Editor view-model JSON; null on failure. */
external fun nativeState(handle: Long): ByteArray?

/** Last open/save failure message, or null. */
external fun nativeLastError(): ByteArray?

internal fun u8(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)

internal fun fromU8(b: ByteArray?): String? = b?.toString(Charsets.UTF_8)
