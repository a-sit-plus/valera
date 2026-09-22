package at.asitplus.wallet.app.common.relyingParty

import androidx.compose.ui.text.intl.Locale
import at.asitplus.etsi.relyingParty.WrpLangString

fun List<WrpLangString>.getCurrentLocalization(): String? =
    this.firstOrNull {
        it.lang.lowercase().contains(Locale.current.language.lowercase())
    }?.value ?: this.firstOrNull()?.value
