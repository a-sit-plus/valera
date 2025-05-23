package at.asitplus.wallet.app.common.domain.platform.di

import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.agent.KeyMaterial
import data.storage.DataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.github.aakira.napier.Napier
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.prompt.PromptModel

fun platformModule(appDependencyProvider: WalletDependencyProvider) = module {
    single<WalletKeyMaterial> {
        appDependencyProvider.keyMaterial
    } binds arrayOf(KeyMaterial::class)

    single<DataStoreService> {
        appDependencyProvider.dataStoreService
    }
    single<PlatformAdapter> {
        appDependencyProvider.platformAdapter
    }
    single<PersistentSubjectCredentialStore> {
        appDependencyProvider.subjectCredentialStore
    }

    single<BuildContext> {
        appDependencyProvider.buildContext
    }
    single<PromptModel> {
        appDependencyProvider.promptModel
    }

    factory<UrlOpener> {
        UrlOpener {
            appDependencyProvider.platformAdapter.openUrl(it)
        }
    }
    factory<ImageDecoder> {
        ImageDecoder {
            try {
                appDependencyProvider.platformAdapter.decodeImage(it)
            } catch (throwable: Throwable) {
                // TODO: should this be emitted to the error service?
                Napier.w("Failed Operation: decodeImage")
                null
            }
        }
    }
}

