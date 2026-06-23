package at.asitplus.wallet.app.common.domain.di

import at.asitplus.wallet.app.common.domain.communications.di.communicationsModule
import at.asitplus.wallet.app.common.domain.platform.di.platformModule
import at.asitplus.wallet.app.common.domain.vck.di.vckModule
import org.koin.dsl.module

fun domainModule() = module {
    includes(platformModule())
    includes(communicationsModule())
    includes(vckModule())
}
