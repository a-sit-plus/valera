package at.asitplus.wallet.app.common.domain.di

import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.domain.communications.di.communicationsModule
import at.asitplus.wallet.app.common.domain.platform.di.platformModule
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di.tokenStatusListModule
import at.asitplus.wallet.app.common.domain.vck.di.vckModule
import org.koin.dsl.module

fun domainModule(walletDependencyProvider: WalletDependencyProvider) = module {
    includes(platformModule(walletDependencyProvider))
    includes(communicationsModule())
    includes(vckModule())
}