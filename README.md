<div align="center">

<picture>
  <img alt="Logo" src="assets/gh.png">
</picture>
&nbsp;&nbsp;&nbsp;&nbsp;
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/valera-w.png">
  <source media="(prefers-color-scheme: light)" srcset="assets/valera-b.png">
  <img alt="Valera" src="assets/Valera-b.png">
</picture>


# Valera – VC-K-powered CMP Identity Wallet App for iOS and Android
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-brightgreen.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![A-SIT Plus Official](https://img.shields.io/badge/A--SIT_Plus-official-005b79?logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNDMuNzYyODYgMTg0LjgxOTk5Ij48ZGVmcz48Y2xpcFBhdGggaWQ9ImEiIGNsaXBQYXRoVW5pdHM9InVzZXJTcGFjZU9uVXNlIj48cGF0aCBkPSJNMCA1OTUuMjhoODQxLjg5VjBIMFoiLz48L2NsaXBQYXRoPjwvZGVmcz48ZyBjbGlwLXBhdGg9InVybCgjYSkiIHRyYW5zZm9ybT0ibWF0cml4KDEuMzMzMzMzMyAwIDAgLTEuMzMzMzMzMyAtNDgyLjI1IDUxNy41MykiPjxwYXRoIGZpbGw9IiMwMDViNzkiIGQ9Ik00MTUuNjcgMjQ5LjUzYy03LjE1LjA4LTEzLjk0IDEtMjAuMTcgMi43NWE1Mi4zMyA1Mi4zMyAwIDAgMC0xNy40OCA4LjQ2IDQwLjQzIDQwLjQzIDAgMCAwLTExLjk2IDE0LjU2Yy0yLjY4IDUuNDEtNC4xNCAxMS44NC00LjM1IDE5LjA5bC0uMDIgNi4xMnYyLjE3YS43MS43MSAwIDAgMCAuNy43M2gxNi41MmMuMzkgMCAuNy0uMzIuNzEtLjdsLjAxLTIuMmMwLTIuNi4wMi01LjgyLjAzLTYuMDcuMi00LjYgMS4yNC04LjY2IDMuMDgtMTIuMDZhMjguNTIgMjguNTIgMCAwIDEgOC4yMy05LjU4IDM1LjI1IDM1LjI1IDAgMCAxIDExLjk2LTUuNTggNTUuMzggNTUuMzggMCAwIDEgMTIuNTgtMS43NmM0LjMyLjEgOC42LjcgMTIuNzQgMS44YTM1LjA3IDM1LjA3IDAgMCAxIDExLjk2IDUuNTcgMjguNTQgMjguNTQgMCAwIDEgOC4yNCA5LjU3YzEuOTYgMy42NCAzIDguMDIgMy4xMiAxMy4wMnYyNC4wOUgzNjIuNGEuNy43IDAgMCAwLS43MS43VjMzNWMwIDguNDMuMDEgOC4wNS4wMSA4LjE0LjIgNy4zIDEuNjcgMTMuNzcgNC4zNiAxOS4yMmE0MC40MyA0MC40MyAwIDAgMCAxMS45NiAxNC41N2M1IDMuNzYgMTAuODcgNi42MSAxNy40OCA4LjQ2YTc3LjUgNzcuNSAwIDAgMCAyMC4wMiAyLjc3YzcuMTUtLjA3IDEzLjk0LTEgMjAuMTctMi43NGE1Mi4zIDUyLjMgMCAwIDAgMTcuNDgtOC40NiA0MC40IDQwLjQgMCAwIDAgMTEuOTUtMTQuNTdjMS42Mi0zLjI2IDMuNzctMTAuMDQgMy43Ny0xNC42OCAwLS4zOC0uMTctLjc0LS41NC0uODJsLTE2Ljg5LS40Yy0uMi0uMDQtLjM0LjM0LS4zNC41NCAwIC4yNy0uMDMuNC0uMDYuNi0uNSAyLjgyLTEuMzggNS40LTIuNjEgNy42OWEyOC41MyAyOC41MyAwIDAgMS04LjI0IDkuNTggMzUuMDEgMzUuMDEgMCAwIDEtMTEuOTYgNS41NyA1NS4yNSA1NS4yNSAwIDAgMS0xMi41NyAxLjc3Yy00LjMyLS4xLTguNjEtLjcxLTEyLjc1LTEuOGEzNS4wNSAzNS4wNSAwIDAgMS0xMS45Ni01LjU3IDI4LjUyIDI4LjUyIDAgMCAxLTguMjMtOS41OGMtMS44Ni0zLjQ0LTIuOS03LjU1LTMuMDktMTIuMmwtLjAxLTcuNDdoODkuMTZhLjcuNyAwIDAgMCAuNy0uNzJ2LTM5LjVjLS4xLTcuNjUtMS41OC0xNC40LTQuMzgtMjAuMDZhNDAuNCA0MC40IDAgMCAwLTExLjk1LTE0LjU2IDUyLjM3IDUyLjM3IDAgMCAwLTE3LjQ4LTguNDcgNzcuNTYgNzcuNTYgMCAwIDAtMjAuMDEtMi43N1oiLz48cGF0aCBmaWxsPSIjY2U0OTJlIiBkPSJNNDE5LjM4IDI4MC42M2gtNy41N2EuNy43IDAgMCAwLS43MS43MXYxNS40MmE4LjE3IDguMTcgMCAwIDAtMy43OCA2LjkgOC4yOCA4LjI4IDAgMCAwIDE2LjU0IDAgOC4yOSA4LjI5IDAgMCAwLTMuNzYtNi45di0xNS40MmEuNy43IDAgMCAwLS43Mi0uNzEiLz48L2c%2BPC9zdmc%2B&logoColor=white&labelColor=white)](https://a-sit-plus.github.io)
[![Powered by VC-K](https://img.shields.io/badge/VC--K-powered-8A2BE2?logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA4LjAzIDkuNSI+PGcgZmlsbD0iIzhhMmJlMiIgZm9udC1mYW1pbHk9IlZBTE9SQU5UIiBmb250LXNpemU9IjEyLjciIHRleHQtYW5jaG9yPSJtaWRkbGUiPjxwYXRoIGQ9Ik01OS42NCAyMjIuMTNxMC0uOTguMzYtMS44Mi4zNy0uODQuOTgtMS40Ni42Mi0uNjIgMS40Ni0uOTYuODMtLjM2IDEuOC0uMzUgMS4wMy4wMiAxLjkuNDIuODcuNCAxLjUgMS4xMi4wNC4wNS4wMy4xMSAwIC4wNy0uMDUuMWwtMSAuODZxLS4wNi4wMy0uMTIuMDN0LS4xLS4wNnEtLjQyLS40OC0xLS43Ni0uNTYtLjMtMS4yMi0uMjgtLjYuMDEtMS4xMy4yNy0uNTQuMjQtLjkzLjY3LS40LjQyLS42Mi45OC0uMjMuNTYtLjIzIDEuMiAwIC42My4yNCAxLjE4LjI0LjU2LjY1Ljk4LjQuNDIuOTQuNjYuNTMuMjMgMS4xNC4yMy42My0uMDEgMS4yLS4zLjU1LS4yNy45Ni0uNzUuMDQtLjA1LjEtLjA1LjA2LS4wMi4xMS4wM2wxIC44NnEuMDYuMDMuMDYuMS4wMS4wNi0uMDMuMTEtLjY0LjczLTEuNTMgMS4xNC0uOS40MS0xLjk1LjQtLjk1IDAtMS43OS0uMzYtLjgyLS4zNy0xLjQzLS45OS0uNjEtLjYzLS45NS0xLjQ4LS4zNS0uODUtLjM1LTEuODN6IiBzdHlsZT0iLWlua3NjYXBlLWZvbnQtc3BlY2lmaWNhdGlvbjpWQUxPUkFOVDt0ZXh0LWFsaWduOmNlbnRlciIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoLTU5LjY0IC0yMTcuNDIpIi8+PHBhdGggZD0iTTY2LjIxIDIyMS4zNWgxLjNjLjEgMCAuMTYuMDYuMTYuMTd2MS4zOGMwIC4xMS0uMDUuMTctLjE2LjE3aC0xLjNjLS4xIDAtLjE2LS4wNi0uMTYtLjE3di0xLjM4YzAtLjExLjA1LS4xNy4xNi0uMTd6IiBsZXR0ZXItc3BhY2luZz0iLTMuMTIiIHN0eWxlPSItaW5rc2NhcGUtZm9udC1zcGVjaWZpY2F0aW9uOlZBTE9SQU5UO3RleHQtYWxpZ246Y2VudGVyIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtNTkuNjQgLTIxNy40MikiLz48L2c+PC9zdmc+&logoColor=white&labelColor=white)](https://github.com/a-sit-plus/vck)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform--mobile-orange.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)


</div>

**Valera** is a single-codebase [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) **Identity Wallet for iOS and Android**.
It lets you load, store and present [W3C Verifiable Credentials](https://www.w3.org/TR/vc-data-model/) — your PID, driving licence, health insurance card and more — straight from your phone, powered end-to-end by [VC-K](https://github.com/a-sit-plus/vck).

It is the holder side of a complete, self-contained EUDIW playground:

> 🪪 **[Issuer](https://wallet-issuer.a-sit.plus/)** → 📲 **Valera (this app)** → ✅ **[Relying Party](https://wallet-rp.a-sit.plus/)**

Provision credentials from the demo issuer, keep them in your wallet, and present them to the demo verifier — all speaking the same emerging eIDAS 2 / EUDIW protocols.

| ⛔️ Not for production — read before you tap                                                                              |
|:------------------------------------------------------------------------------------------------------------------------|
| Valera is a **Technology Demonstrator and testbed**. Do **not** load real identity data into it. See [Limitations](#limitations--not-for-production). |

<div align="center">

<img alt="screenshot" src="https://github.com/user-attachments/assets/751de375-652f-4270-8cfc-00db761c26a9" width="30%" height="30%">

</div>

Valera tracks the emerging eIDAS 2 technical specification — a regulation that is still very much a moving target when it comes to technical details. Even so, it already interoperates with the EU reference issuing service and EU verifier, and lets you experience first-hand what an [EU Digital Identity Wallet](https://github.com/eu-digital-identity-wallet) (EUDIW) can look and feel like.

## ✨ What's new in 5.8.0

The headline release — built on a major jump to **[VC-K 6.0.0](https://github.com/a-sit-plus/vck)**:

* 🌐 **Digital Credentials API, leveled up** — the OS-native browser handover now negotiates **requests carrying multiple protocols** at once, and **iOS joins the party** with native [ISO/IEC 18013-7 Annex C](https://www.iso.org/standard/82772.html) support.
* 📥 **Issuance over the DC API** — get credentials issued directly through the browser's Digital Credentials API (preliminary [OpenID4VCI #476](https://github.com/openid/OpenID4VCI/pull/476) spec), no redirect dance required.
* 🔁 **Smarter credential refresh** — refresh prompts now show **which credential** they're about, and you can **mute the prompt for a single credential** instead of all-or-nothing.
* 🔐 **Hardware-backed key attestation** updated to the VC-K 6.0 / TS3 **WUA 1.5** APIs.
* 🍎 **iOS 18.6 baseline** — the minimum target moves from 16.0 to 18.6 (iOS 18 is the oldest version still receiving security updates).
* 🤝 Verified to interop with the demo [issuer](https://wallet-issuer.a-sit.plus/) and [relying party](https://wallet-rp.a-sit.plus/).

## Features

Valera fetches credentials from [wallet-issuer.a-sit.plus](https://wallet-issuer.a-sit.plus/) in a range of formats — even beyond what the upcoming EUDIW targets — and presents them to verifiers over multiple transports.

**Get credentials (issuance)**
* 📜 **OpenID4VCI** issuance (authorization code & pre-authorized code flows)
* 🌐 **Browser-native issuance** via the **Digital Credentials API**
* 🔄 **Refresh** credentials when they expire, with per-credential prompts
* 🛡️ **Hardware-backed holder keys**, biometric/passcode-gated, with WUA key attestation

**Show credentials (presentation)**
* 🪪 **OpenID4VP** with both **SD-JWT VC** and **ISO mDoc** presentation
* 🧮 **DCQL** and ISO **DeviceRequest** queries — verifiers specify the credentials and data elements to request
* 📡 **Proximity presentation** to a verifier over **Bluetooth Low Energy** and **NFC** ([ISO/IEC 18013-5](https://www.iso.org/standard/69084.html))
* 🌐 **Digital Credentials API** presentation, including **ISO 18013-7 Annex C** (now on iOS too)
* 🔎 Built-in **technical detail view** to inspect a credential's claims, validity and status — great for debugging interop
* ✅ **Freshness & status** indicators backed by token status lists

**Supported credentials**

| Credential | SD-JWT VC | ISO mDoc |
|---|:---:|:---:|
| Person Identification Data (PID) | ✅ | ✅ |
| Mobile Driving Licence (mDL) | ✅ | ✅ |
| European Health Insurance Card (EHIC) | ✅ | |
| Age Verification | ✅ | ✅ |
| Power of Representation (PoR) | ✅ | |
| Certificate of Residence (CoR) | ✅ | |
| Tax ID | ✅ | |
| e-Prescription | ✅ | |

Unknown schemes degrade gracefully via a fallback scheme, so even credentials Valera doesn't natively model can still be loaded and inspected. For full schema details, head over to the [credentials collection repo](https://github.com/a-sit-plus/credential-collection).

## The ecosystem

Valera is developed in lockstep with two companion reference services, so the whole issue → hold → present loop stays interoperable:

| | What it is | Try it |
|---|---|---|
| 🪪 **Issuing Backend** | A Spring Boot **OpenID4VCI issuer** (also issuance over the DC API) that mints wallet-ready PID, mDL, EHIC, Age Verification, PoR, CoR and Tax ID credentials as JWT VC, SD-JWT VC and ISO mDoc. | [wallet-issuer.a-sit.plus](https://wallet-issuer.a-sit.plus/) · [repo](https://github.com/a-sit-plus/wallet-issuing-backend) |
| ✅ **Relying Party** | A Spring Boot **OpenID4VP verifier** that requests and validates presentations over QR, deep link and the DC API, with configurable DCQL and ISO DeviceRequest queries. | [wallet-rp.a-sit.plus](https://wallet-rp.a-sit.plus/) · [repo](https://github.com/a-sit-plus/wallet-relying-party) |

The demo relying party in particular lets you freely define **how and which details** of a credential are requested — the best way to explore verifiable presentation in all its (technical) glory.

## Limitations — not for production

Valera is a **testbed and technology demonstrator**, not a shippable wallet. Concretely:

* ⚠️ **Never load real identity data.** Treat everything in the app as throwaway test data.
* 🧪 **The demo issuer issues synthetic credentials** — many claim values are random or placeholder, and the demo services use **ephemeral, self-signed keys** that change on restart. Nothing here is a trust anchor.
* 🎯 **The specs are a moving target.** eIDAS 2 / EUDIW, OpenID4VCI, OpenID4VP, HAIP and the DC API are still evolving; interop can and will break between drafts. Valera follows the latest drafts, not a frozen, certified profile.
* 🚧 **No production hardening.** It is not security-audited, not certified, and makes no guarantees around privacy, key management or data protection for real-world use.
* 📦 **Limited scope.** Only the credentials and transports listed above are supported.

It is, however, an excellent sandbox — actively used in teaching at TU Graz's [Institute of Information Security](https://www.isec.tugraz.at/).

## Design Principles
Valera is designed with distinctly different goals in mind than the EU reference implementation.
Most prominently, we follow a KMP-first approach. In a nutshell, this means that given the choice between
using two platform-native libraries and glue code to get a job done and investing the blood, sweat and tears it takes to
conceive a proper KMP solution, we go the distance and invest in the KMP solution.

In the end, this (at times) tiresome approach brings a couple of advantages to the table:
* Consistency across platforms
* Shared UI tests
* Common, consistent, and thoroughly tested [VC-K-powered](https://github.com/a-sit-plus/vck) business logic across
  * Issuer
  * Verifier
  * iOS App
  * Android App
* As an immediate consequence: far less margin for mistakes
* Unified cryptographic functionality integrated with platform-native biometric and passcode-base authentication based on
[Signum](https://github.com/a-sit-plus/signum).

This much tighter integration of all moving parts across all parts of back-end and front-end opens up makes experimenting
with new features much easier compared to having multiple discrete codebases.
As a consequence, introducing new credentials (and testing issuing, and presenting them) must only be done once.
It is even possible to introduce new cryptographic algorithms or alter any part of certain workflows and propagate such changes
with very little friction across back-end and front-end.
As such, Valera, VC-K and Signum make for an ideal sandbox&nbsp;&mdash; a property actively used in teaching at TU Graz's
[Institute of Information Security](https://www.isec.tugraz.at/).

Valera, the issuing service and the demo service provider are under active development and are updated
in tandem with each other.
Outside contributions are welcome (see [CONTRIBUTING.MD](CONTRIBUTING.md))!

## Development

To set up this project locally see [DEVELOPMENT.md](DEVELOPMENT.md).

---

| ![eu.svg](assets/eu.svg) <br> Co&#8209;Funded&nbsp;by&nbsp;the<br>European&nbsp;Union |   This project has received funding from the European Union’s <a href="https://digital-strategy.ec.europa.eu/en/activities/digital-programme">Digital Europe Programme (DIGITAL)</a>, Project 101102655 — POTENTIAL.   |
|:-------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

---

The Apache License does not apply to the logos, (including the A-SIT logo) and the project/module name(s), as these are the sole property of A-SIT/A-SIT Plus GmbH and may not be used in derivative works without explicit permission! 
