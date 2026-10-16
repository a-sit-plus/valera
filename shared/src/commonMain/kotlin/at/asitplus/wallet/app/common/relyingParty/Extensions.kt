package at.asitplus.wallet.app.common.relyingParty

import androidx.compose.ui.text.intl.Locale
import at.asitplus.etsi.relyingParty.WrpLangString
import at.asitplus.etsi.relyingParty.WrpMultiLangString
import kotlin.jvm.JvmName

fun List<WrpLangString>.getCurrentLocalization(): String? =
    this.firstOrNull {
        it.lang.lowercase().contains(Locale.current.language.lowercase())
    }?.value ?: this.firstOrNull()?.value

@JvmName("getCurrentLocalizationOfMultiLangStrings")
fun List<WrpMultiLangString>.getCurrentLocalization(): String? =
    this.firstOrNull {
        it.lang.lowercase().contains(Locale.current.language.lowercase())
    }?.content ?: this.firstOrNull()?.content
