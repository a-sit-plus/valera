
#pragma once

#include <string>
#include <vector>
#include <optional>

#include "CredentialDatabase.h"

extern "C" {
#include "cJSON.h"
}

struct DcqlPathSegment {
    enum class Kind { Member, Index, Wildcard };
    Kind kind;
    std::string member;
    uint64_t index = 0;
};

struct DcqlScalarValue {
    std::string kind;
    std::string value;
};

struct DcqlRequestedClaim {
    std::string id;
    std::vector<std::string> values;
    std::vector<std::string> path;

    // Legacy strings above remain for mdoc/other request producers. SD-JWT uses only these
    // typed fields, populated by the DCQL parser, never joinPath().
    std::vector<DcqlPathSegment> typedPath;
    std::vector<DcqlScalarValue> typedValues;
    bool typedValid = false;
    bool invalidRequest = false;
    bool hasValueConstraint = false;

    DcqlRequestedClaim(std::string id_, std::vector<std::string> values_,
                       std::vector<std::string> path_, bool retain_)
        : id(std::move(id_)), values(std::move(values_)), path(std::move(path_)),
          intentToRetain(retain_) {}

    // Only relevant for ISO mdoc
    bool intentToRetain;

    std::string joinPath() const {
        std::string ret;
        for (const auto& pathElem : path) {
            if (!ret.empty()) {
                ret += "." + pathElem;
            } else {
                ret = pathElem;
            }
        }
        return ret;
    }
};

struct DcqlClaimSet {
    std::vector<std::string> claimIdentifiers;
};

struct DcqlCredentialQuery {
    std::string id;
    std::string format;

    std::string mdocDocType;
    std::vector<std::string> vctValues;

    std::vector<std::vector<uint8_t>> issuerIdentifiers;
    std::vector<std::vector<uint8_t>> readerAuthAkis;

    std::vector<DcqlRequestedClaim> requestedClaims;
    std::vector<DcqlClaimSet> claimSets;

    bool lenientClaimMatching = false;

    DcqlCredentialQuery(
            std::string id_,
            std::string format_,
            std::string mdocDocType_,
            std::vector<std::string> vctValues_,
            std::vector<std::vector<uint8_t>> issuerIdentifiers_,
            std::vector<std::vector<uint8_t>> readerAuthAkis_,
            std::vector<DcqlRequestedClaim> requestedClaims_,
            std::vector<DcqlClaimSet> claimSets_,
            bool lenientClaimMatching_ = false
    ) : id(id_), format(format_), mdocDocType(mdocDocType_), vctValues(vctValues_),
        issuerIdentifiers(issuerIdentifiers_), readerAuthAkis(readerAuthAkis_),
        requestedClaims(requestedClaims_), claimSets(claimSets_),
        lenientClaimMatching(lenientClaimMatching_) {}

    DcqlRequestedClaim* findRequestedClaim(const std::string& claimId);
};

struct DcqlResponse;

struct DcqlCredentialSetOption {
    std::vector<std::string> credentialIds;
};

struct DcqlCredentialSetQuery {
    bool required;
    std::vector<DcqlCredentialSetOption> options;
};

struct DcqlQuery {
    std::vector<DcqlCredentialQuery> dcqlCredentialQueries;
    std::vector<DcqlCredentialSetQuery> dcqlCredentialSetQueries;

    void log();

    std::optional<DcqlResponse> execute(CredentialDatabase* credentialDatabase, const std::string& protocol);

    static DcqlQuery parse(cJSON* dcqlQuery);
};

struct DcqlResponseCredentialSetOptionMemberMatch {
    Credential* credential;
    std::vector<Claim*> claims;
};

struct DcqlResponseCredentialSetOptionMember {
    std::vector<DcqlResponseCredentialSetOptionMemberMatch> matches;
};

struct DcqlResponseCredentialSetOption {
    std::vector<DcqlResponseCredentialSetOptionMember> members;
};

struct DcqlResponseCredentialSet {
    bool optional;
    std::vector<DcqlResponseCredentialSetOption> options;

    DcqlResponseCredentialSet consolidateSingleMemberOptions() const;
};

struct DcqlResponse {
    std::vector<DcqlResponseCredentialSet> credentialSets;

    std::vector<DcqlResponseCredentialSet> consolidateCredentialSets() const;
    std::vector<Combination> getCredentialCombinations();
};
