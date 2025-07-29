package at.asitplus.wallet.app
import androidx.compose.runtime.Composable
import at.asitplus.wallet.app.common.DummyPlatformAdapter
import at.asitplus.wallet.app.common.PlatformAdapter

@Composable
actual fun getPlatformAdapter(): PlatformAdapter {
    return DummyPlatformAdapter()
}