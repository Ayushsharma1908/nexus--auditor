# NEXUS-COMPLY Cyber Layer — Live Demo Guide

This guide details the end-to-end "Golden Path" demonstration script for **NEXUS-COMPLY (SIH26155)**, executing the full multi-vendor network security compliance auditing lifecycle without requiring an external UI.

---

## 1. Quickstart: Running the Live Demo

Run the automated demonstration suite using Maven:

```powershell
# From the project root (D:\auditor):
mvn test -Dtest=FullDemoScenarioTest
```

To run with full test output logging:

```powershell
mvn test -Dtest=FullDemoScenarioTest > demo\mvn-demo-run.log 2>&1
```

Execution time is typically **under 5 seconds** using the in-memory test database.

---

## 2. Demonstration Workflow ("Golden Path")

The test class [`FullDemoScenarioTest`](file:///d:/auditor/src/test/java/com/nexuscomply/cyber/demo/FullDemoScenarioTest.java) executes 8 sequential phases:

```
[1. Ingestion] -> [2. Posture] -> [3. Remediation] -> [4. Simulation (What-If)]
       |
[8. Report]    <- [7. AI Loop]  <- [6. Drift]       <- [5. Fix Deployment]
```

1. **Multi-Vendor Baseline Ingestion:**
   Loads non-compliant configuration fixtures across 4 major network vendors:
   - Cisco IOS XE (`fixtures/cisco-ios-xe-bad.cfg`)
   - Juniper JunOS (`fixtures/juniper-junos-bad.cfg`)
   - Fortinet FortiOS (`fixtures/fortinet-fortios-bad.cfg`)
   - Palo Alto PAN-OS (`fixtures/paloalto-panos-bad.cfg`)
   Orchestrates audits across all 4 devices (`demo-cisco-01`, `demo-juniper-01`, `demo-fortinet-01`, `demo-paloalto-01`).

2. **Initial Posture & Fleet Summary:**
   Aggregates compliance scores across CIS, NIST SP 800-53, and ISO 27001 controls. Highlights that `demo-cisco-01` has Telnet enabled, yielding 3 open findings and a 0.0% compliance score.

3. **Automated Remediation Generation:**
   Resolves vendor-specific remediation templates for the Cisco Telnet violation. Produces remediation commands annotated with confirmation status (`[CONFIRMED]`). The plan is created in `PLANNED` status (never auto-approved).

4. **What-If Simulation (Pre-Flight Impact Analysis):**
   Simulates disabling Telnet *in memory* before touching production devices. Proves risk reduction (70 $\rightarrow$ 0), 100% compliance score, and finding delta of -3 with `DECREASED` impact.

5. **Deploy Fix & Re-Audit:**
   Ingests the compliant configuration (`fixtures/cisco-ios-xe-good.cfg`) with Telnet disabled and SSH v2 enforced. Re-audit executes and yields **0 open findings** and **100% compliance**.

6. **Configuration Drift Detection:**
   Compares normalized configurations between v1.0 and v2.0. Emits drift telemetry classifying `security.telnet.enabled` as `IMPROVED`, with risk dropping from 70 to 0 and overall impact `DECREASED`.

7. **AI Unknown-Syntax Self-Learning Loop:**
   Introduces an unrecognized Cisco CLI command (`custom-security-feature enable`). Captures line into `ai_mappings` with status `PENDING_REVIEW`, requests an AI suggestion, requires explicit human review and approval (`approve()`), and re-audits to demonstrate that the parser now understands the syntax (`APPROVED_MAPPING`).

8. **Executive Markdown Report Export:**
   Generates a GitHub-flavored Markdown audit report containing executive posture metrics, control coverage, evidence traceability, and remediation instructions.

---

## 3. Exported Demonstration Artifacts

All outputs are written directly to disk under [`demo/output/`](file:///d:/auditor/demo/output/):

| File | Format | Description |
| :--- | :--- | :--- |
| `fleet_summary_initial.json` | JSON | Aggregated multi-vendor fleet posture across all 4 devices. |
| `cisco_posture_initial.json` | JSON | Initial non-compliant posture for Cisco (Telnet violations). |
| `cisco_remediation_plan.json` | JSON | Automated remediation plan with vendor commands. |
| `what_if_simulation.json` | JSON | Pre-deployment What-If simulation results showing risk delta (-70). |
| `cisco_drift_event.json` | JSON | Configuration drift telemetry record classifying security improvement. |
| `cisco_final_audit_report.md` | Markdown | Complete executive compliance audit report with evidence traceability. |

---

## 4. Key Value Propositions Demonstrated to Evaluators

1. **Multi-Vendor Normalized Ingestion:** Universal canonical security model unifying Cisco, Juniper, Fortinet, and Palo Alto.
2. **Cross-Framework Mapping:** Single canonical fact evaluated simultaneously against CIS, NIST SP 800-53 Rev 5, and ISO/IEC 27001.
3. **Pre-Flight Safety (What-If):** Change impact is calculated in-memory without operational side effects.
4. **Governed AI Loop:** AI suggestions are strictly advisory and require human approval before parser ingestion.
5. **Continuous Drift Auditing:** Detects unauthorized config changes and computes risk deltas between snapshots.
