# NEXUS-COMPLY — Cyber Layer Master Task Checklist
**Problem Statement ID:** 26155 — AI-Driven Multi-Vendor Network Security Compliance Auditor  
**Backend:** Spring Boot + Spring Data MongoDB  
**Module:** Package B (Cyber Layer & Intelligence Engine)  
**Status:** In Progress  
**Audit Log of Completed Tasks:** [COMPLETED_TASKS.md](file:///d:/auditor/COMPLETED_TASKS.md)  

---

## 0. Completed Milestones (Verified & Tested)
- [x] **0.1 Vendor Fingerprint Detection Service**
  - Regex clue-scoring engine for Cisco, Juniper, Fortinet, and Palo Alto.
  - Confidence scoring formula: $0.50 + (\text{matches} \times 0.10)$, capped at $0.99$.
  - Strict states: `DETECTED` ($\ge 0.70$), `UNCERTAIN` ($< 0.70$), and `UNKNOWN` ($0$ matches).
  - Fixed interface name regex to support unspaced numbers (e.g., `GigabitEthernet0/1`).
  - REST endpoint: `POST /api/v1/cyber/detect-vendor`.
- [x] **0.2 Canonical Security Model Schema (`schemaVersion: "1.0"`)**
  - Universal vendor-neutral representation for network security facts:
    - `security.ssh`: `{ enabled: boolean, version: int }`
    - `security.telnet`: `{ enabled: boolean }`
    - `security.https`: `{ enabled: boolean }`
    - `security.snmp`: `{ enabled: boolean, version: string }`
    - `authentication.aaa`: `boolean`
    - `logging.syslog`: `boolean`
    - `logging.localLogging`: `boolean`
    - `ntp.configured`: `boolean`
    - Flexible maps: `managementAccess`, `acl`, `crypto`, `services`.
  - Line-level auditability via `SourceMapEntry` (`canonicalField`, `sourceLine`, `rawText`).
- [x] **0.3 Vendor Parser Interface & Dynamic Router**
  - Standardized `VendorParser` interface (`parse(String rawConfig)` and `supports(vendor, platform)`).
  - `ParserService` & `ParserServiceImpl` dynamic routing.
  - Safe error handling: empty, null, or unsupported inputs fail safely without throwing exceptions.
- [x] **0.4 Cisco IOS/IOS-XE Parser (`CiscoIosParser`)**
  - 10 core extraction rules (SSHv1/v2/disabled, Telnet transport, HTTPS, SNMPv3, AAA, Syslog, Local logging, NTP).
  - Line-level source tracking and unknown command capture in `unknowns`.
- [x] **0.5 Normalized Configuration Persistence in MongoDB**
  - Entity `NormalizedConfigurationDocument` matching `schema1.md` Section 5 (`normalized_configurations`).
  - Spring Data Mongo repository and `NormalizationService`.
  - Tested with real in-memory MongoDB daemon roundtrip.

---

## 1. Multi-Framework Compliance Rule Engine
*(cyberlayer.pdf Section 33, Steps 6, 7, 8)*
- [x] **1.1 Compliance Rule Engine Core & Domain Abstraction**
  - Define `ComplianceRule`, `RuleRequirement`, `RuleEvaluator`, and evaluation models.
  - Output status evaluation: `PASS`, `FAIL`, `UNKNOWN`, `NOT_APPLICABLE`, `ERROR`.
  - Enforce rule safety: Missing evidence returns `UNKNOWN` or `NOT_ASSESSABLE`, never a false `PASS`. Execution errors return `ERROR`.
- [x] **1.2 CIS Network Benchmark Rule Set**
  - Concrete rule implementations for CIS Cisco IOS XE 17.x Benchmark v2.2.1:
    - CIS 1.1.1 (AAA enabled)
    - CIS 1.2.2 (Telnet disabled on VTY)
    - CIS 2.1.1.2 (SSH version 2)
    - CIS 1.5.9 (SNMPv3 privacy required - snmp.version == "3")
    - CIS 2.2.1 / 2.2.4 (Logging enabled with remote syslog host)
    - CIS 2.2.2 / 2.2.3 (Local logging configured buffered/console)
    - CIS 2.3.2 (Authoritative NTP server configured)
  - Idempotent seed component `CisCiscoIosXeRuleSeeder.java` upserting 1 Framework, 7 Controls, and 7 ComplianceRules.
  - Documented known gap: HTTPS rule excluded per CIS benchmark posture.
  - Documented known limitation: SNMPv3 `priv` keyword verification not yet modeled in canonical.
  - Severity explicitly labeled as internal engineering judgment, not CIS-assigned.
  - Integration verified via `CisCiscoIosXeRuleSeederIntegrationTest` (3/3 tests passing, 37 total tests passing).
- [x] **1.3 Cross-Framework Technical Mappings (NIST SP 800-53 Rev 5 & ISO/IEC 27001:2013)**
  - **NIST SP 800-53 (Rev 5)**: Idempotently seeded via `NistSp80053RuleSeeder.java` (7 Controls, 7 ComplianceRules):
    - IA-2 (AAA enabled), AC-17 (Telnet disabled), SC-8 (SSHv2 enforced), CM-6 (SNMPv3 required), AU-2 (Syslog host), AU-12 (Local logging), AU-8 (NTP configured).
    - Baselines (LOW, MODERATE, HIGH) stored separately from severity.
  - **ISO/IEC 27001:2013 Annex A**: Idempotently seeded via `Iso27001RuleSeeder.java` (6 Controls, 7 ComplianceRules):
    - Controls: A.9.4.2 (Secure log-on), A.13.1.1 (Network controls), A.10.1.1 (Cryptographic controls), A.12.4.1 (Event logging), A.12.4.3 (Admin/operator logs), A.12.4.4 (Clock sync).
    - Rules: ISO-A.9.4.2, ISO-A.13.1.1-TELNET, ISO-A.10.1.1, ISO-A.13.1.1-SNMP, ISO-A.12.4.1, ISO-A.12.4.3, ISO-A.12.4.4 (with TELNET and SNMP rules referencing the same A.13.1.1 ControlDocument _id).
    - Evaluator result label map stored in static framework metadata (PASS -> "Technical evidence available", FAIL -> "Technical evidence missing", etc.).
    - Public Annex A numbering used with original descriptions; no copyrighted ISO text reproduced.
  - Reused exact same `RuleRequirement` definitions across CIS, NIST, and ISO rules.
  - Verified via `NistAndIsoCrossFrameworkRuleSeederIntegrationTest` (3/3 tests passing, 40 total tests passing).
  - *Note: DISA STIG mapping is explicitly scoped for a subsequent task.*
- [x] **1.4 Framework Persistence Layer**
  - MongoDB entities & repositories for `frameworks`, `controls`, and `compliance_rules` matching `schema1.md` Sections 6, 7, and 8.

---

## 2. Findings, Line-Level Evidence & Audit Orchestration
*(cyberlayer.pdf Section 33, Steps 9, 10)*
- [x] **2.1 Findings Engine**
  - Convert failed compliance checks (`FAIL`) into actionable finding records with initial status `OPEN`.
  - Lifecycle state constants / enum: `OPEN`, `ACKNOWLEDGED`, `IN_REVIEW`, `REMEDIATION_PLANNED`, `RESOLVED`, `FALSE_POSITIVE` (`schema1.md` Section 4.1).
  - Exact schema match to `schema1.md` Section 10 (`findings` collection).
  - Traceability retained for `ruleId`, `controlId`, `frameworkIds`, `deviceId`, `configurationId`, `auditId`.
  - Inherent severity preserved directly from rule evaluation without recalculation.
  - Zero findings created on `PASS`, `UNKNOWN`, `NOT_APPLICABLE`, or `ERROR`.
  - Verified via `FindingCreationServiceUnitTest` and `FindingCreationIntegrationTest` (49/49 total tests passing).
- [ ] **2.2 Line-Level Evidence Engine**
  - Connect each finding to its exact source line, context lines, actual value, expected value, and rule reference.
  - Persisting to `evidence` collection (`schema1.md` Section 11).
- [ ] **2.3 Audit Orchestration Service**
  - Full audit lifecycle state machine:
    `QUEUED` -> `DETECTING` -> `PARSING` -> `NORMALIZING` -> `UNKNOWN_REVIEW` -> `CHECKING` -> `RISK_CALCULATION` -> `COMPLETED` / `FAILED` / `CANCELLED`.
  - `UNKNOWN_REVIEW` is where the AI-suggestion/human-approval loop plugs in.
  - `CANCELLED` is the terminal state supporting API endpoint B-021 (`POST /api/v1/audits/{id}/cancel`).
  - Persisting to `audits` collection (`schema1.md` Section 9).

---

## 3. Contextual Risk Scoring Engine
*(cyberlayer.pdf Section 33, Step 11 & Section 18)*
- [ ] **3.1 Contextual Risk Calculation Service**
  - Strictly separate Finding Severity (inherent rule severity) from Contextual Risk (operational impact).
  - Explainable scoring formula incorporating 5 distinct factors per cyberlayer.pdf Section 18:
    $$\text{Risk Score} = f(\text{Finding Severity}, \text{Asset Criticality}, \text{Network Exposure}, \text{Control Importance}, \text{Confidence/Uncertainty})$$
    - **Finding Severity**: CRITICAL=10, HIGH=8, MEDIUM=5, LOW=2 (from rule/finding)
    - **Asset Criticality**: PRODUCTION/HIGH=1.5, MEDIUM=1.0, LOW=0.7 (from device inventory)
    - **Network Exposure**: INTERNET_FACING=1.5, INTERNAL=1.0 (from device context)
    - **Control Importance**: High-impact controls vs baseline controls (from framework)
    - **Confidence / Uncertainty**: Parser detection confidence & AI evidence uncertainty factor
  - Store complete calculation factor breakdown in the database for transparent explainability.
  - Persisting to `risk_assessments` collection (`schema1.md` Section 12).

---

## 4. Remaining Three Vendor Parsers (Four-Vendor Parity)
*(cyberlayer.pdf Section 33, Step 12)*
- [ ] **4.1 Juniper Junos Parser (`JuniperJunosParser`)**
  - Implement `VendorParser` for Juniper Junos `set`-style CLI syntax:
    - `set system services ssh protocol-version v2` -> `security.ssh.version = 2`, `enabled = true`
    - `set system services telnet` -> `security.telnet.enabled = true`
    - `set system services web-management https` -> `security.https.enabled = true`
    - `set snmp v3 ...` -> `security.snmp.enabled = true`, `version = "3"`
    - `set system syslog host ...` -> `logging.syslog = true`
    - `set system ntp server ...` -> `ntp.configured = true`
  - Line-level `SourceMapEntry` tracking and unknown line routing.
  - Unit tests with real Juniper Junos configuration scenario.
- [ ] **4.2 Fortinet FortiOS Parser (`FortinetFortiOSParser`)**
  - Implement `VendorParser` for Fortinet hierarchical block syntax:
    - `config system admin` / `set admin-ssh-port` / `set ssh-v1 disable` -> `security.ssh`
    - `set admin-telnet disable` / `enable` -> `security.telnet.enabled`
    - `set admin-https-redirect enable` / `set admin-sport` -> `security.https.enabled`
    - `config system snmp sysinfo` / `config system snmp user` (v3) -> `security.snmp`
    - `config log syslogd setting` -> `logging.syslog = true`
    - `config system ntp` -> `ntp.configured = true`
  - Unit tests with real FortiGate configuration.
- [ ] **4.3 Palo Alto PAN-OS Parser (`PaloAltoPanOsParser`)**
  - Implement `VendorParser` for Palo Alto set/hierarchical management configuration:
    - Management profile / service settings for SSHv2, HTTPS, Telnet disablement, SNMPv3, Syslog server profiles, NTP.
  - Unit tests with real PAN-OS configuration.

---

## 5. Validated Remediation Engine
*(cyberlayer.pdf Section 33, Step 13)*
- [ ] **5.1 Curated Platform Hardening CLI Templates**
  - Pre-validated, deterministic CLI remediation commands tailored per vendor/platform (Cisco, Juniper, Fortinet, Palo Alto).
  - Step-by-step ordered command sequences with expected post-remediation validation state.
  - Absolute rule: No autonomous execution on production infrastructure.
  - Persisting to `remediation_templates` and `remediation_plans` (`schema1.md` Sections 15 & 16).

---

## 6. Configuration Drift Intelligence
*(cyberlayer.pdf Section 33, Step 14)*
- [ ] **6.1 Semantic Drift Detection Engine**
  - Compare canonical security state between Version $N$ and Version $N+1$.
  - Filter out cosmetic whitespace/formatting changes; isolate genuine security facts added, modified, or removed.
  - Calculate security posture delta (improved, degraded, unchanged) and affected controls.
  - Persisting to `drift_events` collection (`schema1.md` Section 13).

---

## 7. AI Unknown-Syntax Training Loop (Safe Boundary)
*(cyberlayer.pdf Section 33, Step 15)*
- [ ] **7.1 AI Unknown-Syntax Suggestion Service**
  - Ingest unrecognized constructs from `unknowns`.
  - Request semantic suggestions (canonical field, recommended value, confidence).
- [ ] **7.2 Human-in-the-Loop Review & Learning Loop**
  - Workflow states: `REVIEW_REQUIRED`, `APPROVED`, `REJECTED`.
  - Human approval boundary: AI suggestions must be validated by an administrator before becoming reusable.
  - Store approved mappings in `ai_mappings` collection (`schema1.md` Section 17).
  - Dynamic runtime reuse: Parsers apply approved mappings to subsequent configuration runs without requiring backend code redeployment.

---

## 8. What-If Simulation Engine
*(cyberlayer.pdf Section 33, Step 16)*
- [ ] **8.1 Non-Destructive What-If Simulator**
  - Evaluate hypothetical configuration modifications against an in-memory clone of canonical state.
  - Absolute safety boundary: Never mutates actual configuration history or database audit logs.
  - Output predicted compliance score, risk delta, and affected controls before physical CLI deployment.
  - Persisting to `what_if_simulations` collection (`schema1.md` Section 14).

---

## 9. Actionable Reporting & PDF Export
*(cyberlayer.pdf Section 33, Step 18)*
- [ ] **9.1 Comprehensive Compliance Report Generator**
  - Aggregate device metadata, executive compliance summary, framework scores (CIS, NIST, STIG, ISO), line-level evidence findings, risk scores, and platform CLI remediation scripts.
  - Generate standalone, professional PDF compliance reports for auditors and security teams.
  - Persisting to `reports` collection (`schema1.md` Section 19).

---

## Execution Protocol
1. Tasks are developed **one at a time**.
2. After completing each task:
   - Full test suite is executed.
   - User is notified with raw execution results, code diffs, and verification proof.
   - User reviews and confirms before advancing to the next item.
