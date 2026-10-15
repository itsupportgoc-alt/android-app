package com.wordcraft.documentcore

import org.json.JSONObject

/** A failure reported by the WordCraft engine (open/save/command error). */
class WordCraftException(val error: String) : Exception(error)

/**
 * One open document: an opaque handle to a Rust `wordcraft_engine::Session`.
 *
 * Every mutation goes through the engine's command registry (`run`), which owns selection,
 * undo/redo, AutoCorrect and format semantics exactly as on desktop WordCraft.
 */
class WordCraftEngine private constructor(val handle: Long) {

    companion object {
        /** A new blank document. */
        fun newDocument(): WordCraftEngine = WordCraftEngine(nativeSessionNew())

        /**
         * Open a document from bytes. The name's extension picks the format
         * (docx/docm/dotx/dotm/doc/odt/rtf/html/md/tex/txt/json), as on desktop.
         */
        fun open(name: String, bytes: ByteArray): WordCraftEngine {
            val h = nativeOpenBytes(u8(name), bytes)
            if (h == 0L) throw WordCraftException(lastError())
            return WordCraftEngine(h)
        }

        fun lastError(): String = fromU8(nativeLastError()) ?: "unknown native error"
    }

    /** Run an engine command; returns `{"ok": ...}`. Throws [WordCraftException] on failure. */
    fun run(id: String, params: JSONObject? = null): JSONObject {
        val raw = fromU8(nativeRunCommand(handle, u8(id), u8(params?.toString() ?: "")))
            ?: throw WordCraftException("no response from engine")
        val json = JSONObject(raw)
        if (json.has("error")) throw WordCraftException(json.getString("error"))
        return json
    }

    /** Run a command, swallowing errors (for optional/disabled commands). */
    fun runOrNull(id: String, params: JSONObject? = null): JSONObject? = try {
        run(id, params)
    } catch (_: WordCraftException) {
        null
    }

    /** The editor view model (document blocks, selection, toolbar state). */
    fun state(): EditorState = StateParser.parse(fromU8(nativeState(handle)) ?: throw WordCraftException(lastError()))

    /** Serialize the document (format picked by the file name: .docx, .odt, .md, .txt, …). */
    fun save(name: String): ByteArray =
        nativeSaveBytes(handle, u8(name)) ?: throw WordCraftException(lastError())

    fun close() = nativeCloseSession(handle)
}
