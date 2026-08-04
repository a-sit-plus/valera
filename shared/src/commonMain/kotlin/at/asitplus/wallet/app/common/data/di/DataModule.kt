package at.asitplus.wallet.app.common.data.di

import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.data.SettingsRepository
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun dataModule() = module {
    scope(named(SESSION_NAME)) {
        scoped {
            WalletConfig(
                dataStoreService = get(),
                errorService = get(),
                buildType = get<BuildContext>().buildType,
            )
        } bind SettingsRepository::class
    }
}
