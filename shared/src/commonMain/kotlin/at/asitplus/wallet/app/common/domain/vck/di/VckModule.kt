package at.asitplus.wallet.app.common.domain.vck.di

import at.asitplus.wallet.app.common.SESSION_NAME
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.StatusListTokenResolver
import at.asitplus.wallet.app.common.domain.vck.JsonWebKeySetResolver
import at.asitplus.wallet.app.common.domain.vck.PublicKeyResolver
import at.asitplus.wallet.app.common.domain.vck.tokenStatusList.di.tokenStatusListModule
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.KeyMaterial
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.ValidatorSdJwt
import at.asitplus.wallet.lib.agent.ValidatorVcJws
import at.asitplus.wallet.lib.agent.validation.TokenStatusResolverImpl
import at.asitplus.wallet.lib.jws.VerifyJwsObject
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun vckModule() = module {
    singleOf(::JsonWebKeySetResolver)
    singleOf(::PublicKeyResolver)
    single<Validator> {
        val statusListTokenResolver: StatusListTokenResolver by inject()
        Validator(
            tokenStatusResolver = TokenStatusResolverImpl(
                resolveStatusListToken = statusListTokenResolver::invoke
            )
        )
    }
    scope(named(SESSION_NAME)) {
        scoped<HolderAgent> {
            val keyMaterial: KeyMaterial by inject()
            val subjectCredentialStore: SubjectCredentialStore by inject()
            val validator: Validator by inject()
            val publicKeyResolver: PublicKeyResolver by inject()
            HolderAgent(
                keyMaterial = keyMaterial,
                subjectCredentialStore = subjectCredentialStore,
                validator = validator,
                validatorVcJws = ValidatorVcJws(verifyJwsObject = VerifyJwsObject(publicKeyLookup = publicKeyResolver::invoke)),
                validatorSdJwt = ValidatorSdJwt(verifyJwsObject = VerifyJwsObject(publicKeyLookup = publicKeyResolver::invoke))
            )
        }
    }
    includes(tokenStatusListModule())
}
