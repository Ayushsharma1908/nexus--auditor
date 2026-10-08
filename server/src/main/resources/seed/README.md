# NEXUS-COMPLY Dataset — Antigravity V2

This package is the implementation-ready dataset foundation for SIH26155.

### Use it for
1. Removing hardcoded/mock framework/vendor data from the application.
2. Seeding MongoDB reference collections.
3. Building deterministic parser/normalizer knowledge from vendor syntax.
4. Running repeatable evaluation cases.

### Included
- Source manifest and provenance
- Vendor semantic mappings
- Framework catalog
- Compliance-rule templates
- Evaluation cases
- Mongo seed JSON
- Mongo validation schemas
- Dataset-to-Mongo migration map

### Important
This is not the complete text of CIS Benchmarks, ISO/IEC 27001, vendor manuals, or other copyrighted standards. Use authoritative source files/metadata where legally available and store source/version references.

### Recommended import order
frameworks -> vendor_knowledge -> compliance_rules -> framework_mappings -> evaluation_cases

Do not seed runtime entities such as users/devices/audits/findings as fake demo records.
