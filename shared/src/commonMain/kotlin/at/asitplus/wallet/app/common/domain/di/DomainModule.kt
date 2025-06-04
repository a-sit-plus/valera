package at.asitplus.wallet.app.common.domain.di

import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.domain.BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase
import at.asitplus.wallet.app.common.domain.communications.di.communicationsModule
import at.asitplus.wallet.app.common.domain.platform.di.platformModule
import at.asitplus.wallet.app.common.domain.vck.di.vckModule
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun domainModule(walletDependencyProvider: WalletDependencyProvider) = module {
    singleOf(::BuildAuthenticationConsentPageFromAuthenticationRequestUriUseCase)
    includes(platformModule(walletDependencyProvider))
    includes(communicationsModule())
    includes(vckModule())
}