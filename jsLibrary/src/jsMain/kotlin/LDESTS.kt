import be.ugent.idlab.predict.ldests.core.LDESTS

/**
 * A JS-exportable wrapper for the SolidConnection class from the shared codebase
 */

@JsExport
@JsName("LDESTS")
@ExternalUse
class LDESTSJS {

    private val parent = LDESTS()

    @ExternalUse
    fun append(filename: String) = parent.append(filename = filename)

    @ExternalUse
    fun flush() = promise { parent.flush() }

    @ExternalUse
    fun close() = promise { parent.close() }

}
