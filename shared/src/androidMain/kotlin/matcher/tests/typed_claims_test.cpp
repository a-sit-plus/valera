#include "CredentialDatabase.h"
#include "dcql.h"
#include "cppbor.h"
#include <cassert>
#include <fstream>
#include <iostream>
#include <iterator>

using cppbor::Map;
using cppbor::Array;
static int checks = 0;
static Map scalar(std::string kind, std::string value, std::string display = "") {
    return Map("kind", kind, "value", value, "displayName", "label", "displayValue", display.empty() ? value : display);
}
static Map object(Map members = {}) { return Map("kind", "object", "members", std::move(members)); }
static Map array(Array elements = {}) { return Map("kind", "array", "elements", std::move(elements)); }
static Map tree() {
    return object(Map(
        "parent", object(Map("child", scalar("string", "nested"))),
        "parent.child", scalar("string", "literal"),
        "0", scalar("string", "zero"), "null", scalar("string", "literal-null"),
        "", scalar("string", "empty-name"),
        "groups", array(Array(object(Map("items", array(Array(scalar("string", "AT"), scalar("string", "DE"))))),
                              object(Map("items", array(Array(scalar("string", "FR"))))))),
        "mixed", array(Array(object(Map("x", scalar("string", "yes"))), scalar("string", "bad"))),
        "missing", array(Array(object(), object(Map("x", scalar("string", "yes"))))),
        "emptyObject", object(), "emptyArray", array(), "nil", Map("kind", "null"),
        "boolean", scalar("boolean", "true"), "textBoolean", scalar("string", "true"),
        "number", scalar("number", "42"), "textNumber", scalar("string", "42"),
        "decimal", scalar("number", "1.50"), "large", scalar("number", "2147483648"),
        "boundary", scalar("number", "9007199254740991"),
        "unsafe", scalar("number", "9007199254740993"),
        "precise", scalar("number", "0.10000000000000000001"),
        "empty", scalar("string", ""),
        "long", scalar("string", std::string(400, 'x'), "short display"),
        "cache", array(Array(scalar("string", "a b"), scalar("string", "a"), scalar("string", "b")))
    ));
}
static CredentialDatabase database(int version = 1) {
    Map sdjwt("documentId", "test", "vct", "urn:test", "claimsTree", tree());
    if (version >= 0) sdjwt.add("claimsFormatVersion", version);
    auto bytes = Map("protocols", Array("openid4vp"), "credentials", Array(
        Map("title", "Test", "subtitle", "Synthetic", "bitmap", std::vector<uint8_t>{}, "sdjwt", std::move(sdjwt))
    )).encode();
    return CredentialDatabase(bytes.data(), bytes.size());
}
static DcqlQuery parse(const std::string& query) {
    cJSON* json = cJSON_Parse(query.c_str());
    assert(json);
    auto result = DcqlQuery::parse(json);
    cJSON_Delete(json);
    return result;
}
static DcqlQuery query(const std::string& claim) {
    return parse("{\"credentials\":[{\"id\":\"q\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"urn:test\"]},\"claims\":[{" + claim + "}]}]}");
}
static void expect(CredentialDatabase& db, const std::string& claim, bool expected) {
    auto q = query(claim);
    auto result = q.execute(&db, "openid4vp");
    if (result.has_value() != expected) {
        std::cerr << "Unexpected match=" << result.has_value() << ": " << claim << '\n';
        std::abort();
    }
    ++checks;
}
static std::string readFile(const char* path) {
    std::ifstream stream(path, std::ios::binary);
    assert(stream);
    return {std::istreambuf_iterator<char>(stream), {}};
}
int main(int argc, char** argv) {
    if ((argc == 4 || argc == 5) && std::string(argv[1]) == "--fixture") {
        auto bytes = readFile(argv[2]);
        CredentialDatabase db(reinterpret_cast<const uint8_t*>(bytes.data()), bytes.size());
        auto q = parse(readFile(argv[3]));
        bool match = q.execute(&db, argc == 5 ? argv[4] : "openid4vp-v1-unsigned").has_value();
        std::cout << (match ? "MATCH" : "NO MATCH") << '\n';
        return match ? 0 : 1;
    }
    auto db = database();
    for (auto path : {"[\"parent\"]", "[\"parent\",\"child\"]", "[\"emptyObject\"]", "[\"emptyArray\"]", "[\"nil\"]",
                      "[\"groups\",null,\"items\"]", "[\"groups\",null,\"items\",null]", "[\"groups\",0,\"items\",1]"}) {
        expect(db, std::string("\"path\":") + path, true);
    }
    expect(db, "\"path\":[\"groups\",null,\"items\",null],\"values\":[\"FR\"]", true);
    expect(db, "\"path\":[\"groups\",0,\"items\",1],\"values\":[\"DE\"]", true);
    expect(db, "\"path\":[\"parent.child\"],\"values\":[\"literal\"]", true);
    expect(db, "\"path\":[\"parent\",\"child\"],\"values\":[\"literal\"]", false);
    expect(db, "\"path\":[\"0\"],\"values\":[\"zero\"]", true);
    expect(db, "\"path\":[\"null\"],\"values\":[\"literal-null\"]", true);
    expect(db, "\"path\":[\"\"],\"values\":[\"empty-name\"]", true);
    for (auto path : {"[\"absent\"]", "[\"emptyArray\",null]", "[\"groups\",99]", "[\"mixed\",null,\"x\"]",
                      "[\"boolean\",\"x\"]", "[\"parent\",0]", "[\"parent\",null]", "[null]", "[0]",
                      "[\"groups\",-1]", "[\"groups\",0.5]", "[\"groups\",true]", "[\"groups\",{}]",
                      "[\"groups\",9007199254740992]", "[]", "{}", "null"}) {
        expect(db, std::string("\"path\":") + path, false);
    }
    expect(db, "\"path\":[\"mixed\",null,\"x\"],\"values\":[\"yes\"]", false);
    expect(db, "\"path\":[\"missing\",null,\"x\"],\"values\":[\"yes\"]", true);
    for (auto path : {"emptyObject", "emptyArray", "nil"}) expect(db, std::string("\"path\":[\"") + path + "\"],\"values\":[\"\"]", false);
    expect(db, "\"path\":[\"boolean\"],\"values\":[true]", true);
    expect(db, "\"path\":[\"boolean\"],\"values\":[\"true\"]", false);
    expect(db, "\"path\":[\"textBoolean\"],\"values\":[true]", false);
    expect(db, "\"path\":[\"number\"],\"values\":[42]", true);
    expect(db, "\"path\":[\"number\"],\"values\":[\"42\"]", false);
    expect(db, "\"path\":[\"textNumber\"],\"values\":[42]", false);
    expect(db, "\"path\":[\"decimal\"],\"values\":[1.5]", true);
    expect(db, "\"path\":[\"large\"],\"values\":[2147483648]", true);
    expect(db, "\"path\":[\"boundary\"],\"values\":[9007199254740991]", true);
    expect(db, "\"path\":[\"unsafe\"],\"values\":[9007199254740993]", false);
    expect(db, "\"path\":[\"precise\"],\"values\":[0.1]", false);
    expect(db, "\"path\":[\"empty\"],\"values\":[\"\"]", true);
    expect(db, "\"path\":[\"empty\"],\"values\":[]", false);
    expect(db, "\"path\":[\"empty\"],\"values\":[null]", false);
    expect(db, "\"path\":[\"empty\"],\"values\":{}", false);
    expect(db, "\"path\":[\"long\"],\"values\":[\"" + std::string(400, 'x') + "\"]", true);
    expect(db, "\"path\":[\"long\"],\"values\":[\"short display\"]", false);
    auto first = query("\"path\":[\"cache\",null],\"values\":[\"a b\"]");
    auto second = query("\"path\":[\"cache\",null],\"values\":[\"a\",\"b\"]");
    auto r1 = first.execute(&db, "openid4vp");
    auto p1 = r1->credentialSets[0].options[0].members[0].matches[0].claims[0];
    auto r2 = second.execute(&db, "openid4vp");
    auto p2 = r2->credentialSets[0].options[0].members[0].matches[0].claims[0];
    assert(p1 != p2 && p1->value == "a b" && p2->value == "a, b"); ++checks;
    auto noValues = query("\"path\":[\"cache\",null]").execute(&db, "openid4vp");
    assert(noValues->credentialSets[0].options[0].members[0].matches[0].claims[0]->value == "a b, a, b"); ++checks;
    auto old = database(-1); expect(old, "\"path\":[\"parent\"]", false);
    auto future = database(2); expect(future, "\"path\":[\"parent\"]", false);
    // An unsupported tree version must fail even when no claims are requested.
    auto all = parse("{\"credentials\":[{\"id\":\"q\",\"format\":\"dc+sd-jwt\",\"meta\":{\"vct_values\":[\"urn:test\"]}}]}");
    assert(!all.execute(&old, "openid4vp")); ++checks;
    // A selected element of the wrong type is an error of that claim only (OpenID4VP 1.0, 7.1.1), like a missing
    // claim, so an alternative claim set without it still matches.
    auto alternatives = parse(R"({"credentials":[{"id":"q","format":"dc+sd-jwt","meta":{"vct_values":["urn:test"]},"claims":[{"id":"bad","path":["mixed",null,"x"]},{"id":"good","path":["parent"]}],"claim_sets":[["bad"],["good"]]}]})");
    auto alternativesResult = alternatives.execute(&db, "openid4vp");
    assert(alternativesResult); ++checks;
    assert(alternativesResult->credentialSets[0].options[0].members[0].matches[0].claims[0]->displayName == "parent"); ++checks;
    auto alternativesWithoutMissing = parse(R"({"credentials":[{"id":"q","format":"dc+sd-jwt","meta":{"vct_values":["urn:test"]},"claims":[{"id":"bad","path":["boolean","x"]},{"id":"absent","path":["absent"]}],"claim_sets":[["bad"],["absent"]]}]})");
    assert(!alternativesWithoutMissing.execute(&db, "openid4vp")); ++checks;
    // Without claim sets, every claim is requested, so one of the wrong type fails the credential query
    auto allRequested = parse(R"({"credentials":[{"id":"q","format":"dc+sd-jwt","meta":{"vct_values":["urn:test"]},"claims":[{"path":["parent","child"]},{"path":["parent",0]}]}]})");
    assert(!allRequested.execute(&db, "openid4vp")); ++checks;
    // A malformed claims path pointer makes the request invalid, even where an alternative claim set would match
    auto malformed = parse(R"({"credentials":[{"id":"q","format":"dc+sd-jwt","meta":{"vct_values":["urn:test"]},"claims":[{"id":"bad","path":["groups",-1]},{"id":"good","path":["parent"]}],"claim_sets":[["bad"],["good"]]}]})");
    assert(!malformed.execute(&db, "openid4vp")); ++checks;
    Array invalidNodes(Map("kind", "unknown"), Map("kind", "object", "members", Array()),
                         object(Map("bad", scalar("number", "01"))),
                         object(Map("bad", scalar("boolean", "truth"))));
    for (const auto& invalid : invalidNodes) {
        auto bytes = Map("protocols", Array("openid4vp"), "credentials", Array(Map(
            "title", "bad", "subtitle", "bad", "bitmap", std::vector<uint8_t>{},
            "sdjwt", Map("documentId", "bad", "vct", "urn:test", "claimsFormatVersion", 1, "claimsTree", invalid->clone())
        ))).encode();
        CredentialDatabase malformed(bytes.data(), bytes.size());
        assert(!all.execute(&malformed, "openid4vp")); ++checks;
    }
    auto mdocBytes = Map("protocols", Array("openid4vp"), "credentials", Array(Map(
        "title", "Mdoc", "subtitle", "Synthetic", "bitmap", std::vector<uint8_t>{},
        "mdoc", Map("documentId", "m", "docType", "org.test", "namespaces", Map("ns", Map("a", Array("A", "display", "match"))))
    ))).encode();
    CredentialDatabase mdoc(mdocBytes.data(), mdocBytes.size());
    auto mq = parse("{\"credentials\":[{\"id\":\"mdoc-query\",\"format\":\"mso_mdoc\",\"meta\":{\"doctype_value\":\"org.test\"},\"claims\":[{\"path\":[\"ns\",\"a\"],\"values\":[\"match\"]}]}]}");
    auto mr = mq.execute(&mdoc, "openid4vp");
    assert(mr); ++checks;
    assert(mr->credentialSets[0].options[0].members[0].matches[0].claims[0]->value == "display"); ++checks;
    for (auto invalidValues : {"[9007199254740992]", "[]", "[null]", "{}"}) {
        auto badMdocQuery = parse(std::string(R"({"credentials":[{"id":"m","format":"mso_mdoc","meta":{"doctype_value":"org.test"},"claims":[{"path":["ns","a"],"values":)") + invalidValues + "}]}]}");
        assert(!badMdocQuery.execute(&mdoc, "openid4vp")); ++checks;
    }
    for (auto invalidPath : {R"(["ns","a",true])", R"(["ns","a",-1])", R"(["ns","a",0.5])"}) {
        auto badMdocQuery = parse(std::string(R"({"credentials":[{"id":"m","format":"mso_mdoc","meta":{"doctype_value":"org.test"},"claims":[{"path":)") + invalidPath + "}]}]}");
        assert(!badMdocQuery.execute(&mdoc, "openid4vp")); ++checks;
    }
    std::cout << "PASS " << checks << " typed/native-parser regression checks\n";
}
