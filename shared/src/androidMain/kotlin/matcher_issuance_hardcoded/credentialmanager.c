/*
 * Source: Derived from CMWallet[https://github.com/digitalcredentialsdev/CMWallet] and adapted for this project.
 */

#include "credentialmanager.h"

void* GetRequest() {
	uint32_t size;
	GetRequestSize(&size);
	void* buffer = malloc(size);
	GetRequestBuffer(buffer);
	return buffer;
}

void* GetCredentials() {
	uint32_t size;
	GetCredentialsSize(&size);
	void* buffer = malloc(size);
	ReadCredentialsBuffer(buffer, 0, size);
	return buffer;
}
