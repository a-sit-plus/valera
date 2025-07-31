package at.asitplus.wallet.app.common.domain.platform.di

import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.PlatformAdapter
import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.WalletDependencyProvider
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.domain.platform.UrlOpener
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.storage.DataStoreService
import data.storage.HotWalletSubjectCredentialStore
import data.storage.PersistentSubjectCredentialStore
import data.storage.WalletSubjectCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import org.multipaz.prompt.PromptModel

fun platformModule(appDependencyProvider: WalletDependencyProvider) = module {
    scope(named(SESSION_NAME)) {
        scoped<WalletKeyMaterial> {
            WalletKeyMaterial(appDependencyProvider.keystoreService.getSignerBlocking())
        } binds arrayOf(KeyMaterial::class)
    }

    factory<DataStoreService> {
        appDependencyProvider.dataStoreService
    }
    factory<PlatformAdapter> {
        appDependencyProvider.platformAdapter
    }
    factory<PersistentSubjectCredentialStore> {
        appDependencyProvider.subjectCredentialStore
    }

    single<HotWalletSubjectCredentialStore> {
        HotWalletSubjectCredentialStore(
            delegate = get(),
            coroutineScope = CoroutineScope(Dispatchers.IO)
        )
    } binds arrayOf(
        SubjectCredentialStore::class,
        WalletSubjectCredentialStore::class,
    )

    factory<BuildContext> {
        appDependencyProvider.buildContext
    }
    factory<PromptModel> {
        appDependencyProvider.promptModel
    }

    factory<UrlOpener> {
        UrlOpener {
            appDependencyProvider.platformAdapter.openUrl(it)
        }
    }

    factory<ImageDecoder> {
        ImageDecoder {
            appDependencyProvider.platformAdapter.decodeImage(it)
        }
    }
}

