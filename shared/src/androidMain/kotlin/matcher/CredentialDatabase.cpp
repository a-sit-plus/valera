
#include "CredentialDatabase.h"
#include "Request.h"

extern "C" {
#include "credentialmanager.h"
}

#include "cppbor.h"
#include "cppbor_parse.h"
#include "logger.h"

static std::string canonicalNumber(const std::string& input);

// Reject malformed typed trees instead of reconstructing ambiguous flattened paths.
static bool parseSdJwtNode(const cppbor::Item* item, SdJwtClaimNode& node, size_t depth = 0) {
    if (item == nullptr || item->asMap() == nullptr || depth > 256) return false;
    auto map = item->asMap();
    const auto& kind = map->get("kind");
    if (!kind || !kind->asTstr()) return false;
    node.kind = kind->asTstr()->value();
    if (node.kind != "object" && node.kind != "array" && node.kind != "string" &&
        node.kind != "number" && node.kind != "boolean" && node.kind != "null") return false;
    auto text = [&](const char* key, std::string& target) {
        const auto& value = map->get(key);
        if (!value) return true;
        if (!value->asTstr()) return false;
        target = value->asTstr()->value();
        return true;
    };
    if (!text("value", node.scalar) || !text("displayName", node.display.displayName) ||
        !text("displayValue", node.display.value)) return false;
    node.display.matchValue = node.scalar;
    const auto& members = map->get("members");
    const auto& elements = map->get("elements");
    if (members) {
        if (node.kind != "object" || !members->asMap()) return false;
        for (const auto& [key, value] : *members->asMap()) {
            if (!key->asTstr()) return false;
            SdJwtClaimNode child;
            if (!parseSdJwtNode(value.get(), child, depth + 1) ||
                !node.members.emplace(key->asTstr()->value(), std::move(child)).second) return false;
        }
    }
    if (elements) {
        if (node.kind != "array" || !elements->asArray()) return false;
        for (const auto& value : *elements->asArray()) {
            SdJwtClaimNode child;
            if (!parseSdJwtNode(value.get(), child, depth + 1)) return false;
            node.elements.push_back(std::move(child));
        }
    }
    if (node.kind == "boolean" && node.scalar != "true" && node.scalar != "false") return false;
    if (node.kind == "number" && canonicalNumber(node.scalar).empty()) return false;
    return true;
}

CredentialDatabase::CredentialDatabase(const uint8_t* encodedDatabase, size_t encodedDatabaseLength) {
    auto [item, pos, message] =
            cppbor::parse(encodedDatabase, encodedDatabaseLength);
    if (item == nullptr) {
        printf("Error parsing CBOR: %s\n", message.c_str());
        return;
    }
    auto topMap = item->asMap();
    if (!topMap) return;

    const auto& enforceIdentifierMatching =
            topMap->get("enforceTrustedAuthorityAndReaderIdentifierMatching");
    if (enforceIdentifierMatching != nullptr && enforceIdentifierMatching->asBool() != nullptr) {
        enforceTrustedAuthorityAndReaderIdentifierMatching =
                enforceIdentifierMatching->asBool()->value();
    }

    std::vector<std::string> topProtocols;
    auto protocolsArray = topMap->get("protocols")->asArray();
    for (auto p = protocolsArray->begin(); p != protocolsArray->end(); ++p) {
        topProtocols.push_back((*p)->asTstr()->value());
    }

    auto credentialsArray = topMap->get("credentials")->asArray();
    for (auto i = credentialsArray->begin(); i != credentialsArray->end(); ++i) {
        auto cred = (*i)->asMap();
        auto title = cred->get("title")->asTstr()->value();
        auto subtitle = cred->get("subtitle")->asTstr()->value();
        auto bitmap = cred->get("bitmap")->asBstr()->value();

        std::string documentId = "";
        std::string mdocDoctype = "";
        std::string vcVct = "";
        std::vector<std::string> docProtocols = topProtocols;
        std::vector<std::vector<uint8_t>> issuerIdentifiers;
        std::vector<std::vector<uint8_t>> readerIdentifiers;
        std::vector<std::string> keyAuthorizedNamespaces;
        std::map<std::string, std::vector<std::string>> keyAuthorizedDataElements;
        bool supportsPortraitImageEquivalence = false;
        bool omitClaims = false;
        std::map<std::string, std::string> claimDisplayNames;
        std::optional<SdJwtClaimNode> sdJwtTree;
        std::map resultingClaims = std::map<std::string, Claim>();

        auto& docProtocolsPtr = cred->get("protocols");
        if (docProtocolsPtr != nullptr) {
            auto docProtocolsArray = docProtocolsPtr->asArray();
            if (docProtocolsArray != nullptr) {
                docProtocols.clear();
                for (auto p = docProtocolsArray->begin(); p != docProtocolsArray->end(); ++p) {
                    docProtocols.push_back((*p)->asTstr()->value());
                }
            }
        }

        auto& mdocPtr = cred->get("mdoc");
        if (mdocPtr != nullptr) {
            auto mdoc = mdocPtr->asMap();
            documentId = mdoc->get("documentId")->asTstr()->value();
            mdocDoctype = mdoc->get("docType")->asTstr()->value();

            const auto& piePtr = mdoc->get("supportsPortraitImageEquivalence");
            if (piePtr != nullptr && piePtr->asBool() != nullptr) {
                supportsPortraitImageEquivalence = piePtr->asBool()->value();
            }

            const auto& omitClaimsPtr = mdoc->get("omitClaims");
            if (omitClaimsPtr != nullptr && omitClaimsPtr->asBool() != nullptr) {
                omitClaims = omitClaimsPtr->asBool()->value();
            }

            const auto& claimDisplayNamesPtr = mdoc->get("claimDisplayNames");
            if (claimDisplayNamesPtr != nullptr && claimDisplayNamesPtr->asMap() != nullptr) {
                auto cdnMap = claimDisplayNamesPtr->asMap();
                for (auto it = cdnMap->begin(); it != cdnMap->end(); ++it) {
                    if (it->first->asTstr() != nullptr && it->second->asTstr() != nullptr) {
                        claimDisplayNames[it->first->asTstr()->value()] = it->second->asTstr()->value();
                    }
                }
            }

            const auto& issuerIdentifiersPtr = mdoc->get("issuerIdentifiers");
            if (issuerIdentifiersPtr != nullptr && issuerIdentifiersPtr->asArray() != nullptr) {
                auto arr = issuerIdentifiersPtr->asArray();
                for (auto it = arr->begin(); it != arr->end(); ++it) {
                    if ((*it)->asBstr() != nullptr) {
                        issuerIdentifiers.push_back((*it)->asBstr()->value());
                    }
                }
            }

            const auto& readerIdentifiersPtr = mdoc->get("readerIdentifiers");
            if (readerIdentifiersPtr != nullptr && readerIdentifiersPtr->asArray() != nullptr) {
                auto arr = readerIdentifiersPtr->asArray();
                for (auto it = arr->begin(); it != arr->end(); ++it) {
                    if ((*it)->asBstr() != nullptr) {
                        readerIdentifiers.push_back((*it)->asBstr()->value());
                    }
                }
            }

            const auto& keyAuthPtr = mdoc->get("keyAuthorizations");
            if (keyAuthPtr != nullptr && keyAuthPtr->asMap() != nullptr) {
                auto keyAuthMap = keyAuthPtr->asMap();
                const auto& nsArrPtr = keyAuthMap->get("nameSpaces");
                if (nsArrPtr != nullptr && nsArrPtr->asArray() != nullptr) {
                    auto arr = nsArrPtr->asArray();
                    for (auto it = arr->begin(); it != arr->end(); ++it) {
                        if ((*it)->asTstr() != nullptr) {
                            keyAuthorizedNamespaces.push_back((*it)->asTstr()->value());
                        }
                    }
                }
                const auto& deMapPtr = keyAuthMap->get("dataElements");
                if (deMapPtr != nullptr && deMapPtr->asMap() != nullptr) {
                    auto deMap = deMapPtr->asMap();
                    for (auto it = deMap->begin(); it != deMap->end(); ++it) {
                        std::string nsName = it->first->asTstr()->value();
                        auto deList = it->second->asArray();
                        if (deList != nullptr) {
                            for (auto deIt = deList->begin(); deIt != deList->end(); ++deIt) {
                                if ((*deIt)->asTstr() != nullptr) {
                                    keyAuthorizedDataElements[nsName].push_back((*deIt)->asTstr()->value());
                                }
                            }
                        }
                    }
                }
            }

            const auto& namespacesPtr = mdoc->get("namespaces");
            if (namespacesPtr != nullptr && namespacesPtr->asMap() != nullptr) {
                auto namespaces = namespacesPtr->asMap();
                for (auto j = namespaces->begin(); j != namespaces->end(); ++j) {
                    auto namespaceName = j->first->asTstr()->value();
                    auto dataElementsMap = j->second->asMap();

                    for (auto k = dataElementsMap->begin(); k != dataElementsMap->end(); ++k) {
                        auto dataElementName = k->first->asTstr()->value();
                        auto dataElementDetailsArray = k->second->asArray();
                        auto displayName = dataElementDetailsArray->get(0)->asTstr()->value();
                        auto value = dataElementDetailsArray->get(1)->asTstr()->value();
                        auto matchValue = dataElementDetailsArray->get(2)->asTstr()->value();

                        auto combinedName = namespaceName + "." + dataElementName;
                        resultingClaims[combinedName] = Claim(combinedName, displayName, value, matchValue);
                    }
                }
            }
        }

        auto& sdjwtPtr = cred->get("sdjwt");
        if (sdjwtPtr != nullptr) {
            auto sdjwt = sdjwtPtr->asMap();
            if (!sdjwt || !sdjwt->get("documentId") || !sdjwt->get("documentId")->asTstr() ||
                !sdjwt->get("vct") || !sdjwt->get("vct")->asTstr()) continue;
            documentId = sdjwt->get("documentId")->asTstr()->value();
            vcVct = sdjwt->get("vct")->asTstr()->value();

            const auto& omitClaimsPtr = sdjwt->get("omitClaims");
            if (omitClaimsPtr != nullptr && omitClaimsPtr->asBool() != nullptr) {
                omitClaims = omitClaimsPtr->asBool()->value();
            }

            const auto& claimDisplayNamesPtr = sdjwt->get("claimDisplayNames");
            if (claimDisplayNamesPtr != nullptr && claimDisplayNamesPtr->asMap() != nullptr) {
                auto cdnMap = claimDisplayNamesPtr->asMap();
                for (auto it = cdnMap->begin(); it != cdnMap->end(); ++it) {
                    if (it->first->asTstr() != nullptr && it->second->asTstr() != nullptr) {
                        claimDisplayNames[it->first->asTstr()->value()] = it->second->asTstr()->value();
                    }
                }
            }

            const auto& issuerIdentifiersPtr = sdjwt->get("issuerIdentifiers");
            if (issuerIdentifiersPtr != nullptr && issuerIdentifiersPtr->asArray() != nullptr) {
                auto arr = issuerIdentifiersPtr->asArray();
                for (auto it = arr->begin(); it != arr->end(); ++it) {
                    if ((*it)->asBstr() != nullptr) {
                        issuerIdentifiers.push_back((*it)->asBstr()->value());
                    }
                }
            }

            const auto& readerIdentifiersPtr = sdjwt->get("readerIdentifiers");
            if (readerIdentifiersPtr != nullptr && readerIdentifiersPtr->asArray() != nullptr) {
                auto arr = readerIdentifiersPtr->asArray();
                for (auto it = arr->begin(); it != arr->end(); ++it) {
                    if ((*it)->asBstr() != nullptr) {
                        readerIdentifiers.push_back((*it)->asBstr()->value());
                    }
                }
            }

            const auto& version = sdjwt->get("claimsFormatVersion");
            const auto& tree = sdjwt->get("claimsTree");
            SdJwtClaimNode parsed;
            if (version && version->asUint() && version->asUint()->value() == 1 &&
                parseSdJwtNode(tree.get(), parsed) && parsed.kind == "object") {
                sdJwtTree = std::move(parsed);
            }

        }

        credentials.push_back(
            Credential(
                title, subtitle, bitmap,
                documentId,
                mdocDoctype,
                vcVct,
                docProtocols,
                issuerIdentifiers,
                readerIdentifiers,
                keyAuthorizedNamespaces,
                keyAuthorizedDataElements,
                resultingClaims,
                supportsPortraitImageEquivalence,
                omitClaims,
                claimDisplayNames
            )
        );
        credentials.back().sdJwtClaimsTree = std::move(sdJwtTree);
    }
}

bool Credential::supportsProtocol(const std::string& protocol) {
    for (const auto& p: protocols) {
        if (p == protocol) {
            return true;
        }
    }
    return false;
}

static bool isNumericSegment(const std::string& seg) {
    if (seg.empty()) {
        return false;
    }
    for (char c : seg) {
        if (c < '0' || c > '9') {
            return false;
        }
    }
    return true;
}

std::string Credential::lookupClaimDisplayName(const std::vector<std::string>& path) const {
    if (path.empty()) {
        return "";
    }
    std::string joinedPath;
    for (size_t i = 0; i < path.size(); ++i) {
        if (i > 0) {
            joinedPath += ".";
        }
        joinedPath += path[i];
    }
    auto it = claimDisplayNames.find(joinedPath);
    if (it != claimDisplayNames.end()) {
        return it->second;
    }
    if (!vcVct.empty()) {
        std::string strippedPath;
        std::string lastStringSegment;
        for (const auto& seg : path) {
            if (seg == "null" || isNumericSegment(seg)) {
                continue;
            }
            if (!strippedPath.empty()) {
                strippedPath += ".";
            }
            strippedPath += seg;
            lastStringSegment = seg;
        }
        if (!strippedPath.empty()) {
            auto strippedIt = claimDisplayNames.find(strippedPath);
            if (strippedIt != claimDisplayNames.end()) {
                return strippedIt->second;
            }
        }
        if (!lastStringSegment.empty()) {
            return lastStringSegment;
        }
    }
    return path.back();
}

// Canonical decimal form compares the numeric value without rounding the stored lexical
// content to a double. cJSON query values are printed once by the request parser.
static std::string canonicalNumber(const std::string& input) {
    // Validate JSON number grammar independently: no whitespace, prefix parses, leading
    // zeros or missing fraction/exponent digits. Never round registry values to doubles.
    size_t cursor = 0;
    auto digit = [&](size_t p) { return p < input.size() && input[p] >= '0' && input[p] <= '9'; };
    if (cursor < input.size() && input[cursor] == '-') ++cursor;
    if (!digit(cursor)) return "";
    if (input[cursor] == '0') ++cursor;
    else while (digit(cursor)) ++cursor;
    if (cursor < input.size() && input[cursor] == '.') {
        ++cursor;
        if (!digit(cursor)) return "";
        while (digit(cursor)) ++cursor;
    }
    if (cursor < input.size() && (input[cursor] == 'e' || input[cursor] == 'E')) {
        ++cursor;
        if (cursor < input.size() && (input[cursor] == '+' || input[cursor] == '-')) ++cursor;
        if (!digit(cursor)) return "";
        while (digit(cursor)) ++cursor;
    }
    if (cursor != input.size()) return "";
    size_t i = 0;
    bool negative = i < input.size() && input[i] == '-';
    if (negative) ++i;
    std::string digits;
    int64_t exponent = 0;
    bool fraction = false;
    bool sawDigit = false;
    for (; i < input.size() && input[i] != 'e' && input[i] != 'E'; ++i) {
        char c = input[i];
        if (c == '.' && !fraction) { fraction = true; continue; }
        if (c < '0' || c > '9') return "";
        sawDigit = true;
        digits += c;
        if (fraction) --exponent;
    }
    if (!sawDigit) return "";
    if (i < input.size()) {
        ++i;
        bool expNegative = i < input.size() && input[i] == '-';
        if (i < input.size() && (input[i] == '-' || input[i] == '+')) ++i;
        if (i == input.size()) return "";
        int64_t exp = 0;
        for (; i < input.size(); ++i) {
            if (input[i] < '0' || input[i] > '9' || exp > 1000000) return "";
            exp = exp * 10 + input[i] - '0';
        }
        exponent += expNegative ? -exp : exp;
    }
    auto first = digits.find_first_not_of('0');
    if (first == std::string::npos) return "0";
    digits.erase(0, first);
    while (digits.back() == '0') { digits.pop_back(); ++exponent; }
    return (negative ? "-" : "") + digits + "e" + std::to_string(exponent);
}

static void appendCachePart(std::string& key, const std::string& part) {
    key += std::to_string(part.size()) + ":" + part;
}

// Processes the claims path pointer as in OpenID4VP 1.0, 7.1.1: a selected element of the wrong type for the
// next component, like a selection that becomes empty, is an error of this claim, so the claim does not match,
// while other claim sets of the credential query may still match.
static Claim* findSdJwtClaim(Credential& credential, const DcqlRequestedClaim& request) {
    if (!request.typedValid) return nullptr;
    if (!credential.sdJwtClaimsTree) return nullptr;
    std::vector<const SdJwtClaimNode*> selected = {&*credential.sdJwtClaimsTree};
    std::string key;
    for (const auto& segment : request.typedPath) {
        key += std::to_string(static_cast<int>(segment.kind));
        appendCachePart(key, segment.kind == DcqlPathSegment::Kind::Member ? segment.member :
                        segment.kind == DcqlPathSegment::Kind::Index ? std::to_string(segment.index) : "");
        std::vector<const SdJwtClaimNode*> next;
        for (auto node : selected) {
            if (segment.kind == DcqlPathSegment::Kind::Member) {
                if (node->kind != "object") return nullptr;
                auto found = node->members.find(segment.member);
                if (found != node->members.end()) next.push_back(&found->second);
            } else {
                if (node->kind != "array") return nullptr;
                if (segment.kind == DcqlPathSegment::Kind::Wildcard) {
                    for (const auto& child : node->elements) next.push_back(&child);
                } else if (segment.index < node->elements.size()) {
                    next.push_back(&node->elements[segment.index]);
                }
            }
        }
        selected = std::move(next);
    }
    key += request.hasValueConstraint ? "V" : "N";
    for (const auto& value : request.typedValues) {
        appendCachePart(key, value.kind);
        appendCachePart(key, value.value);
    }
    std::vector<const SdJwtClaimNode*> matches;
    for (auto node : selected) {
        bool match = !request.hasValueConstraint;
        for (const auto& value : request.typedValues) {
            if (node->kind != value.kind) continue;
            if (value.kind == "number") {
                auto number = canonicalNumber(node->scalar);
                match = match || (!number.empty() && number == canonicalNumber(value.value));
            } else {
                match = match || node->scalar == value.value;
            }
        }
        if (match) matches.push_back(node);
    }
    if (matches.empty()) return nullptr;
    auto cached = credential.sdJwtQueryClaims.find(key);
    if (cached != credential.sdJwtQueryClaims.end()) return &cached->second;
    Claim display = matches.front()->display;
    // claimName is a collision-free typed identifier, never used for structural lookup.
    display.claimName = key;
    if (display.displayName.empty()) display.displayName = request.path.back();
    for (size_t i = 1; i < matches.size(); ++i) {
        display.value += ", " + matches[i]->display.value;
    }
    return &credential.sdJwtQueryClaims.emplace(key, std::move(display)).first->second;
}

Claim* Credential::findMatchingClaim(const DcqlRequestedClaim& requestedClaim) {
    if (!vcVct.empty()) return findSdJwtClaim(*this, requestedClaim);
    // Malformed/unsafe constraints must not turn into an unconstrained legacy mdoc
    // request when the parser intentionally omitted an unrepresentable value.
    if (requestedClaim.invalidRequest) return nullptr;
    auto joinedPath = requestedClaim.joinPath();
    if (omitClaims) {
        bool isDeviceSigned = false;
        if (requestedClaim.path.size() == 2) {
            const std::string& ns = requestedClaim.path[0];
            const std::string& elem = requestedClaim.path[1];
            if (ns == "org.iso.transactiondata") {
                isDeviceSigned = true;
            } else if (ns == "org.iso.23220.5.1" && elem == "CHV_1") {
                isDeviceSigned = true;
            }
        }
        auto dynIt = dynamicDeviceClaims.find(joinedPath);
        if (dynIt == dynamicDeviceClaims.end()) {
            std::string displayName = isDeviceSigned ? "" : lookupClaimDisplayName(requestedClaim.path);
            auto [newIt, _] = dynamicDeviceClaims.emplace(
                joinedPath,
                Claim(joinedPath, displayName, "", "", isDeviceSigned, /* omitValue = */ true)
            );
            return &(newIt->second);
        }
        return &(dynIt->second);
    }

    auto ret = claims.find(joinedPath);
    if (ret != claims.end()) {
        // Perform value matching, if requested
        if (!requestedClaim.values.empty()) {
            const std::vector<std::string>& values = requestedClaim.values;
            if (std::find(values.begin(), values.end(), ret->second.matchValue) == values.end()) {
                return nullptr;
            }
        }
        return &(ret->second);
    }

    if (!requestedClaim.values.empty()) {
        return nullptr;
    }

    // Check KeyAuthorizations for device-signed data elements
    if (requestedClaim.path.size() == 2) {
        const std::string& ns = requestedClaim.path[0];
        const std::string& elem = requestedClaim.path[1];
        bool authorized = false;
        if (!vcVct.empty() && ns == "org.iso.transactiondata") {
            authorized = true;
        } else if (ns == "org.iso.23220.5.1") {
            if (elem == "CHV_1" && supportsPortraitImageEquivalence) {
                if (std::find(keyAuthorizedNamespaces.begin(), keyAuthorizedNamespaces.end(), ns) != keyAuthorizedNamespaces.end()) {
                    authorized = true;
                } else {
                    auto it = keyAuthorizedDataElements.find(ns);
                    if (it != keyAuthorizedDataElements.end()) {
                        if (std::find(it->second.begin(), it->second.end(), elem) != it->second.end()) {
                            authorized = true;
                        }
                    }
                }
            }
        } else if (std::find(keyAuthorizedNamespaces.begin(), keyAuthorizedNamespaces.end(), ns) != keyAuthorizedNamespaces.end()) {
            authorized = true;
        } else {
            auto it = keyAuthorizedDataElements.find(ns);
            if (it != keyAuthorizedDataElements.end()) {
                if (std::find(it->second.begin(), it->second.end(), elem) != it->second.end()) {
                    authorized = true;
                }
            }
        }
        if (authorized) {
            auto dynIt = dynamicDeviceClaims.find(joinedPath);
            if (dynIt == dynamicDeviceClaims.end()) {
                auto [newIt, _] = dynamicDeviceClaims.emplace(
                    joinedPath,
                    Claim(joinedPath, "", "", "", /* isDeviceSigned = */ true)
                );
                return &(newIt->second);
            }
            return &(dynIt->second);
        }
    }

    return nullptr;
}

void Combination::addToCredmanPicker(const Request& request) const {
    uint32_t credmanRuntimeVersion = 0;
    GetWasmVersion(&credmanRuntimeVersion);

    std::string setIdStr = std::to_string(combinationNumber) + " " + request.protocol;
    char* setId = strdup(setIdStr.c_str());
    int setLength = elements.size();

    if (credmanRuntimeVersion >= 2) {
        AddEntrySet(setId, setLength);
    }

    int setIndex = 0;
    for (const auto& element : elements) {
        for (const auto& match: element.matches) {
            Credential *credential = match.credential;
            std::string entryIdStr =
                    std::to_string(combinationNumber) + " " + request.protocol + " " +
                    credential->documentId;
            // The wallet presents the credential for the query the user selected it for, whose claims are shown
            if (!match.credentialQueryId.empty()) {
                entryIdStr += " " + match.credentialQueryId;
            }
            char *entryId = (char *) strdup(entryIdStr.c_str());

            void *icon = nullptr;
            if (credential->bitmap.size() > 0) {
                icon = malloc(credential->bitmap.size());
                memcpy(icon, credential->bitmap.data(), credential->bitmap.size());
            }
            if (credmanRuntimeVersion >= 2) {
                std::string subtitle = credential->subtitle + " (via " + request.protocol + ")";
                ::AddEntryToSet(
                        entryId,
                        (char *) icon,
                        credential->bitmap.size(),
                        (char *) strdup(credential->title.c_str()),
                        (char *) strdup(subtitle.c_str()),
                        nullptr,
                        nullptr,
                        nullptr,
                        setId,
                        setIndex
                );
            } else {
                ::AddStringIdEntry(
                        entryId,
                        (char *) icon,
                        credential->bitmap.size(),
                        (char *) strdup(credential->title.c_str()),
                        (char *) strdup(credential->subtitle.c_str()),
                        nullptr,
                        nullptr
                );
            }

            if (match.claims.empty()) {
                if (credmanRuntimeVersion >= 2) {
                    ::AddFieldToEntrySet(entryId,
                                         strdup("Requested attributes"),
                                         strdup("All (mandatory) attributes"),
                                         setId,
                                         setIndex
                    );
                } else {
                    ::AddFieldForStringIdEntry(entryId,
                                               strdup("Requested attributes"),
                                               strdup("All (mandatory) attributes")
                    );
                }
            }

            for (const auto &claim: match.claims) {
                if (claim->isDeviceSigned) {
                    continue;
                }
                char *fieldValue = claim->omitValue ? nullptr : strdup(claim->value.c_str());
                if (credmanRuntimeVersion >= 2) {
                    ::AddFieldToEntrySet(entryId,
                                         strdup(claim->displayName.c_str()),
                                         fieldValue,
                                         setId,
                                         setIndex
                    );
                } else {
                    ::AddFieldForStringIdEntry(entryId,
                                               strdup(claim->displayName.c_str()),
                                               fieldValue
                    );
                }
            }

            if (credmanRuntimeVersion < 2) {
                break;
            }
        }

        setIndex++;
        if (credmanRuntimeVersion < 2) {
            break;
        }
    }
}
