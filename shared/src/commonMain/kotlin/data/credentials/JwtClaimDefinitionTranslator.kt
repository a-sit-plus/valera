package data.credentials

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.jwt_claim_label_aud
import at.asitplus.valera.resources.jwt_claim_label_exp
import at.asitplus.valera.resources.jwt_claim_label_iat
import at.asitplus.valera.resources.jwt_claim_label_iss
import at.asitplus.valera.resources.jwt_claim_label_nbf
import at.asitplus.valera.resources.jwt_claim_label_sub

class JwtClaimDefinitionTranslator {
    fun translate(claimDefinition: JwtClaimDefinition) = when(claimDefinition) {
        JwtClaimDefinition.ExpirationTime -> Res.string.jwt_claim_label_exp
        JwtClaimDefinition.Issuer -> Res.string.jwt_claim_label_iss
        JwtClaimDefinition.Subject -> Res.string.jwt_claim_label_sub
        JwtClaimDefinition.NotBefore -> Res.string.jwt_claim_label_nbf
        JwtClaimDefinition.IssuedAt -> Res.string.jwt_claim_label_iat
        JwtClaimDefinition.Audience -> Res.string.jwt_claim_label_aud
    }
}

