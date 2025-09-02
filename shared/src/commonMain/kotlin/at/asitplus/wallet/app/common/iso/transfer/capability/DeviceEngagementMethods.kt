package at.asitplus.wallet.app.common.iso.transfer.capability

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import at.asitplus.valera.resources.nfc
import at.asitplus.valera.resources.qr
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Nfc
import at.asitplus.valera.resources.Res

enum class DeviceEngagementMethods(val friendlyName: String, val icon: ImageVector) {
    NFC(runBlocking { getString(Res.string.nfc) }, Icons.Outlined.Nfc),
    QR_CODE(runBlocking { getString(Res.string.qr) }, Icons.Outlined.QrCode)
}
