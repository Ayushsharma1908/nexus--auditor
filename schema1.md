# NEXUS-COMPLY — MongoDB Schema Specification
## Package B: Processing, Compliance & Intelligence

**Problem:** SIH26155 — AI-Driven Multi-Vendor Network Security Compliance Auditor  
**Backend:** Spring Boot + Spring Data MongoDB  
**Frontend reference:** `nexus-frontend.zip`  
**Purpose:** Database contract for Package B APIs and Antigravity implementation.

---

# 1. Source of this schema

This schema is derived from:

1. The Package B API specification.
2. The complete API functionality specification.
3. The Package B Antigravity Master Guide.
4. The current NEXUS-COMPLY frontend types, mock data, and pages in `nexus-frontend.zip`.
5. The existing project architecture where MongoDB is the current database direction.

The frontend currently exposes models for:

- Device
- Configuration
- Audit
- Finding
- Framework
- DriftEvent
- AiMapping
- Report
- DashboardData

The backend database must support those UI concepts while also supporting the full Package B API surface.

**Important:** The frontend mock values are demo data, not production database values. IDs, scores, dates, names, and example configuration contents must not be treated as fixed database records.

---

# 2. Database architecture

Use one MongoDB database for the NEXUS-COMPLY backend.

```text
React
  ↓
Spring Boot REST API
  ↓
Services
  ↓
Spring Data MongoDB
  ↓
MongoDB
```

React must never connect directly to MongoDB.

Package B owns these collections:

```text
normalized_configurations
frameworks
controls
compliance_rules
audits
findings
evidence
risk_assessments
drift_events
what_if_simulations
remediation_templates
remediation_plans
ai_mappings
ai_jobs
reports
```

Package A owns or provides access to:

```text
users
roles
devices
configurations
configuration_versions
vendor_detections
audit_logs
```

Package B must reference Package A IDs instead of creating duplicate Device, User, or Configuration models.

---

# 3. Global MongoDB conventions

## 3.1 ID strategy

Use application-generated UUID strings for domain IDs.

Example:

```json
{
  "_id": "8c9f6d8b-0c71-4c5d-9e8c-8d0d4b0e5f21"
}
```

Do not mix UUID strings and Mongo ObjectId for domain IDs.

If Spring Data requires a Mongo `_id`, the UUID string can be the `_id`.

---

## 3.2 Timestamps

All persistent entities that have lifecycle information should use:

```json
{
  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:10:00Z"
}
```

Use UTC and ISO-8601 timestamps.

Java representation:

```java
Instant
```

Do not store formatted UI strings such as:

```text
"Sep 23, 2026 09:40"
```

The frontend should format timestamps.

---

## 3.3 Common audit metadata

Where applicable:

```json
{
  "createdBy": "user-uuid",
  "updatedBy": "user-uuid",
  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:10:00Z"
}
```

Do not assume every collection needs every field. Use only fields relevant to the resource.

---

# 4. Enum conventions

Use stable uppercase machine values in MongoDB.

## Finding status

```text
OPEN
ACKNOWLEDGED
IN_REVIEW
REMEDIATION_PLANNED
RESOLVED
FALSE_POSITIVE
```

## Audit status

```text
QUEUED
DETECTING
PARSING
NORMALIZING
UNKNOWN_REVIEW
CHECKING
RISK_CALCULATION
COMPLETED
FAILED
CANCELLED
```

The frontend may translate these into human-readable labels.

## Compliance status

```text
PASS
FAIL
UNKNOWN
NOT_APPLICABLE
ERROR
```

## Severity

```text
CRITICAL
HIGH
MEDIUM
LOW
```

## Job status

```text
QUEUED
RUNNING
COMPLETED
FAILED
CANCELLED
```

## Report status

```text
PROCESSING
READY
ARCHIVED
FAILED
```

---

# 5. Collection: normalized_configurations

## Purpose

Stores the canonical, vendor-neutral representation produced from a configuration version.

This is the primary Package B input for compliance evaluation.

## Example

```json
{
  "_id": "norm-uuid",
  "deviceId": "device-uuid",
  "configurationId": "configuration-uuid",
  "versionId": "version-uuid",

  "vendor": "Cisco",
  "platform": "IOS-XE",
  "osVersion": "17.9.4a",

  "schemaVersion": "1.0",

  "canonical": {
    "ssh": {
      "version": 2,
      "authenticationRetries": 3,
      "timeoutSeconds": 60
    },
    "management": {
      "telnetEnabled": true
    },
    "logging": {
      "enabled": true
    }
  },

  "sourceMap": [
    {
      "canonicalField": "ssh.version",
      "sourceLine": 17,
      "rawText": "ip ssh version 2"
    }
  ],

  "unknowns": [],

  "normalizationStatus": "COMPLETED",

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Important rule

`canonical` must remain flexible because vendor configurations are heterogeneous.

Do not create hundreds of rigid top-level Mongo fields for every possible network attribute.

## Indexes

```text
deviceId
configurationId
versionId
createdAt
(deviceId, createdAt)
```

---

# 6. Collection: frameworks

## Purpose

Stores compliance frameworks such as:

```text
CIS
NIST
STIG
ISO/IEC 27001
```

## Schema

```json
{
  "_id": "framework-uuid",
  "code": "CIS",
  "name": "CIS Benchmarks",
  "version": "2025",
  "category": "NETWORK_SECURITY",
  "description": "Framework description",
  "status": "ACTIVE",
  "controlCount": 120,
  "metadata": {},
  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Indexes

```text
code
(code, version)
status
```

---

# 7. Collection: controls

## Purpose

Stores individual controls belonging to frameworks.

## Schema

```json
{
  "_id": "control-uuid",
  "frameworkId": "framework-uuid",
  "controlId": "CIS-5.1",
  "title": "Disable insecure management protocols",
  "description": "Control description",
  "category": "ACCESS_CONTROL",
  "severity": "HIGH",
  "status": "ACTIVE",

  "requirements": [
    {
      "field": "management.telnetEnabled",
      "operator": "EQUALS",
      "expectedValue": false
    }
  ],

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Indexes

```text
frameworkId
controlId
(frameworkId, controlId)
status
```

---

# 8. Collection: compliance_rules

## Purpose

Stores executable compliance-rule definitions or rule references.

The actual cybersecurity evaluation algorithm belongs to the Cyber Engine.

Package B stores and exposes the rule resource and calls the engine.

## Schema

```json
{
  "_id": "rule-uuid",
  "ruleCode": "CIS-SSH-V2",
  "controlId": "control-uuid",

  "name": "SSH version must be 2",
  "description": "SSH must use version 2.",

  "expression": {
    "field": "ssh.version",
    "operator": "EQUALS",
    "value": 2
  },

  "severity": "HIGH",

  "frameworkIds": [
    "framework-cis-uuid",
    "framework-nist-uuid"
  ],

  "applicableVendors": [
    "Cisco",
    "Juniper",
    "Fortinet"
  ],

  "status": "ACTIVE",

  "version": 1,

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Indexes

```text
ruleCode
controlId
frameworkIds
status
```

---

# 9. Collection: audits

## Purpose

Represents a compliance audit execution.

## Schema

```json
{
  "_id": "audit-uuid",

  "deviceId": "device-uuid",
  "configurationId": "configuration-uuid",
  "versionId": "version-uuid",
  "normalizedConfigurationId": "norm-uuid",

  "frameworkIds": [
    "framework-cis-uuid",
    "framework-nist-uuid"
  ],

  "status": "COMPLETED",

  "progress": {
    "stage": "RISK_CALCULATION",
    "percent": 100
  },

  "summary": {
    "totalControls": 50,
    "passed": 42,
    "failed": 6,
    "unknown": 2,
    "notApplicable": 0
  },

  "complianceScore": 84.0,

  "startedAt": "2026-09-24T08:00:00Z",
  "completedAt": "2026-09-24T08:02:00Z",

  "createdBy": "user-uuid",
  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:02:00Z"
}
```

## Indexes

```text
deviceId
configurationId
versionId
status
createdAt
(deviceId, createdAt)
```

---

# 10. Collection: findings

## Purpose

Stores compliance findings generated from audit evaluation.

## Schema

```json
{
  "_id": "finding-uuid",

  "auditId": "audit-uuid",
  "deviceId": "device-uuid",
  "configurationId": "configuration-uuid",

  "controlId": "control-uuid",
  "ruleId": "rule-uuid",

  "controlCode": "CIS-5.1",
  "title": "Telnet enabled",
  "description": "Telnet is enabled for management access.",

  "status": "OPEN",
  "complianceStatus": "FAIL",
  "severity": "HIGH",

  "frameworkIds": [
    "framework-cis-uuid",
    "framework-nist-uuid"
  ],

  "canonicalField": "management.telnetEnabled",

  "expected": false,
  "actual": true,

  "impact": "Insecure remote management protocol is enabled.",

  "evidenceIds": [
    "evidence-uuid"
  ],

  "risk": {
    "score": 68,
    "level": "HIGH"
  },

  "remediationAvailable": true,

  "createdAt": "2026-09-24T08:02:00Z",
  "updatedAt": "2026-09-24T08:02:00Z"
}
```

## Indexes

```text
auditId
deviceId
controlId
ruleId
status
severity
frameworkIds
createdAt
(deviceId, status)
(auditId, status)
```

---

# 11. Collection: evidence

## Purpose

Stores technical evidence supporting findings.

## Schema

```json
{
  "_id": "evidence-uuid",

  "findingId": "finding-uuid",
  "auditId": "audit-uuid",

  "configurationId": "configuration-uuid",
  "versionId": "version-uuid",

  "source": {
    "lineNumber": 32,
    "rawText": "transport input telnet ssh",
    "sourceType": "CONFIGURATION"
  },

  "canonical": {
    "field": "management.telnetEnabled",
    "value": true
  },

  "reason": "Configuration explicitly permits Telnet.",

  "createdAt": "2026-09-24T08:02:00Z"
}
```

## Indexes

```text
findingId
auditId
configurationId
versionId
```

---

# 12. Collection: risk_assessments

## Purpose

Stores risk calculations associated with findings, devices, or audits.

The actual risk calculation belongs to the Cyber Engine.

## Schema

```json
{
  "_id": "risk-uuid",

  "auditId": "audit-uuid",
  "deviceId": "device-uuid",
  "findingId": "finding-uuid",

  "score": 68,
  "level": "HIGH",

  "factors": {
    "severity": 8,
    "assetCriticality": 10,
    "exposure": 8,
    "exploitability": 7
  },

  "calculationVersion": "1.0",

  "calculatedAt": "2026-09-24T08:02:00Z",

  "createdAt": "2026-09-24T08:02:00Z",
  "updatedAt": "2026-09-24T08:02:00Z"
}
```

## Indexes

```text
auditId
deviceId
findingId
calculatedAt
(deviceId, calculatedAt)
```

---

# 13. Collection: drift_events

## Purpose

Stores meaningful differences between configuration versions.

## Schema

```json
{
  "_id": "drift-uuid",

  "deviceId": "device-uuid",

  "fromVersionId": "version-13-uuid",
  "toVersionId": "version-14-uuid",

  "fromVersion": 13,
  "toVersion": 14,

  "changes": [
    {
      "canonicalField": "management.telnetEnabled",
      "before": false,
      "after": true,
      "changeType": "MODIFIED",
      "sourceBefore": "transport input ssh",
      "sourceAfter": "transport input telnet ssh"
    }
  ],

  "affectedControlIds": [
    "control-uuid"
  ],

  "affectedFindingIds": [
    "finding-uuid"
  ],

  "riskBefore": 58,
  "riskAfter": 68,

  "impact": "INCREASED",

  "detectedAt": "2026-09-24T08:02:00Z",

  "createdAt": "2026-09-24T08:02:00Z",
  "updatedAt": "2026-09-24T08:02:00Z"
}
```

## Indexes

```text
deviceId
fromVersionId
toVersionId
detectedAt
impact
(deviceId, detectedAt)
```

---

# 14. Collection: what_if_simulations

## Purpose

Stores proposed changes and their simulated compliance/risk effects.

## Schema

```json
{
  "_id": "simulation-uuid",

  "deviceId": "device-uuid",
  "baseConfigurationVersionId": "version-uuid",

  "name": "Disable Telnet",
  "description": "Simulate disabling Telnet on management access.",

  "changes": [
    {
      "canonicalField": "management.telnetEnabled",
      "oldValue": true,
      "newValue": false
    }
  ],

  "status": "COMPLETED",

  "before": {
    "complianceScore": 74,
    "riskScore": 68,
    "failedControls": 6
  },

  "after": {
    "complianceScore": 82,
    "riskScore": 52,
    "failedControls": 4
  },

  "affectedControlIds": [
    "control-uuid"
  ],

  "affectedFindingIds": [
    "finding-uuid"
  ],

  "createdBy": "user-uuid",

  "createdAt": "2026-09-24T08:10:00Z",
  "updatedAt": "2026-09-24T08:11:00Z"
}
```

## Indexes

```text
deviceId
baseConfigurationVersionId
status
createdBy
createdAt
```

---

# 15. Collection: remediation_templates

## Purpose

Stores curated vendor-specific remediation instructions.

**Do not generate production remediation commands directly from an LLM.**

## Schema

```json
{
  "_id": "template-uuid",

  "controlId": "control-uuid",
  "ruleId": "rule-uuid",

  "vendor": "Cisco",
  "platform": "IOS-XE",

  "title": "Enable SSH version 2",

  "commands": [
    "ip ssh version 2"
  ],

  "description": "Enable SSH version 2.",

  "preconditions": [
    "SSH service is installed"
  ],

  "verification": [
    "show ip ssh"
  ],

  "risk": "LOW",

  "status": "ACTIVE",

  "version": 1,

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Indexes

```text
controlId
ruleId
vendor
platform
status
(controlId, vendor, platform)
```

---

# 16. Collection: remediation_plans

## Purpose

Stores a remediation plan generated for a finding.

## Schema

```json
{
  "_id": "plan-uuid",

  "findingId": "finding-uuid",
  "deviceId": "device-uuid",

  "templateId": "template-uuid",

  "status": "PLANNED",

  "steps": [
    {
      "order": 1,
      "action": "EXECUTE_COMMAND",
      "command": "ip ssh version 2",
      "description": "Enable SSH version 2."
    }
  ],

  "validation": {
    "status": "VALID",
    "validatedAt": "2026-09-24T08:15:00Z",
    "validatedBy": "user-uuid",
    "messages": []
  },

  "verification": {
    "status": "PENDING",
    "verifiedAt": null,
    "messages": []
  },

  "createdBy": "user-uuid",

  "createdAt": "2026-09-24T08:14:00Z",
  "updatedAt": "2026-09-24T08:15:00Z"
}
```

## Indexes

```text
findingId
deviceId
templateId
status
createdBy
```

---

# 17. Collection: ai_mappings

## Purpose

Stores human-validated mappings for previously unknown configuration syntax.

AI suggests; human validates; the mapping becomes reusable.

> **Architectural Note (LLM Integration with 10-Second Fallback):**
> Suggestions are powered by Google Gemini (`gemini-2.5-flash`) via `GeminiSuggestionProvider`. The provider enforces a strict 10-second client timeout window: if the Gemini API call fails, errors, or exceeds 10 seconds, the system seamlessly falls back to `DeterministicStubSuggestionProvider`. All generated suggestions remain strictly `PENDING_REVIEW` until approved through explicit human review (`review.reviewerId`). The schema fields (`confidence`, `reason`, `status`, `suggestedBy`) remain completely unchanged, preserving the exact JSON contract.

## Schema

```json
{
  "_id": "mapping-uuid",

  "vendor": "Juniper",
  "platform": "Junos",

  "rawSyntax": "set security foo-bar 123",

  "canonicalField": "security.fooBar",
  "mappedValue": 123,

  "unit": "seconds",

  "confidence": 0.91,

  "reason": "Similar validated syntax was found.",

  "status": "PENDING_REVIEW",

  "suggestedBy": "AI",

  "review": {
    "reviewerId": null,
    "reviewedAt": null,
    "comment": null
  },

  "usageCount": 0,

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:00Z"
}
```

## Status

```text
PENDING_REVIEW
APPROVED
REJECTED
```

## Indexes

```text
vendor
platform
rawSyntax
canonicalField
status
(vendor, platform, rawSyntax)
```

---

# 18. Collection: ai_jobs

## Purpose

Tracks asynchronous AI analysis operations.

> **Architectural Note (LLM Integration with 10-Second Fallback):**
> Tracks analysis operations executed against Google Gemini (`gemini-2.5-flash`) via `GeminiSuggestionProvider` or its automatic 10-second fallback delegate `DeterministicStubSuggestionProvider`. The schema fields (`type`, `status`, `input`, `result`, `error`) remain completely unchanged, capturing operation status, suggestion counts, latency, and fallback execution under the exact existing contract.

## Schema

```json
{
  "_id": "ai-job-uuid",

  "type": "UNKNOWN_SYNTAX_ANALYSIS",

  "status": "COMPLETED",

  "configurationId": "configuration-uuid",
  "versionId": "version-uuid",

  "input": {
    "unknownCount": 3
  },

  "result": {
    "suggestionCount": 3,
    "highConfidenceCount": 2
  },

  "error": null,

  "startedAt": "2026-09-24T08:00:00Z",
  "completedAt": "2026-09-24T08:00:10Z",

  "createdBy": "user-uuid",

  "createdAt": "2026-09-24T08:00:00Z",
  "updatedAt": "2026-09-24T08:00:10Z"
}
```

## Indexes

```text
status
type
configurationId
versionId
createdBy
createdAt
```

---

# 19. Collection: reports

## Purpose

Stores generated report metadata.

Do not necessarily store large PDF binaries inside this document.

Prefer object/file storage or filesystem storage and keep a reference.

## Schema

```json
{
  "_id": "report-uuid",

  "title": "Core Router Compliance Report",

  "type": "COMPLIANCE",

  "deviceId": "device-uuid",
  "auditId": "audit-uuid",

  "frameworkIds": [
    "framework-cis-uuid",
    "framework-nist-uuid"
  ],

  "status": "READY",

  "complianceScore": 84.0,

  "file": {
    "storageType": "LOCAL",
    "path": "/reports/report-uuid.pdf",
    "contentType": "application/pdf",
    "sizeBytes": 182340
  },

  "generatedBy": "user-uuid",

  "generatedAt": "2026-09-24T08:20:00Z",

  "createdAt": "2026-09-24T08:20:00Z",
  "updatedAt": "2026-09-24T08:20:00Z"
}
```

## Indexes

```text
deviceId
auditId
status
type
generatedAt
```

---

# 20. Package A collections used by Package B

Package B should reference these collections but should not duplicate ownership.

## devices

Package B needs at least:

```text
deviceId
hostname
vendor
platform
model
osVersion
criticality
environment
location
status
```

The current frontend Device model also exposes:

```text
ip
risk
compliance
lastAudit
findings
```

Prefer treating `risk`, `compliance`, `findings`, and `lastAudit` as derived/current posture values rather than duplicating historical truth unnecessarily.

---

## configurations

Package B needs:

```text
configurationId
deviceId
```

and metadata such as:

```text
filename
uploadedBy
uploadedAt
hash
status
```

---

## configuration_versions

Package B needs:

```text
versionId
configurationId
deviceId
version
content
hash
createdAt
```

Large raw configuration content should be handled deliberately; do not duplicate the same full content into normalized configuration, findings, evidence, and audits.

---

# 21. Relationship map

```text
Device
  │
  ├── Configuration
  │       │
  │       └── ConfigurationVersion
  │                │
  │                └── NormalizedConfiguration
  │
  └── Audits
          │
          ├── Frameworks
          │      └── Controls
          │              └── ComplianceRules
          │
          ├── Findings
          │      └── Evidence
          │
          ├── RiskAssessments
          │
          ├── DriftEvents
          │
          ├── WhatIfSimulations
          │
          ├── RemediationPlans
          │      └── RemediationTemplates
          │
          └── Reports

ConfigurationVersion
        │
        └── AI Jobs
                └── AI Mappings
```

---

# 22. Embedding vs referencing

Do not blindly embed everything.

## Reference

Use references for:

```text
device
configuration
configuration version
framework
control
rule
audit
finding
evidence
risk assessment
report
```

These resources have independent APIs and lifecycle operations.

## Embed

Small immutable/value-like structures can be embedded:

```text
risk factors
sourceMap entries
what-if changes
remediation steps
audit summary
configuration canonical JSON
```

This is a design choice and should remain consistent.

---

# 23. Query requirements derived from Package B APIs

The schema must support:

## Audits

```text
find audits by device
find audits by configuration
find audits by status
find audits by date
```

## Findings

```text
find findings by audit
find findings by device
find findings by severity
find findings by status
find findings by framework
find findings by control
```

## Risk

```text
find current device risk
find risk history
find risk by finding
find risk by audit
```

## Drift

```text
find drift by device
find drift by version pair
find affected controls
find risk impact
```

## AI

```text
find mappings by vendor
find mappings by platform
find mappings pending review
find mappings by raw syntax
find AI jobs by configuration/version
```

## Reports

```text
find reports by device
find reports by audit
find reports by status
```

---

# 24. Required Mongo indexes

Minimum Package B indexes:

```text
normalized_configurations:
  deviceId
  configurationId
  versionId
  createdAt

frameworks:
  code + version
  status

controls:
  frameworkId
  controlId
  frameworkId + controlId

compliance_rules:
  ruleCode
  controlId
  frameworkIds
  status

audits:
  deviceId
  configurationId
  versionId
  status
  createdAt
  deviceId + createdAt

findings:
  auditId
  deviceId
  controlId
  ruleId
  status
  severity
  frameworkIds
  createdAt
  deviceId + status
  auditId + status

evidence:
  findingId
  auditId
  configurationId
  versionId

risk_assessments:
  auditId
  deviceId
  findingId
  calculatedAt
  deviceId + calculatedAt

drift_events:
  deviceId
  fromVersionId
  toVersionId
  detectedAt
  deviceId + detectedAt

what_if_simulations:
  deviceId
  baseConfigurationVersionId
  status
  createdBy
  createdAt

remediation_templates:
  controlId
  ruleId
  vendor
  platform
  status
  controlId + vendor + platform

remediation_plans:
  findingId
  deviceId
  templateId
  status

ai_mappings:
  vendor
  platform
  rawSyntax
  canonicalField
  status
  vendor + platform + rawSyntax

ai_jobs:
  status
  type
  configurationId
  versionId
  createdBy
  createdAt

reports:
  deviceId
  auditId
  status
  type
  generatedAt
```

Avoid creating every possible index automatically. Verify actual query patterns before adding additional indexes.

---

# 25. MongoDB implementation rules for Antigravity

Antigravity must:

1. Inspect the existing repository before creating documents/repositories.
2. Reuse existing Package A IDs and models where available.
3. Never create duplicate Device/User/Configuration ownership in Package B.
4. Use Spring Data MongoDB.
5. Use UUID strings consistently for domain IDs.
6. Use `Instant` for timestamps.
7. Use DTOs at the REST boundary.
8. Keep Mongo documents separate from API DTOs.
9. Use repositories for persistence.
10. Keep business orchestration in services.
11. Do not put cybersecurity decision logic in controllers.
12. Do not implement Cyber Engine algorithms inside repositories.
13. Do not invent fields that contradict the API specifications.
14. If a required field is missing from the specification, stop and flag it before silently changing the contract.
15. Create indexes based on documented API query requirements.
16. Add repository tests for important query methods.
17. Add integration tests against the project's chosen MongoDB test strategy.
18. Verify persistence through Postman/API tests.
19. Never commit real credentials or secrets.
20. Preserve backward compatibility with Package A integration contracts.

---

# 26. Important architecture boundary

The following are API/data responsibilities:

```text
Controller
Service orchestration
DTO validation
Mongo persistence
pagination
filtering
authentication/authorization integration
job tracking
resource lifecycle
```

The following are Cyber Engine responsibilities:

```text
vendor parsing algorithms
normalization algorithms
compliance decision logic
finding generation logic
evidence derivation
risk calculation
drift calculation
what-if calculation
remediation validation logic
AI semantic analysis
```

Package B calls these capabilities through stable Java interfaces.

**Do not duplicate Cyber Engine logic merely to make an API appear functional.**

Use mocks/stubs when the real engine is not yet available.

---

# 27. Frontend compatibility requirements

The current frontend expects data corresponding to:

```text
Device
Configuration
Audit
Finding
Framework
DriftEvent
AiMapping
Report
DashboardData
```

Examples of fields visible in the frontend include:

```text
Device:
id
hostname
vendor
platform
ip
model
osVersion
environment
location
status
lastAudit
risk
compliance
criticality
findings

Configuration:
id
deviceId
version
filename
uploadedBy
uploadedAt
size
status
hash
lines

Audit:
id
deviceId
configurationId
date
createdAt
duration
findings
frameworks
compliance
status

Finding:
id
deviceId
auditId
control
title
severity
status
line
framework
createdAt
rule
description
expected
actual
impact
canonicalField
remediation

Framework:
id
name
version
category
mappedRules
coverage
controls
note

DriftEvent:
id
deviceId
version
date
change
impact
controls
riskBefore
riskAfter
finding

AiMapping:
id
syntax
vendor
confidence
canonicalField
suggestedValue
reason
status
reviewer
date

Report:
id
title
type
deviceId
auditId
date
status
compliance
```

The backend API response DTOs may transform normalized Mongo fields into these frontend-friendly shapes. Do not distort the database schema solely to copy frontend mock structures.

---

# 28. Migration from frontend mocks

The current frontend contains mock/demo data.

The implementation sequence should be:

```text
MongoDB schema
      ↓
Spring Boot repositories
      ↓
Services
      ↓
REST APIs
      ↓
Postman verification
      ↓
Replace frontend mocks with API calls
```

Do not copy all mock data permanently into MongoDB.

Instead, create a small controlled seed dataset for development/demo.

---

# 29. Seed data recommendation

Create seed data for:

```text
4 devices
5 configuration examples
multiple configuration versions
4 frameworks
sample controls
sample compliance rules
sample audits
sample findings
sample evidence
sample risk assessments
sample drift events
sample AI mappings
sample remediation templates
sample reports
```

Use synthetic data only.

The frontend currently labels the dashboard as a demo/synthetic environment; preserve that concept in development data.

---

# 30. Final schema acceptance criteria

MongoDB schema work is complete only when:

```text
[ ] All Package B collections are defined.
[ ] Package A references are clearly defined.
[ ] Every Package B API has required persistence fields.
[ ] IDs are consistent.
[ ] Timestamp strategy is consistent.
[ ] Status enums are consistent.
[ ] Required indexes are documented.
[ ] Example documents exist.
[ ] Repository interfaces can be derived from the schema.
[ ] Frontend response models can be produced by API DTOs.
[ ] No Package B collection duplicates Package A ownership.
[ ] Cyber Engine logic is not embedded in Mongo repositories.
[ ] Postman tests can create/read/update relevant resources.
[ ] Mongo persistence can be verified during integration tests.
[ ] Schema changes are documented before implementation.
```

---

# 31. Antigravity instruction

Use this document as the **MongoDB data contract for Package B**.

Before implementing a collection:

```text
1. Inspect existing repository.
2. Check whether the collection/model already exists.
3. Compare existing code with this schema.
4. Reuse compatible existing code.
5. Identify conflicts.
6. Do not silently overwrite conflicting architecture.
7. Implement document + repository + service integration.
8. Add indexes.
9. Add tests.
10. Verify through the relevant API.
11. Only then continue to the next API.
```

If the Cyber Engine contract, Package A contract, or existing repository contradicts this document, **stop and report the conflict rather than inventing a third contract**.

