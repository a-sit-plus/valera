package at.asitplus.wallet.app.common.iso.transfer.method

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.ui.graphics.vector.ImageVector
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.nfc
import at.asitplus.valera.resources.qr
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

enum class DeviceEngagementMethods(val friendlyName: String, val icon: ImageVector) {
    NFC(runBlocking { getString(Res.string.nfc) }, Icons.Outlined.Nfc),
    QR_CODE(runBlocking { getString(Res.string.qr) }, Icons.Outlined.QrCode)
}
