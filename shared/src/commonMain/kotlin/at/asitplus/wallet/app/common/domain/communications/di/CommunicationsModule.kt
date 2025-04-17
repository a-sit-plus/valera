package at.asitplus.wallet.app.common.domain.communications.di

import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.PresentationService
import at.asitplus.wallet.app.common.ProvisioningService
import at.asitplus.wallet.app.common.SigningService
import at.asitplus.wallet.app.common.dcapi.DCAPIExportService
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun communicationsModule() = module {
    singleOf(::HttpService)
    singleOf(::ProvisioningService)
    singleOf(::PresentationService)
    singleOf(::SigningService)
    singleOf(::DCAPIExportService)
}