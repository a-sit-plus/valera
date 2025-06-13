package ui.viewmodels

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_error_action_reset_app
import at.asitplus.valera.resources.info_text_error_action_start_screen
import at.asitplus.valera.resources.info_text_error_cause_reset_app
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

class ErrorViewModel(
    val resetStack: () -> Unit,
    val resetApp: () -> Unit,
    val message: String?,
    val cause: String?,
    val onClickLogo: () -> Unit,
    val onClickSettings: () -> Unit
){
    var onClickButton: () -> Unit
    var actionDescription: StringResource
    var textCause: String?

    init {
        when(message) {
            "ResetAppException" -> {
                onClickButton = resetApp
                actionDescription = Res.string.info_text_error_action_reset_app
                textCause = runBlocking { getString(Res.string.info_text_error_cause_reset_app) }
            }
            else -> {
                onClickButton = resetStack
                actionDescription = Res.string.info_text_error_action_start_screen
                textCause = cause
            }
        }
    }
}