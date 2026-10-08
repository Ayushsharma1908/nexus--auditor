# NEXUS-COMPLY SIH26155 — Dataset-to-Mongo Migration Map

## Purpose
This package is a structured dataset foundation for Antigravity. It is NOT a redistribution of complete CIS, ISO, vendor manuals, or other copyrighted standards.

## Collections populated by this package
- `frameworks` <- `mongo_seed/frameworks.json`
- `vendor_knowledge` <- `mongo_seed/vendor_knowledge.json`
- `compliance_rules` <- `mongo_seed/compliance_rules.json`
- `framework_mappings` <- intentionally empty until mappings are validated from authoritative sources
- `evaluation_cases` <- `mongo_seed/evaluation_cases.json`

## Important rule
Do not invent official framework/control IDs. The compliance-rule templates in this package are implementation templates and must be linked to validated CIS/NIST/STIG/ISO control references before being represented as authoritative compliance requirements.

## Existing runtime collections that must remain
`users`, `roles`, `devices`, `configurations`, `configuration_versions`, `vendor_detections`,
`normalized_configurations`, `audits`, `findings`, `evidence`, `risk_assessments`,
`drift_events`, `what_if_simulations`, `remediation_templates`, `remediation_plans`,
`ai_mappings`, `ai_jobs`, `reports`, `audit_logs`.

## Required collection naming
Use `what_if_simulations` consistently. Do not create a second `simulations` collection.

## No browser-to-Mongo access
React must call Spring Boot APIs only. MongoDB credentials must never reach the browser.

## Dataset provenance
Every derived rule/knowledge item should retain source metadata/version/reference where the schema supports it.
