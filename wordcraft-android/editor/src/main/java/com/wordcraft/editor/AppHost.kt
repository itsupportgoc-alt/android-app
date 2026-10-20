package com.wordcraft.editor

/**
 * Platform services the ribbon needs but the editor module cannot provide itself
 * (file pickers, the print service, the share sheet).
 *
 * The app layer installs a handler in [AppHost.handler]; the editor calls
 * [AppHost.perform]. When no handler is installed the action reports honestly instead of
 * silently doing nothing.
 */
enum class AppActionKind {
    /** Start a new blank document. */
    NEW,

    /** Open a document from the device. */
    OPEN,

    /** Save the document (to the file it came from, or via Save as…). */
    SAVE,

    /** Save the document under a new name. */
    SAVE_AS,

    /** Export the document as PDF. */
    SAVE_AS_PDF,

    /** Open a PDF and make it editable. */
    OPEN_PDF,

    /** Share the document. */
    SHARE,

    /** Print the document. */
    PRINT,

    /** Show the recent documents. */
    RECENTS,

    /** Pick an image and insert it. */
    PICK_IMAGE,

    /** Pick a document and insert its text. */
    PICK_TEXT_FILE,

    /** Pick a recipient list (CSV). */
    PICK_CSV,

    /** Pick a document to compare with. */
    PICK_DOCUMENT,

    /** Open a web page (help, support). */
    OPEN_URL,
}

/** The bridge between the editor's ribbon and the app's platform services. */
object AppHost {

    /** Installed by the app layer. */
    var handler: ((AppActionKind) -> Unit)? = null

    /** Installed by the app layer: opens a web page (Help tab). */
    var urlHandler: ((String) -> Unit)? = null

    /** Installed by the app layer: the app's version, shown by the About dialog. */
    var appVersion: String = "1.0"

    /** Run [kind]; reports honestly when the app did not install a handler. */
    fun perform(kind: AppActionKind) {
        val h = handler
        if (h == null) {
            EditorController.showStatus("${kind.name.replace('_', ' ').lowercase()} is not available in this build")
        } else {
            h(kind)
        }
    }

    /** Open [url] in the browser (used by the Help tab). */
    fun openUrl(url: String) {
        val h = urlHandler
        if (h == null) EditorController.showStatus(url) else h(url)
    }
}
