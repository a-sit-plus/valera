package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_fiissuer_request_submitted
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.fiissuer.FIIssuerCredentialTypeDto
import at.asitplus.wallet.app.common.fiissuer.FIIssuerService
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class LoadWithFIIssuerViewModel(
    private val walletMain: WalletMain,
    private val fiIssuerService: FIIssuerService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoadWithFIIssuerUiState())
    val uiState: StateFlow<LoadWithFIIssuerUiState> = _uiState.asStateFlow()

    init {
        loadCredentialTypes()
    }

    fun loadCredentialTypes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                fiIssuerService.listCredentialTypeNames()
            }.onSuccess { names ->
                val credentialTypeNames = names.withLocalPassportCredentialType()
                val selected = credentialTypeNames.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    credentialTypeNames = credentialTypeNames,
                    selectedCredentialType = selected,
                    selectedCredentialTypeDetails = null,
                    claimValues = emptyMap(),
                )
                selected?.let { loadCredentialTypeDetails(it) }
            }.onFailure { error ->
                Napier.w("FIIssuer: failed to load credential type names", error)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: error::class.simpleName ?: "Failed to load credential types",
                )
            }
        }
    }

    fun selectCredentialType(typeName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedCredentialType = typeName,
                selectedCredentialTypeDetails = null,
                claimValues = emptyMap(),
                errorMessage = null,
            )
            loadCredentialTypeDetails(typeName)
        }
    }

    fun updateClaimValue(key: String, value: String) {
        _uiState.value = _uiState.value.copy(
            claimValues = _uiState.value.claimValues + (key to value),
        )
    }

    fun applyScannedDocumentText(scannedText: String) {
        val state = _uiState.value
        val claimKeys = state.selectedCredentialTypeDetails?.requiredClaimKeys.orEmpty()
        if (claimKeys.isEmpty()) {
            _uiState.value = state.copy(lastScannedDocumentText = scannedText)
            return
        }

        val extractedValues = extractClaimValuesFromDocumentText(scannedText, claimKeys)
        if (extractedValues.isEmpty()) {
            _uiState.value = state.copy(
                errorMessage = "No matching document data was detected",
                lastScannedDocumentText = scannedText,
            )
            return
        }

        _uiState.value = state.copy(
            claimValues = state.claimValues + extractedValues,
            errorMessage = null,
            lastScannedDocumentText = scannedText,
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val selectedType = _uiState.value.selectedCredentialType
        val claimValues = _uiState.value.claimValues
        if (selectedType.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a credential type")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null)
            runCatching {
                fiIssuerService.createCredentialRequest(
                    credentialType = selectedType,
                    claims = claimValues,
                )
            }.onSuccess { response ->
                Napier.d("FIIssuer: created credential request transactionId=${response.transactionId} status=${response.status}")
                walletMain.fiIssuerPollingService.trackPendingRequest(
                    transactionId = response.transactionId,
                    credentialType = selectedType,
                    claims = claimValues,
                )
                walletMain.snackbarService.showSnackbar(getString(Res.string.snackbar_fiissuer_request_submitted))
                _uiState.value = _uiState.value.copy(isSubmitting = false)
                onSuccess()
            }.onFailure { error ->
                Napier.w("FIIssuer: failed to create credential request", error)
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: error::class.simpleName ?: "Failed to submit FIIssuer request",
                )
            }
        }
    }

    private suspend fun loadCredentialTypeDetails(typeName: String) {
        if (typeName == LOCAL_PASSPORT_CREDENTIAL_TYPE.typeName) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedCredentialTypeDetails = LOCAL_PASSPORT_CREDENTIAL_TYPE,
                claimValues = LOCAL_PASSPORT_CREDENTIAL_TYPE.requiredClaimKeys.associateWith {
                    _uiState.value.claimValues[it] ?: ""
                },
                errorMessage = null,
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        runCatching {
            fiIssuerService.getCredentialType(typeName)
        }.onSuccess { details ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedCredentialTypeDetails = details,
                claimValues = details.requiredClaimKeys.associateWith { _uiState.value.claimValues[it] ?: "" },
            )
        }.onFailure { error ->
            Napier.w("FIIssuer: failed to load credential type details for $typeName", error)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = error.message ?: error::class.simpleName ?: "Failed to load credential type",
            )
        }
    }
}

data class LoadWithFIIssuerUiState(
    val isLoading: Boolean = true,
    val credentialTypeNames: List<String> = emptyList(),
    val selectedCredentialType: String? = null,
    val selectedCredentialTypeDetails: FIIssuerCredentialTypeDto? = null,
    val claimValues: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val lastScannedDocumentText: String? = null,
)

private val LOCAL_PASSPORT_CREDENTIAL_TYPE = FIIssuerCredentialTypeDto(
    typeName = "Passport",
    requiredClaimKeys = listOf(
        "first_name",
        "last_name",
        "birth_date",
        "document_id",
        "country_code",
        "issuance_date",
        "expiry_date",
        "issuing_country",
    ),
)

private fun List<String>.withLocalPassportCredentialType(): List<String> =
    if (contains(LOCAL_PASSPORT_CREDENTIAL_TYPE.typeName)) {
        this
    } else {
        this + LOCAL_PASSPORT_CREDENTIAL_TYPE.typeName
    }

private fun extractClaimValuesFromDocumentText(
    scannedText: String,
    claimKeys: List<String>,
): Map<String, String> {
    val lines = scannedText.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    if (lines.isEmpty()) return emptyMap()

    val mrzValues = parseMrz(lines)
    val result = mutableMapOf<String, String>()
    claimKeys.forEach { key ->
        val canonicalKey = key.canonicalClaimKey()
        val value = mrzValues[canonicalKey]
            ?: findLabeledValue(lines, canonicalKey)
            ?: findUnlabeledValue(lines, canonicalKey)
        if (!value.isNullOrBlank()) {
            result[key] = value
        }
    }
    return result
}

private fun parseMrz(lines: List<String>): Map<String, String> {
    val mrzLines = lines
        .flatMap { it.split(Regex("\\s+")) + listOf(it) }
        .map { it.normalizeMrzLine() }
        .filter { it.isMrzLikeLine() }
        .distinct()

    val td3 = mrzLines.windowed(2).firstOrNull {
        it[0].length >= 30 &&
                it[1].length >= 30 &&
                (it[0].startsWith("P<") || it[0].startsWith("P"))
    }
    if (td3 != null) {
        val firstLine = td3[0].padEnd(44, '<')
        val secondLine = td3[1].padEnd(44, '<')
        val nameParts = firstLine.drop(5).split("<<", limit = 2)
        return buildMap {
            putNames(nameParts)
            val documentId = secondLine.take(9).cleanMrzValue()
            val countryCode = secondLine.substringSafe(10, 13).cleanMrzValue().toIso2CountryCode()
            put("document_number", documentId)
            put("document_id", documentId)
            put("nationality", countryCode)
            put("country_code", countryCode)
            put("issuing_country", firstLine.substringSafe(2, 5).cleanMrzValue().toIso2CountryCode())
            put("birth_date", secondLine.substringSafe(13, 19).formatMrzDate())
            put("sex", secondLine.substringSafe(20, 21).cleanMrzValue())
            put("expiry_date", secondLine.substringSafe(21, 27).formatMrzDate())
        }.filterValues { it.isNotBlank() }
    }

    val td1 = mrzLines.windowed(3).firstOrNull { it[0].length >= 30 && it[1].length >= 30 && it[2].length >= 30 }
    if (td1 != null) {
        val firstLine = td1[0].padEnd(30, '<')
        val secondLine = td1[1].padEnd(30, '<')
        val nameParts = td1[2].split("<<", limit = 2)
        return buildMap {
            putNames(nameParts)
            val documentId = firstLine.substringSafe(5, 14).cleanMrzValue()
            val countryCode = secondLine.substringSafe(15, 18).cleanMrzValue().toIso2CountryCode()
            put("document_number", documentId)
            put("document_id", documentId)
            put("issuing_country", firstLine.substringSafe(2, 5).cleanMrzValue().toIso2CountryCode())
            put("birth_date", secondLine.substringSafe(0, 6).formatMrzDate())
            put("sex", secondLine.substringSafe(7, 8).cleanMrzValue())
            put("expiry_date", secondLine.substringSafe(8, 14).formatMrzDate())
            put("nationality", countryCode)
            put("country_code", countryCode)
        }.filterValues { it.isNotBlank() }
    }

    return parsePartialMrz(mrzLines)
}

private fun MutableMap<String, String>.putNames(nameParts: List<String>) {
    val familyName = nameParts.getOrNull(0).orEmpty().cleanMrzValue().toTitleCaseAscii()
    val givenName = nameParts.getOrNull(1).orEmpty().cleanMrzValue().toTitleCaseAscii()
    if (familyName.isNotBlank()) put("family_name", familyName)
    if (givenName.isNotBlank()) put("given_name", givenName)
}

private fun parsePartialMrz(mrzLines: List<String>): Map<String, String> = buildMap {
    val nameLine = mrzLines.firstOrNull { it.contains("<<") && it.any(Char::isLetter) }
    nameLine?.let { line ->
        when {
            line.startsWith("P<") && line.length > 5 -> line.drop(5)
            line.startsWith("P") && line.length > 4 -> line.drop(4)
            else -> line
        }
    }
        ?.split("<<", limit = 2)
        ?.let { putNames(it) }

    val dataLine = mrzLines.firstOrNull { line ->
        line.count(Char::isDigit) >= 12 && line.length >= 20
    }
    dataLine?.let { line ->
        val dates = Regex("\\d{6}").findAll(line).map { it.value }.toList()
        dates.getOrNull(0)?.formatMrzDate()?.let { put("birth_date", it) }
        dates.getOrNull(1)?.formatMrzDate()?.let { put("expiry_date", it) }
        Regex("[A-Z]{3}").find(line)?.value?.toIso2CountryCode()?.let {
            put("country_code", it)
            put("nationality", it)
        }
    }
}.filterValues { it.isNotBlank() }

private fun String.normalizeMrzLine(): String = uppercase()
    .replace('«', '<')
    .replace('‹', '<')
    .replace('＜', '<')
    .replace('⟨', '<')
    .replace(Regex("[^A-Z0-9<]"), "")

private fun String.isMrzLikeLine(): Boolean =
    length >= 12 && (
            count { it == '<' } >= 2 ||
                    startsWith("P") ||
                    startsWith("I") ||
                    startsWith("A") ||
                    count(Char::isDigit) >= 6
            )

private fun findLabeledValue(lines: List<String>, canonicalKey: String): String? {
    val aliases = aliasesForClaim(canonicalKey)
    lines.forEachIndexed { index, line ->
        val normalizedLine = line.normalizedForMatching()
        val matchedAlias = aliases.firstOrNull { alias ->
            normalizedLine.startsWith("$alias ") ||
                    normalizedLine.startsWith("$alias:") ||
                    normalizedLine.startsWith("$alias.") ||
                    normalizedLine == alias
        }
        if (matchedAlias != null) {
            val inlineValue = line.substringAfter(':', "").ifBlank {
                line.valueAfterNormalizedAlias(matchedAlias)
            }.trimLabelValue()
            return inlineValue.ifBlank { lines.getOrNull(index + 1)?.trimLabelValue() }
        }
    }
    return null
}

private fun findUnlabeledValue(lines: List<String>, canonicalKey: String): String? = when {
    canonicalKey.contains("date") -> lines.firstNotNullOfOrNull { extractDate(it) }
    canonicalKey.contains("document_number") ||
            canonicalKey.contains("document_id") ||
            canonicalKey.contains("administrative_number") ->
        lines.firstOrNull { it.matches(Regex(".*[A-Z0-9]{6,}.*", RegexOption.IGNORE_CASE)) }
            ?.replace(Regex("[^A-Za-z0-9]"), "")
    canonicalKey.contains("country_code") || canonicalKey.contains("issuing_country") ->
        lines.firstOrNull { it.trim().matches(Regex("[A-Z]{2,3}", RegexOption.IGNORE_CASE)) }
            ?.trim()
            ?.uppercase()
    else -> null
}

private fun aliasesForClaim(canonicalKey: String): List<String> = when {
    canonicalKey.contains("family_name") || canonicalKey.contains("last_name") -> listOf("family name", "surname", "last name", "name")
    canonicalKey.contains("given_name") || canonicalKey.contains("first_name") -> listOf("given names", "given name", "first name", "forenames")
    canonicalKey.contains("birth_date") || canonicalKey.contains("date_of_birth") -> listOf("date of birth", "birth date", "dob", "born")
    canonicalKey.contains("expiry_date") || canonicalKey.contains("expiration_date") -> listOf("expiry date", "expiration date", "valid until", "expires")
    canonicalKey.contains("issuance_date") || canonicalKey.contains("issue_date") -> listOf("date of issue", "issue date", "issued")
    canonicalKey.contains("document_number") || canonicalKey.contains("document_id") -> {
        listOf("document id", "document number", "document no", "passport no", "passport number", "id number", "licence number", "license number", "number")
    }
    canonicalKey.contains("issuing_authority") -> listOf("issuing authority", "authority")
    canonicalKey.contains("issuing_country") -> listOf("issuing country", "country of issue", "issuing state", "country")
    canonicalKey.contains("country_code") -> listOf("country code", "nationality", "country")
    canonicalKey.contains("nationality") -> listOf("nationality", "country code")
    canonicalKey.contains("sex") || canonicalKey.contains("gender") -> listOf("sex", "gender")
    canonicalKey.contains("address") -> listOf("address", "residence", "resident address")
    else -> listOf(canonicalKey.replace("_", " "))
}.map { it.normalizedForMatching() }

private fun String.canonicalClaimKey(): String = lowercase()
    .replace(".", "_")
    .replace("-", "_")
    .let {
        when (it) {
            "surname", "last_name" -> "family_name"
            "firstname", "first_name", "forename" -> "given_name"
            "passport_number", "document_no", "document_number" -> "document_id"
            "date_of_birth", "dob" -> "birth_date"
            "expiration_date" -> "expiry_date"
            "nationality" -> "country_code"
            "gender" -> "sex"
            else -> it
        }
    }

private fun String.normalizedForMatching(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private fun String.trimLabelValue(): String = trim()
    .trim(':', '.', '-', ' ')
    .takeIf { it.length >= 2 }
    .orEmpty()

private fun String.valueAfterNormalizedAlias(alias: String): String {
    val words = alias.split(" ").filter { it.isNotBlank() }
    if (words.isEmpty()) return ""
    var searchStart = 0
    words.forEach { word ->
        val match = Regex(Regex.escape(word), RegexOption.IGNORE_CASE).find(this, searchStart) ?: return ""
        searchStart = match.range.last + 1
    }
    return drop(searchStart)
}

private fun String.cleanMrzValue(): String = replace('<', ' ')
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.toTitleCaseAscii(): String = lowercase()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word ->
        word.replaceFirstChar { char -> char.uppercase() }
    }

private fun String.toIso2CountryCode(): String = uppercase().let { code ->
    when (code) {
        "AUT" -> "AT"
        "BEL" -> "BE"
        "BGR" -> "BG"
        "HRV" -> "HR"
        "CYP" -> "CY"
        "CZE" -> "CZ"
        "DNK" -> "DK"
        "EST" -> "EE"
        "FIN" -> "FI"
        "FRA" -> "FR"
        "DEU", "D<<" -> "DE"
        "GRC" -> "GR"
        "HUN" -> "HU"
        "IRL" -> "IE"
        "ITA" -> "IT"
        "LVA" -> "LV"
        "LTU" -> "LT"
        "LUX" -> "LU"
        "MLT" -> "MT"
        "NLD" -> "NL"
        "POL" -> "PL"
        "PRT" -> "PT"
        "ROU" -> "RO"
        "SVK" -> "SK"
        "SVN" -> "SI"
        "ESP" -> "ES"
        "SWE" -> "SE"
        "UKR" -> "UA"
        else -> code.takeIf { it.length == 2 } ?: code
    }
}

private fun String.substringSafe(startIndex: Int, endIndex: Int): String =
    if (length >= endIndex) substring(startIndex, endIndex) else ""

private fun String.formatMrzDate(): String {
    if (!matches(Regex("\\d{6}"))) return cleanMrzValue()
    val year = take(2).toIntOrNull() ?: return cleanMrzValue()
    val month = substring(2, 4)
    val day = substring(4, 6)
    val century = if (year > 30) "19" else "20"
    return "$century${take(2)}$month$day"
}

private fun extractDate(text: String): String? {
    Regex("\\b(\\d{4})(\\d{2})(\\d{2})\\b").find(text)?.let {
        return "${it.groupValues[1]}${it.groupValues[2]}${it.groupValues[3]}"
    }
    Regex("(\\d{4})[-./ ](\\d{1,2})[-./ ](\\d{1,2})").find(text)?.let {
        return "${it.groupValues[1]}${it.groupValues[2].padStart(2, '0')}${it.groupValues[3].padStart(2, '0')}"
    }
    Regex("(\\d{1,2})[-./ ](\\d{1,2})[-./ ](\\d{2,4})").find(text)?.let {
        val year = it.groupValues[3].let { value -> if (value.length == 2) "20$value" else value }
        return "$year${it.groupValues[2].padStart(2, '0')}${it.groupValues[1].padStart(2, '0')}"
    }
    return null
}
