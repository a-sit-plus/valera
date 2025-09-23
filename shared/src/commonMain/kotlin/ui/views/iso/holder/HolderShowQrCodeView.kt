package ui.views.iso.holder

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.heading_label_show_qr_code_screen
import at.asitplus.wallet.app.common.iso.transfer.MdocConstants
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.io.bytestring.ByteString
import org.jetbrains.compose.resources.stringResource
import org.multipaz.util.toBase64Url
import ui.composables.Logo
import ui.composables.ScreenHeading
import ui.composables.buttons.NavigateUpButton
import ui.viewmodels.iso.holder.HolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolderShowQrCodeView(
    bytes: ByteString?,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
    vm: HolderViewModel,
    bottomBar: @Composable () -> Unit,
    onError: (Throwable) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            ScreenHeading(stringResource(Res.string.heading_label_show_qr_code_screen))
                        }
                    }
                },
                actions = {
                    Logo(onClick = onClickLogo)
                    Column(modifier = Modifier.clickable(onClick = onClickSettings)) {
                        Icon(Icons.Outlined.Settings, null)
                    }
                    Spacer(Modifier.width(15.dp))
                },
                navigationIcon = { NavigateUpButton({ vm.onResume() }) }
            )
        },
        bottomBar = { bottomBar() }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val data = bytes ?: run {
                    // TODO: add string resource
                    onError(Throwable("Invalid data for qr code."))
                    return@Box
                }
                Image(
                    painter = rememberQrCodePainter(
                        data = MdocConstants.MDOC_PREFIX + data.toByteArray().toBase64Url(),
                        colors = if (isSystemInDarkTheme()) {
                            QrColors(dark = QrBrush.solid(Color.White))
                        } else QrColors()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.8f)
                )
            }
        }
    }
}
