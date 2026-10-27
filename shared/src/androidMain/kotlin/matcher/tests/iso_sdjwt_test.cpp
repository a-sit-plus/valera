// ISO/IEC 18013-7 Annex C requests for SD-JWT VCs as proposed by WG10 N2933 v2: a DocRequest with
// `docFormat` `dc+sd-jwt`, the vct as `docType`, claims in the namespace `_` mapped to typed claim paths.
#include "CredentialDatabase.h"
#include "Request.h"
#include "cppbor.h"
#include <cassert>
#include <iostream>
#include <memory>
#include <optional>

using cppbor::Array;
using cppbor::Bool;
using cppbor::Bstr;
using cppbor::Map;
using cppbor::Nint;
using cppbor::Null;
using cppbor::SemanticTag;

static int checks = 0;

static Map scalar(std::string value) {
    return Map("kind", "string", "value", value, "displayName", "label", "displayValue", value);
}
static Map object(Map members) { return Map("kind", "object", "members", std::move(members)); }
static Map array(Array elements) { return Map("kind", "array", "elements", std::move(elements)); }

static CredentialDatabase database() {
    auto tree = object(Map(
        "given_name", scalar("Erika"),
        "address", object(Map("postal_code", scalar("1010"), "locality", scalar("Wien"))),
        "nationalities", array(Array(scalar("AT"), scalar("DE")))
    ));
    auto bytes = Map("protocols", Array("org-iso-mdoc"), "credentials", Array(
        Map("title", "PID", "subtitle", "Synthetic", "bitmap", std::vector<uint8_t>{},
            "sdjwt", Map("documentId", "sd", "vct", "urn:test", "claimsFormatVersion", 1, "claimsTree", std::move(tree))),
        Map("title", "Mdoc", "subtitle", "Synthetic", "bitmap", std::vector<uint8_t>{},
            "mdoc", Map("documentId", "m", "docType", "org.test", "namespaces", Map("ns", Map("a", Array("A", "A", "A")))))
    )).encode();
    return CredentialDatabase(bytes.data(), bytes.size());
}

static std::string base64Url(const std::vector<uint8_t>& data) {
    static const char* alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    std::string out;
    size_t i = 0;
    for (; i + 2 < data.size(); i += 3) {
        uint32_t n = (data[i] << 16) | (data[i + 1] << 8) | data[i + 2];
        for (int s = 18; s >= 0; s -= 6) out += alphabet[(n >> s) & 63];
    }
    if (i + 1 == data.size()) {
        uint32_t n = data[i] << 16;
        out += alphabet[(n >> 18) & 63];
        out += alphabet[(n >> 12) & 63];
    } else if (i + 2 == data.size()) {
        uint32_t n = (data[i] << 16) | (data[i + 1] << 8);
        out += alphabet[(n >> 18) & 63];
        out += alphabet[(n >> 12) & 63];
        out += alphabet[(n >> 6) & 63];
    }
    return out;
}

/** An Annex C request of [docRequest] items requests, with `version` [version]. */
static bool matches(CredentialDatabase& db, Map itemsRequest, const std::string& version = "1.0") {
    auto itemsRequestBytes = itemsRequest.encode();
    auto deviceRequest = Map(
        "version", version,
        "docRequests", Array(Map("itemsRequest", SemanticTag(24, Bstr(itemsRequestBytes))))
    ).encode();
    std::string json = "{\"deviceRequest\":\"" + base64Url(deviceRequest) + "\"}";
    cJSON* data = cJSON_Parse(json.c_str());
    assert(data);
    auto request = MdocRequest::parseMdocApi("org-iso-mdoc", data);
    cJSON_Delete(data);
    assert(request);
    ++checks;
    return request->dcqlQuery.execute(&db, "org-iso-mdoc").has_value();
}

static Map mapping() {
    return Map(
        "given_name", Array("given_name"),
        "postal_code", Array("address", "postal_code"),
        "locality", Array("address", "locality"),
        "absent", Array("address", "absent"),
        "first_nationality", Array("nationalities", 0),
        "nationalities", Array("nationalities", Null()),
        "negative", Array("nationalities", Nint(-1)),
        "boolean", Array("given_name", Bool(true)),
        "empty", Array()
    );
}

static Map sdJwtRequest(Map elements, std::string vct = "urn:test", std::string ns = "_",
                        std::optional<Array> alternatives = std::nullopt) {
    Map requestInfo("docFormat", "dc+sd-jwt", "dataElementIdentifierMapping", mapping());
    if (alternatives) requestInfo.add("alternativeDataElements", std::move(*alternatives));
    return Map("docType", vct, "nameSpaces", Map(ns, std::move(elements)), "requestInfo", std::move(requestInfo));
}

static Map requested(std::initializer_list<const char*> elements) {
    Map map;
    for (auto element : elements) map.add(element, Bool(false));
    return map;
}

int main() {
    auto db = database();

    // Claim paths of text, index, and null components select claims of the typed claims tree
    for (auto element : {"given_name", "postal_code", "first_nationality", "nationalities"}) {
        if (!matches(db, sdJwtRequest(requested({element})))) {
            std::cerr << "Expected a match for " << element << '\n';
            std::abort();
        }
    }
    assert(matches(db, sdJwtRequest(requested({"given_name", "postal_code"}))));

    // Another vct, a claim the credential lacks, an element without a valid mapping, or another namespace match nothing
    assert(!matches(db, sdJwtRequest(requested({"given_name"}), "urn:other")));
    assert(!matches(db, sdJwtRequest(requested({"absent"}))));
    for (auto invalid : {"unmapped", "negative", "boolean", "empty"}) {
        if (matches(db, sdJwtRequest(requested({"given_name", invalid})))) {
            std::cerr << "Expected no match for " << invalid << '\n';
            std::abort();
        }
    }
    assert(!matches(db, sdJwtRequest(requested({"given_name"}), "urn:test", "org.test.ns")));

    // Every claim of an SD-JWT VC is required, also in a request of version 1.0, unlike the elements of an mdoc
    assert(!matches(db, sdJwtRequest(requested({"given_name", "absent"}))));
    assert(matches(db, Map("docType", "org.test", "nameSpaces", Map("ns", requested({"a", "missing"})))));

    // An alternative set satisfies a missing element
    // Array(Array(...)) with one argument would move instead of nesting
    Array alternativeSet;
    alternativeSet.add(Array("_", "locality"));
    Array alternativeSets;
    alternativeSets.add(std::move(alternativeSet));
    auto alternatives = Array(Map(
        "requestedElement", Array("_", "absent"),
        "alternativeElementSets", std::move(alternativeSets)
    ));
    assert(matches(db, sdJwtRequest(requested({"absent"}), "urn:test", "_", std::move(alternatives))));

    // Without requested claims, only the claims that are always disclosed are requested
    assert(matches(db, sdJwtRequest(requested({}))));

    std::cout << "PASS " << checks << " ISO Annex C SD-JWT request checks\n";
}
