package domain

import at.asitplus.wallet.lib.data.dif.PresentationDefinition

class ExtractClaimsFromPresentationDefinitionUseCase {

    operator fun invoke(presentationDefinition: PresentationDefinition): List<String> {
        return presentationDefinition.inputDescriptors
            .mapNotNull { it.constraints }.flatMap { it.fields?.toList() ?: listOf() }
            .flatMap { it.path.toList() }
            .filter { it != "$.type" }
            .filter { it != "$.mdoc.doctype" }
            .map { it.removePrefix("\$.mdoc.") }
            .map { it.removePrefix("\$.") }
    }
}