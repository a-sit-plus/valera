package at.asitplus.wallet.app.common.data.di

import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.WalletConfig
import at.asitplus.wallet.app.common.data.DataProvidingScope
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.storage.HotSubjectCredentialStore
import data.storage.WalletCredentialStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.multipaz.prompt.PromptModel

fun dataModule() = module {
    singleOf(::WalletConfig) {
        bind<SettingsRepository>()
    }
    singleOf(::HotSubjectCredentialStore) {
        // use this in vck and ui
        bind<SubjectCredentialStore>()
        bind<WalletCredentialStore>()
    }
    single<DataProvidingScope> {
        val errorService: ErrorService by inject()
        val promptModel: PromptModel by inject()
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, error ->
            errorService.emit(error)
        }
        DataProvidingScope(
            CoroutineScope(
                Dispatchers.IO + coroutineExceptionHandler + promptModel + CoroutineName("DataProvidingScope")
            )
        )
    }
}