package data.idaustria

import at.asitplus.wallet.lib.LibraryInitializer
import at.asitplus.wallet.lib.data.CredentialSubject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class Initializer {

    companion object {

        /**
         * A reference to this class is enough to trigger the init block
         */
        init {
            initWithVcLib()
        }

        /**
         * This has to be called first, before anything first, to load the
         * relevant classes of this library into the base implementations of vclib
         */
        fun initWithVcLib() {
            LibraryInitializer.registerExtensionLibrary(
                LibraryInitializer.ExtensionLibraryInfo(
                    credentialScheme = ConstantIndex.IdAustriaCredential,
                    serializersModule = SerializersModule {
                        polymorphic(CredentialSubject::class) {
                            subclass(IdAustriaCredential::class)
                        }
                    },
                )
            )
        }
    }

}