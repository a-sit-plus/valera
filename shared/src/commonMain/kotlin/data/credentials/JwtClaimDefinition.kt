package data.credentials

// TODO: add more claims?
enum class JwtClaimDefinition(val claimName: String) {
    Issuer("iss"),
    Subject("sub"),
    ExpirationTime("exp"),
    NotBefore("nbf"),
    IssuedAt("iat"),
    Audience("aud"),
    ;

    companion object {
        fun valueOfClaimNameOrNull(claimName: String) = entries.firstOrNull {
            it.claimName == claimName
        }
    }
}


