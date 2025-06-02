// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation
// Updated to support new DC API format and OID4VP-v1 requests

extern "C" {
#include "credentialmanager.h"
#include "cJSON.h"
}

#include "CredentialDatabase.h"
#include "Request.h"

bool MATCH_ALL_CREDENTIALS = false;

extern "C" int main() {
    CallingAppInfo* appInfo = (CallingAppInfo*) malloc(sizeof(CallingAppInfo));
    ::GetCallingAppInfo(appInfo);

    uint32_t credsBlobSize;
    ::GetCredentialsSize(&credsBlobSize);
    uint8_t* credsBlob = (uint8_t*) malloc(credsBlobSize);
    ::ReadCredentialsBuffer((void*) credsBlob, 0, credsBlobSize);
    CredentialDatabase* db = new CredentialDatabase(credsBlob, credsBlobSize);

    if (MATCH_ALL_CREDENTIALS) {
        for (auto& credential : db->credentials) {
            credential.addCredentialToPickerWithoutRequest();
        }
    }

    uint32_t requestSize;
    ::GetRequestSize(&requestSize);
    char* requestBlob = (char*) malloc(requestSize);
    ::GetRequestBuffer(requestBlob);
    cJSON* requestJson = cJSON_Parse(requestBlob);
    cJSON *providers = cJSON_GetObjectItemCaseSensitive(requestJson, "providers");
    if (providers == NULL) {
        providers = cJSON_GetObjectItemCaseSensitive(requestJson, "requests");
    }

    if (cJSON_IsArray(providers)) {
        int numProviders = cJSON_GetArraySize(providers);
        for (int n = 0; n < numProviders; n++) {
            cJSON *provider = cJSON_GetArrayItem(providers, n);
            if (!cJSON_IsObject(provider)) {
                continue;
            }
            cJSON *protocol = cJSON_GetObjectItem(provider, "protocol");
            std::string protocolValue = std::string(cJSON_GetStringValue(protocol));
            cJSON *protocolRequest = cJSON_GetObjectItem(provider, "request");

            cJSON* protocolRequestJson;
            if (protocolRequest == NULL) {
                protocolRequest = cJSON_GetObjectItem(provider, "data");
                if (cJSON_IsString(protocolRequest)) {
                    char* protocolRequestStr = cJSON_GetStringValue(protocolRequest);
                    protocolRequestJson = cJSON_Parse(protocolRequestStr);
                } else {
                    protocolRequestJson = protocolRequest;
                }
            } else {
                const char* protocolRequestValue = cJSON_GetStringValue(protocolRequest);
                protocolRequestJson = cJSON_Parse(protocolRequestValue);
            }


            std::unique_ptr<Request> request;
            if (protocolValue == "preview") {
                // The OG "preview" protocol.
                //
                request = std::move(Request::parsePreview(protocolRequestJson));
            } else if (protocolValue.rfind("openid4vp", 0) == 0) {
                // 18013-7 Annex D
                //
                request = std::move(Request::parseOpenID4VP(protocolRequestJson));
            } else if (protocolValue == "org.iso.mdoc") {
                // 18013-7 Annex C
                //
                request = std::move(Request::parseMdocApi(protocolRequestJson));
            } else if (protocolValue == "austroads-request-forwarding-v2") {
                // From a matcher point of view, ARFv2 is structurally equivalent to mdoc-api
                //
                request = std::move(Request::parseMdocApi(protocolRequestJson));
            }

            if (request) {
                for (auto& credential : db->credentials) {
                    if (credential.matchesRequest(*request)) {
                        credential.addCredentialToPicker(*request);
                    }
                }
            }
        }
    }
}
