package ui.viewmodels

import AppResetRequiredException
import ErrorHandlingOverrideException
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_reset_app
import at.asitplus.valera.resources.info_text_error_action_start_screen
import at.asitplus.valera.resources.info_text_error_cause_reset_app
import at.asitplus.wallet.app.common.enrichMessage
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class ErrorViewModel(
    val clearError: () -> Unit,
    val resetStack: () -> Unit,
    val resetApp: () -> Unit,
    val throwable: Throwable,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit
){
    var onClickButton: () -> Unit
    var actionDescription: StringResource
    var textCause: String?

    private val displayThrowable = (throwable as? ErrorHandlingOverrideException)?.cause ?: throwable
    val message = displayThrowable.enrichMessage()
    val cause = displayThrowable.cause?.toString()

    init {
        val exceptionOverride = throwable as? ErrorHandlingOverrideException
        val onAcknowledge = exceptionOverride?.onAcknowledge
        when {
            exceptionOverride?.hasUiOverride == true -> {
                onClickButton = {
                    clearError()
                    onAcknowledge?.runCatching { invoke() }
                    exceptionOverride.resetStackOverride!!.invoke()
                }
                actionDescription = exceptionOverride.actionDescriptionOverride!!
                textCause = cause
            }
            message == AppResetRequiredException.toString() -> {
                onClickButton = {
                    clearError()
                    resetApp()
                    onAcknowledge?.runCatching { invoke() }
                }
                actionDescription = Res.string.info_text_error_action_reset_app
                textCause = runBlocking { getString(Res.string.info_text_error_cause_reset_app) }
            }
            else -> {
                onClickButton = {
                    clearError()
                    resetStack()
                    onAcknowledge?.runCatching { invoke() }
                }
                actionDescription = Res.string.info_text_error_action_start_screen
                textCause = cause
            }
        }
    }
}
