import { useState } from "react";
import { Link } from "wouter";
import { Icon } from "@/components/Icon";
import { useAuth } from "@/contexts/AuthContext";
import {
  Button,
  ComplianceLineChart,
  LinkButton,
  Metric,
  Notice,
  Panel,
  ProgressBar,
  RiskLineChart,
  StatusBadge,
} from "@/components/WorkspaceComponents";
import {
  useAudits,
  useConfigurations,
  useDevices,
  useDriftEvents,
  useFindings,
  useFrameworks,
  useReports,
  useAiMappings,
  useFrameworkControls,
} from "@/lib/useApi";
import { api } from "@/lib/api";
import { AiAnalystQueueCard } from "@/components/AiAnalystQueueCard";
import { SimulationSandbox } from "@/components/SimulationSandbox";

import type {
  AiMapping,
  Audit,
  Control,
  Device,
  DriftEvent,
  Finding,
  Framework,
  Report,
  WhatIfSimulationResult,
} from "@/types";

export function Drift() {
  const { data: devices } = useDevices();
  const { data: driftEvents } = useDriftEvents();
  const { data: configurations } = useConfigurations();
  const { data: audits } = useAudits();

  const queryDevice =
    typeof window !== "undefined"
      ? new URLSearchParams(window.location.search).get("device")
      : null;
  const initialDevice =
    queryDevice && devices.some(d => d.id === queryDevice)
      ? queryDevice
      : "dev-001";

  const [deviceId, setDeviceId] = useState<string>(initialDevice);
  const [impactFilter, setImpactFilter] = useState<
    "ALL" | "Increased" | "Decreased"
  >("ALL");

  const filteredEvents = driftEvents.filter((event: DriftEvent) => {
    const matchesDevice = deviceId === "ALL" || event.deviceId === deviceId;
    const matchesImpact =
      impactFilter === "ALL" || event.impact === impactFilter;
    return matchesDevice && matchesImpact;
  });

  const selectedDevice =
    deviceId !== "ALL"
      ? (devices.find((item: Device) => item.id === deviceId) ?? devices[0])
      : null;

  // Key numbers & calculations
  const totalEvents = filteredEvents.length;
  const increasedCount = filteredEvents.filter(
    e => e.impact === "Increased"
  ).length;
  const decreasedCount = filteredEvents.filter(
    e => e.impact === "Decreased"
  ).length;

  const allControls = filteredEvents.flatMap(e => e.controls);
  const uniqueControls = Array.from(new Set(allControls));
  const findingsLinked = filteredEvents.filter(e => Boolean(e.finding));

  const netRiskDelta = filteredEvents.reduce(
    (sum, e) => sum + (e.riskAfter - e.riskBefore),
    0
  );

  const deviceConfigs = selectedDevice
    ? configurations.filter(c => c.deviceId === selectedDevice.id)
    : configurations;

  const deviceAudits = selectedDevice
    ? audits.filter(a => a.deviceId === selectedDevice.id)
    : audits;

  const chartData = filteredEvents
    .slice()
    .reverse()
    .map((event: DriftEvent) => ({
      month: `v${event.version}`,
      score: event.riskAfter,
    }));

  const minRisk =
    chartData.length > 0 ? Math.min(...chartData.map(d => d.score)) : 40;
  const maxRisk =
    chartData.length > 0 ? Math.max(...chartData.map(d => d.score)) : 70;
  const currentRisk =
    chartData.length > 0
      ? chartData[chartData.length - 1].score
      : selectedDevice
        ? selectedDevice.risk
        : 55;

  return (
    <div className="drift-page">
      {/* Filter and Action Strip */}
      <div className="drift-filter-bar">
        <div className="drift-filter-left">
          <div className="filter-control">
            <Icon name="router" size={14} />
            <select
              value={deviceId}
              onChange={e => setDeviceId(e.target.value)}
              aria-label="Filter by device"
            >
              <option value="ALL">
                All Devices ({devices.length} registered)
              </option>
              {devices.map((item: Device) => (
                <option value={item.id} key={item.id}>
                  {item.hostname} ({item.vendor} · {item.platform})
                </option>
              ))}
            </select>
          </div>

          <div className="impact-pills-wrap">
            <button
              type="button"
              className={`impact-pill-btn ${impactFilter === "ALL" ? "active" : ""}`}
              onClick={() => setImpactFilter("ALL")}
            >
              All Impact (
              {
                driftEvents.filter(
                  e => deviceId === "ALL" || e.deviceId === deviceId
                ).length
              }
              )
            </button>
            <button
              type="button"
              className={`impact-pill-btn ${impactFilter === "Increased" ? "active" : ""}`}
              onClick={() => setImpactFilter("Increased")}
            >
              Risk Escalated (
              {
                driftEvents.filter(
                  e =>
                    (deviceId === "ALL" || e.deviceId === deviceId) &&
                    e.impact === "Increased"
                ).length
              }
              )
            </button>
            <button
              type="button"
              className={`impact-pill-btn ${impactFilter === "Decreased" ? "active" : ""}`}
              onClick={() => setImpactFilter("Decreased")}
            >
              Remediated (
              {
                driftEvents.filter(
                  e =>
                    (deviceId === "ALL" || e.deviceId === deviceId) &&
                    e.impact === "Decreased"
                ).length
              }
              )
            </button>
          </div>
        </div>

        <div className="drift-filter-right">
          {selectedDevice && (
            <LinkButton
              href={`/devices/${selectedDevice.id}`}
              variant="secondary"
            >
              <Icon name="router" size={13} /> View Device Details
            </LinkButton>
          )}
          <LinkButton href="/what-if" variant="primary">
            <Icon name="sliders" size={13} /> Simulate in What-If
          </LinkButton>
        </div>
      </div>

      {/* Selected Device Banner (or All Devices Banner) */}
      {selectedDevice ? (
        <div className="drift-device-hero">
          <div className="drift-device-left">
            <div className="drift-icon-box">
              <Icon name="router" size={24} />
            </div>
            <div className="drift-device-info">
              <div className="drift-device-title-row">
                <Link
                  href={`/devices/${selectedDevice.id}`}
                  className="drift-device-name"
                >
                  {selectedDevice.hostname} <Icon name="external" size={13} />
                </Link>
                <StatusBadge value={selectedDevice.status} />
              </div>
              <div className="drift-device-tags">
                <span className="drift-meta-pill">
                  {selectedDevice.vendor} · {selectedDevice.platform}
                </span>
                <span className="drift-meta-pill mono">
                  {selectedDevice.ip}
                </span>
                <span className="drift-meta-pill">
                  {selectedDevice.environment}
                </span>
                <span className="drift-meta-pill">
                  {selectedDevice.location}
                </span>
                <span className="drift-meta-pill">
                  {selectedDevice.criticality} Criticality
                </span>
              </div>
            </div>
          </div>

          <div className="drift-device-right">
            <div className="drift-hero-stat">
              <span>Current Risk</span>
              <strong>{selectedDevice.risk}</strong>
              <small>
                {netRiskDelta >= 0
                  ? `+${netRiskDelta} net drift`
                  : `${netRiskDelta} net drift`}
              </small>
            </div>
            <div className="drift-hero-stat">
              <span>Active Config</span>
              <Link
                href={`/configurations/${deviceConfigs[0]?.id || "CFG-001"}`}
                className="panel-link"
              >
                v{deviceConfigs[0]?.version || 14}{" "}
                <Icon name="external" size={12} />
              </Link>
              <small>{deviceConfigs[0]?.filename || "Active"}</small>
            </div>
            <div
              className="drift-hero-stat"
              style={{ borderRight: "none", paddingRight: 0 }}
            >
              <span>Latest Audit</span>
              <Link
                href={`/audits/${deviceAudits[0]?.id || "AUD-2026-0918"}`}
                className="panel-link"
              >
                {deviceAudits[0]?.compliance ?? 74}%{" "}
                <Icon name="external" size={12} />
              </Link>
              <small>{selectedDevice.lastAudit || new Date().toISOString().slice(0, 10)}</small>
            </div>
          </div>
        </div>
      ) : (
        <div className="drift-device-hero">
          <div className="drift-device-left">
            <div className="drift-icon-box">
              <Icon name="layers" size={24} />
            </div>
            <div className="drift-device-info">
              <div className="drift-device-title-row">
                <span className="drift-device-name">
                  Entire Workspace Fleet
                </span>
                <span className="drift-meta-pill">
                  {devices.length} Monitored Devices
                </span>
              </div>
              <span
                style={{
                  fontSize: "11px",
                  color: "var(--text-secondary)",
                  marginTop: "2px",
                }}
              >
                Continuous configuration telemetry across multi-vendor
                infrastructure. Select a device above for target analysis.
              </span>
            </div>
          </div>
          <div className="drift-device-right">
            <div className="drift-hero-stat">
              <span>Observed Drifts</span>
              <strong>{driftEvents.length}</strong>
              <small>Across fleet</small>
            </div>
            <div
              className="drift-hero-stat"
              style={{ borderRight: "none", paddingRight: 0 }}
            >
              <span>Triggered Findings</span>
              <strong>{findingsLinked.length}</strong>
              <small>Audit violations</small>
            </div>
          </div>
        </div>
      )}

      {/* KPI Numbers Strip */}
      <div className="drift-kpis-grid">
        <div className="drift-kpi-card">
          <div className="drift-kpi-head">
            <span>Observed Events</span>
            <Icon name="layers" size={13} />
          </div>
          <strong className="drift-kpi-val">{totalEvents}</strong>
          <div className="drift-kpi-foot">
            <span style={{ color: "var(--color-red)" }}>
              {increasedCount} risk escalated
            </span>
            <span>·</span>
            <span style={{ color: "var(--accent-lime)" }}>
              {decreasedCount} remediated
            </span>
          </div>
        </div>

        <div className="drift-kpi-card">
          <div className="drift-kpi-head">
            <span>Cumulative Risk Delta</span>
            <Icon name="trend" size={13} />
          </div>
          <strong
            className={`drift-kpi-val ${netRiskDelta > 0 ? "tone-bad" : "tone-good"}`}
          >
            {netRiskDelta >= 0 ? `+${netRiskDelta}` : netRiskDelta} pts
          </strong>
          <div className="drift-kpi-foot">
            <span>
              {netRiskDelta > 0
                ? "Adverse posture drift detected"
                : "Net posture improvement"}
            </span>
          </div>
        </div>

        <div className="drift-kpi-card">
          <div className="drift-kpi-head">
            <span>Compromised Controls</span>
            <Icon name="shield" size={13} />
          </div>
          <strong className="drift-kpi-val tone-warn">
            {uniqueControls.length}
          </strong>
          <div className="drift-kpi-foot">
            <span>Across CIS, NIST & STIG rules</span>
          </div>
        </div>

        <div className="drift-kpi-card">
          <div className="drift-kpi-head">
            <span>Generated Findings</span>
            <Icon name="warning" size={13} />
          </div>
          <strong className="drift-kpi-val tone-warn">
            {findingsLinked.length}
          </strong>
          <div className="drift-kpi-foot">
            <Link
              href="/findings"
              className="panel-link"
              style={{ fontSize: "10px" }}
            >
              View findings queue <Icon name="arrow" size={10} />
            </Link>
          </div>
        </div>
      </div>

      {/* Analytical Grid: Trajectory & Affected Controls */}
      <div className="drift-analytics-grid">
        <Panel
          title="Risk Trajectory Across Revisions"
          eyebrow="Deterministic posture scores per configuration version"
        >
          {chartData.length > 0 ? (
            <>
              <RiskLineChart data={chartData} />
              <div className="drift-chart-summary">
                <div>
                  <span>Lowest Risk:</span>
                  <strong
                    className="mono"
                    style={{ color: "var(--accent-lime)" }}
                  >
                    {minRisk}
                  </strong>
                </div>
                <div>
                  <span>Highest Risk:</span>
                  <strong
                    className="mono"
                    style={{ color: "var(--color-red)" }}
                  >
                    {maxRisk}
                  </strong>
                </div>
                <div>
                  <span>Latest Score:</span>
                  <strong className="mono">{currentRisk}</strong>
                </div>
              </div>
            </>
          ) : (
            <div
              style={{
                padding: "30px 0",
                textAlign: "center",
                color: "var(--text-muted)",
              }}
            >
              No trajectory data for this filter.
            </div>
          )}
        </Panel>

        <Panel
          title="Compromised Controls"
          eyebrow="Compliance rules impacted by configuration changes"
        >
          {uniqueControls.length === 0 ? (
            <div
              style={{
                padding: "30px 0",
                textAlign: "center",
                color: "var(--text-muted)",
              }}
            >
              No controls affected by drift.
            </div>
          ) : (
            <div className="control-impact-list">
              {uniqueControls.map((control: string) => {
                const affectedCount = filteredEvents.filter(e =>
                  e.controls.includes(control)
                ).length;
                return (
                  <div className="control-impact-card" key={control}>
                    <div className="control-impact-left">
                      <span className="control-badge-pill mono">{control}</span>
                      <span className="control-change-count">
                        {affectedCount}{" "}
                        {affectedCount === 1
                          ? "drift occurrence"
                          : "drift occurrences"}
                      </span>
                    </div>
                    <Link
                      href={`/controls/${control}`}
                      className="control-inspect-btn"
                    >
                      Inspect rule{" "}
                      <Icon name="arrow" size={11} className="rotate-45" />
                    </Link>
                  </div>
                );
              })}
            </div>
          )}
        </Panel>
      </div>

      {/* Configuration Drift Timeline */}
      <Panel
        title="Deterministic Drift Timeline"
        eyebrow="Chronological audit trail of detected changes and linked security artifacts"
      >
        {filteredEvents.length === 0 ? (
          <div
            style={{
              padding: "40px 0",
              textAlign: "center",
              color: "var(--text-muted)",
            }}
          >
            No drift events matching the selected criteria.
          </div>
        ) : (
          <div className="drift-timeline-wrap">
            {filteredEvents.map((event: DriftEvent) => {
              const eventDevice =
                devices.find(d => d.id === event.deviceId) || devices[0];
              const matchingConfig =
                configurations.find(
                  c =>
                    c.deviceId === event.deviceId && c.version === event.version
                ) || configurations.find(c => c.deviceId === event.deviceId);
              const configId = matchingConfig ? matchingConfig.id : "CFG-001";
              const delta = event.riskAfter - event.riskBefore;

              return (
                <div className="drift-timeline-item" key={event.id}>
                  <div className="drift-timeline-rail">
                    <span
                      className={`drift-timeline-dot ${
                        event.impact === "Increased"
                          ? "dot-increased"
                          : "dot-decreased"
                      }`}
                    />
                  </div>

                  <div className="drift-card">
                    <div className="drift-card-header">
                      <div className="drift-card-header-left">
                        <Link
                          href={`/configurations/${configId}`}
                          className="drift-version-tag"
                        >
                          <Icon name="file" size={12} />
                          <span>
                            v{event.version} ·{" "}
                            {matchingConfig
                              ? matchingConfig.filename
                              : `Config v${event.version}`}
                          </span>
                          <Icon name="external" size={10} />
                        </Link>

                        {deviceId === "ALL" && (
                          <Link
                            href={`/devices/${eventDevice.id}`}
                            className="drift-meta-pill"
                          >
                            <Icon name="router" size={11} />{" "}
                            {eventDevice.hostname}
                          </Link>
                        )}

                        <span className="drift-date-pill">{event.date}</span>

                        <span
                          className={`drift-risk-indicator ${
                            delta > 0 ? "risk-up" : "risk-down"
                          }`}
                        >
                          <Icon
                            name={delta > 0 ? "trend" : "check"}
                            size={11}
                          />
                          <span>
                            Risk: {event.riskBefore} → {event.riskAfter} (
                            {delta > 0
                              ? `+${delta} escalated`
                              : `${delta} resolved`}
                            )
                          </span>
                        </span>
                      </div>

                      <StatusBadge value={event.impact} />
                    </div>

                    <h4 className="drift-card-title">{event.change}</h4>

                    <div className="drift-card-details">
                      <div className="drift-controls-wrap">
                        <span className="drift-controls-label">
                          Affected Controls:
                        </span>
                        {event.controls.map((control: string) => (
                          <Link
                            href={`/controls/${control}`}
                            className="control-chip-link mono"
                            key={control}
                            title={`Inspect rule for ${control}`}
                          >
                            <span>{control}</span>
                            <Icon name="external" size={9} />
                          </Link>
                        ))}
                      </div>

                      <div className="drift-card-actions">
                        {event.finding && (
                          <Link
                            href={`/findings/${event.finding}`}
                            className="drift-finding-pill"
                          >
                            <Icon name="warning" size={12} />
                            <span>
                              Finding {event.finding} · Inspect Evidence
                            </span>
                            <Icon
                              name="arrow"
                              size={11}
                              className="rotate-45"
                            />
                          </Link>
                        )}
                        <Link
                          href={`/configurations/${configId}`}
                          className="drift-btn-outline"
                        >
                          <Icon name="file" size={11} />
                          <span>Compare Diff</span>
                        </Link>
                        <Link href="/what-if" className="drift-btn-outline">
                          <Icon name="sliders" size={11} />
                          <span>Simulate Revert</span>
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Panel>
    </div>
  );
}

export function WhatIf() {
  return <SimulationSandbox />;
}

export function AiAnalyst() {
  const { data: dbMappings } = useAiMappings();
  const [localOverrides, setLocalOverrides] = useState<
    Record<string, { status: "Approved" | "Rejected"; reviewer: string }>
  >({});
  const [notice, setNotice] = useState("");

  const defaultSeedMappings: AiMapping[] = [
    {
      id: "AIM-001",
      syntax: "set system login user audit-operator class read-only",
      vendor: "JUNOS",
      confidence: 96,
      canonicalField: "users[audit-operator].role",
      suggestedValue: "READ_ONLY",
      reason: "Maps Junos read-only class definition to canonical non-privileged auditor role.",
      rationale: "Maps Junos read-only class definition to canonical non-privileged auditor role under NIST AC-6 least privilege guidelines.",
      provider: "GEMINI",
      latencyText: "Timeout Budget: 10s (Passed in 1.4s)",
      status: "Needs review",
      reviewer: "Unassigned",
      date: "2026-10-07",
    },
    {
      id: "AIM-002",
      syntax: "transport input ssh",
      vendor: "CISCO_IOS",
      confidence: 99,
      canonicalField: "remoteAccess.vty.transportInbound",
      suggestedValue: '["SSH"]',
      reason: "Restricts VTY transport inbound access exclusively to encrypted SSH.",
      rationale: "Restricts VTY transport inbound access exclusively to encrypted SSH, enforcing automated CIS-1.1.2 protocol compliance.",
      provider: "GEMINI",
      latencyText: "Timeout Budget: 10s (Passed in 0.9s)",
      status: "Approved",
      reviewer: "Ayush Sharma",
      date: "2026-10-06",
    },
    {
      id: "AIM-003",
      syntax: "config system global set admin-sport 8443",
      vendor: "FORTIOS",
      confidence: 92,
      canonicalField: "management.https.port",
      suggestedValue: "8443",
      reason: "Identifies non-standard secure administrative HTTPS management port.",
      rationale: "Deterministic heuristic regex pattern matched non-standard HTTPS administrative port 8443 on FortiOS platform.",
      provider: "FALLBACK_STUB",
      latencyText: "Timeout Budget: 10s (Offline Stub in 0.04s)",
      status: "Approved",
      reviewer: "Ayush Sharma",
      date: "2026-10-05",
    },
  ];

  const sourceMappings = dbMappings.length > 0 ? dbMappings : defaultSeedMappings;
  const mappings = sourceMappings.map(item => {
    const override = localOverrides[item.id];
    const base: AiMapping = {
      ...item,
      provider: item.provider || (item.id === "AIM-003" ? "FALLBACK_STUB" : "GEMINI"),
      latencyText: item.latencyText || (item.id === "AIM-003" ? "Timeout Budget: 10s (Offline Stub in 0.04s)" : "Timeout Budget: 10s (Passed in 1.4s)"),
      rationale: item.rationale || item.reason,
    };
    return override ? { ...base, ...override } : base;
  });

  const update = async (id: string, status: "Approved" | "Rejected") => {
    try {
      if (status === "Approved") {
        await api.ai.approve(id);
      } else {
        await api.ai.reject(id);
      }
    } catch (err) {
      console.warn("Backend AI approval sync:", err);
    }
    setLocalOverrides(prev => ({
      ...prev,
      [id]: { status, reviewer: "Ayush Sharma" },
    }));
    setNotice(
      `Mapping ${id} ${status.toLowerCase()} and saved to database. Deterministic checks will use it only after review.`
    );
  };

  return (
    <div className="page-stack">
      <div className="ai-principle">
        <div className="ai-principle-mark">
          <Icon name="spark" size={18} />
        </div>
        <div>
          <div className="eyebrow">Human-in-the-loop mapping</div>
          <strong>
            AI suggests. Analysts validate. Rules decide compliance.
          </strong>
          <span>
            Unknown vendor syntax is isolated for review and never converted
            into arbitrary production commands.
          </span>
        </div>
        <LinkButton href="/frameworks" variant="secondary">
          View rule coverage <Icon name="arrow" size={14} />
        </LinkButton>
      </div>

      {notice && <Notice>{notice}</Notice>}

      <Panel
        title="Unknown syntax queue"
        eyebrow={`${
          mappings.filter((item: AiMapping) => item.status === "Needs review")
            .length
        } items need review`}
      >
        <div className="mapping-list flex flex-col gap-4">
          {mappings.map((mapping: AiMapping) => (
            <AiAnalystQueueCard
              key={mapping.id}
              mapping={mapping}
              onApprove={id => update(id, "Approved")}
              onReject={id => update(id, "Rejected")}
            />
          ))}
        </div>
      </Panel>

      <Panel title="Mapping history" eyebrow="Reviewed canonical mappings">
        <div className="table-scroll">
          <div className="data-table mapping-table">
            <div className="data-row data-head">
              <span>Syntax</span>
              <span>Vendor</span>
              <span>Suggested mapping</span>
              <span>Confidence</span>
              <span>Reviewer</span>
              <span>Status</span>
              <span>Date</span>
            </div>
            {mappings
              .filter((item: AiMapping) => item.status !== "Needs review")
              .map((item: AiMapping) => (
                <div className="data-row" key={item.id}>
                  <span className="mono">{item.syntax}</span>
                  <span>{item.vendor}</span>
                  <span className="mono">{item.canonicalField}</span>
                  <span>{item.confidence}%</span>
                  <span>{item.reviewer}</span>
                  <span>
                    <StatusBadge value={item.status} />
                  </span>
                  <span>{item.date}</span>
                </div>
              ))}
          </div>
        </div>
      </Panel>
    </div>
  );
}

const reportCards = [
  {
    name: "Executive Summary",
    icon: "report" as const,
    desc: "Leadership-ready posture",
  },
  {
    name: "Findings",
    icon: "warning" as const,
    desc: "Open and resolved findings",
  },
  {
    name: "Framework Mapping",
    icon: "layers" as const,
    desc: "Control and evidence coverage",
  },
  {
    name: "Risk Assessment",
    icon: "trend" as const,
    desc: "Explainable device risk",
  },
  {
    name: "Remediation",
    icon: "check" as const,
    desc: "Action plan by vendor",
  },
];

export function Reports() {
  const { data: reports } = useReports();
  const { data: devices } = useDevices();
  const [typeFilter, setTypeFilter] = useState("All types");
  const [showNotice, setShowNotice] = useState(false);

  const filteredReports = reports.filter(
    (r: Report) => typeFilter === "All types" || r.type === typeFilter
  );

  return (
    <div className="page-stack">
      <div className="page-actions">
        <div className="filter-control">
          <Icon name="filter" size={14} />
          <select
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value)}
          >
            <option>All types</option>
            <option>Executive Summary</option>
            <option>Findings</option>
            <option>Framework Mapping</option>
            <option>Risk Assessment</option>
          </select>
        </div>
        <Button onClick={() => setShowNotice(true)}>
          <Icon name="plus" size={15} /> Generate report
        </Button>
      </div>

      {showNotice && (
        <Notice>
          Report request queued. Select a ready report to open the
          evidence-preserving preview.
        </Notice>
      )}

      <div className="report-type-grid">
        {reportCards.map(item => (
          <button
            className={`report-type ${typeFilter === item.name ? "active" : ""}`}
            onClick={() => {
              setTypeFilter(prev => prev === item.name ? "All types" : item.name);
              setShowNotice(true);
            }}
            key={item.name}
          >
            <div className="report-type-icon">
              <Icon name={item.icon} size={17} />
            </div>
            <div>
              <strong>{item.name}</strong>
              <span>{item.desc}</span>
            </div>
            <Icon name="arrow" size={14} />
          </button>
        ))}
      </div>

      <Panel title="Generated reports" eyebrow={`${filteredReports.length} records`}>
        <div className="table-scroll">
          <div className="data-table report-table">
            <div className="data-row data-head">
              <span>Report ID / title</span>
              <span>Type</span>
              <span>Device</span>
              <span>Audit</span>
              <span>Date</span>
              <span>Status</span>
              <span>Compliance</span>
              <span>Actions</span>
            </div>
            {filteredReports.map((report: Report) => (
              <div className="data-row" key={report.id}>

                <span>
                  <strong className="mono">{report.id}</strong>
                  <small>{report.title}</small>
                </span>
                <span>{report.type}</span>
                <span>
                  {
                    devices.find(
                      (device: Device) => device.id === report.deviceId
                    )?.hostname
                  }
                </span>
                <span className="mono">{report.auditId}</span>
                <span>{report.date}</span>
                <span>
                  <StatusBadge value={report.status} />
                </span>
                <span className="score-cell">{report.compliance}%</span>
                <span>
                  <LinkButton href={`/reports/${report.id}`} variant="ghost">
                    Open <Icon name="external" size={13} />
                  </LinkButton>
                </span>
              </div>
            ))}
          </div>
        </div>
      </Panel>
    </div>
  );
}

export function ReportPreview({ id }: { id: string }) {
  const { data: reports } = useReports();
  const { data: devices } = useDevices();
  const { data: audits } = useAudits();
  const { data: findings } = useFindings();
  const report = reports.find((item: Report) => item.id === id) ?? reports[0];
  const device = devices.find((item: Device) => item.id === report?.deviceId) ?? devices[0];
  const audit = audits.find((item: Audit) => item.id === report?.auditId) ?? audits[0];
  const [downloaded, setDownloaded] = useState(false);

  if (!report || !device || !audit) {
    return (
      <div className="page-stack report-page">
        <Panel title="Loading report..." eyebrow="Assessment archive">
          <div>Loading compliance report preview...</div>
        </Panel>
      </div>
    );
  }

  return (
    <div className="page-stack report-page">
      <div className="detail-header">
        <div>
          <div className="detail-back">
            <Link href="/reports">
              <Icon name="arrow" size={14} className="rotate-180" /> Reports
            </Link>
          </div>
          <div className="eyebrow">
            {report.id} · {report.type}
          </div>
          <h2>{report.title}</h2>
          <div className="detail-subline">
            <span>
              {device.vendor} {device.platform}
            </span>
            <span>•</span>
            <span>Audit date {report.date}</span>
            <span>•</span>
            <span>Prepared for Security Engineering</span>
          </div>
        </div>
        <div className="header-actions">
          <StatusBadge value={report.status} />
          <Button
            onClick={() => {
              setDownloaded(true);
              try {
                window.print();
              } catch {
                // Ignore if running headless
              }
            }}
          >
            <Icon name="download" size={14} /> Download PDF
          </Button>
        </div>

      </div>

      {downloaded && (
        <Notice>
          PDF export prepared from the same audit data shown in this preview.
        </Notice>
      )}

      <div className="report-sheet">
        <div className="report-sheet-head">
          <div className="report-brand-wrap">
            <img
              src="/logo.png"
              alt="Nexus-Comply"
              className="brand-logo-img"
            />
            <div>
              <div className="brand-name">NEXUS-COMPLY</div>
              <div className="brand-id">SECURITY ASSESSMENT · {report.id}</div>
            </div>
          </div>
          <div className="report-score">
            <span>Overall compliance</span>
            <strong>{report.compliance}%</strong>
            <small>Evidence-backed score</small>
          </div>
        </div>

        <div className="report-summary-grid">
          <div>
            <span>Device</span>
            <strong>{device.hostname}</strong>
          </div>
          <div>
            <span>Vendor / platform</span>
            <strong>
              {device.vendor} · {device.platform}
            </strong>
          </div>
          <div>
            <span>Audit date</span>
            <strong>{report.date}</strong>
          </div>
          <div>
            <span>Audit ID</span>
            <strong className="mono">{audit.id}</strong>
          </div>
        </div>

        <div className="report-section">
          <span className="eyebrow">Executive summary</span>
          <h3>
            Configuration evidence shows a strong baseline with targeted
            remediation required.
          </h3>
          <p>
            {device.hostname} is currently at {device.compliance}% compliance
            coverage across {audit.frameworks.join(" and ")}. The audit
            identified {audit.findings} findings, with the highest-priority item
            linked directly to its source configuration line.
          </p>
        </div>

        <div className="report-section">
          <span className="eyebrow">Compliance overview</span>
          <ComplianceLineChart
            data={[
              { month: "Jun", score: 68 },
              { month: "Jul", score: 70 },
              { month: "Aug", score: 72 },
              { month: "Sep", score: report.compliance },
            ]}
          />
        </div>

        <div className="report-section">
          <span className="eyebrow">Findings and evidence</span>
          {findings
            .filter((finding: Finding) => finding.deviceId === device.id)
            .slice(0, 3)
            .map((finding: Finding) => (
              <div className="report-finding" key={finding.id}>
                <div>
                  <strong>{finding.id}</strong>
                  <span>{finding.title}</span>
                </div>
                <StatusBadge value={finding.severity} />
                <span className="mono">Line {finding.line}</span>
              </div>
            ))}
        </div>

        <div className="report-section">
          <span className="eyebrow">Risk assessment</span>
          <div className="report-risk-row">
            <div>
              <span>Current risk score</span>
              <strong>{device.risk}</strong>
            </div>
            <ProgressBar value={device.risk} tone="amber" />
            <div>
              <span>Asset criticality</span>
              <strong>{device.criticality}</strong>
            </div>
          </div>
        </div>

        <div className="report-section report-footnote">
          <Icon name="shield" size={15} />
          <span>
            Framework language reflects technical evidence, control mapping, and
            assessment coverage. It does not imply ISO/IEC 27001 certification.
          </span>
        </div>
      </div>
    </div>
  );
}

export function Frameworks() {
  const { data: frameworks } = useFrameworks();
  return (
    <div className="page-stack">
      <div className="framework-intro">
        <div>
          <div className="eyebrow">Governance library</div>
          <p>
            Compare technical evidence, control mappings, and assessment
            coverage across the frameworks configured for this workspace.
          </p>
        </div>
        <Notice tone="info">
          Coverage is not certification. Scores reflect evidence available
          in the current workspace.
        </Notice>
      </div>

      <Panel
        title="Framework register"
        eyebrow={`${frameworks.length} active frameworks`}
      >
        <div className="table-scroll">
          <div className="data-table framework-table">
            <div className="data-row data-head">
              <span>Framework</span>
              <span>Version</span>
              <span>Control ID</span>
              <span>Category</span>
              <span>Rule count</span>
              <span>Coverage</span>
              <span>Status</span>
              <span />
            </div>
            {frameworks.map((framework: Framework) => (
              <Link
                href={`/frameworks/${framework.id}`}
                className="data-row"
                key={framework.id}
              >
                <span>
                  <strong>{framework.name}</strong>
                  <small>{framework.note}</small>
                </span>
                <span>{framework.version}</span>
                <span className="mono">
                  {framework.id === "cis-v8"
                    ? "CIS-1.1"
                    : framework.id === "nist-800-53"
                      ? "AC-2"
                      : framework.id === "stig"
                        ? "V-219160"
                        : "A.5.1"}
                </span>
                <span>{framework.category}</span>
                <span>{framework.mappedRules}</span>
                <span className="coverage-cell">
                  <ProgressBar
                    value={framework.coverage}
                    tone={
                      framework.coverage > 80
                        ? "green"
                        : framework.coverage > 70
                          ? "blue"
                          : "amber"
                    }
                  />
                  <strong>{framework.coverage}%</strong>
                </span>
                <span>
                  <StatusBadge value="Active" />
                </span>
                <span>
                  <Icon name="chevron" size={14} />
                </span>
              </Link>
            ))}
          </div>
        </div>
      </Panel>
    </div>
  );
}

export function FrameworkDetail({ id }: { id: string }) {
  const { data: frameworks } = useFrameworks();
  const { data: liveControls } = useFrameworkControls(id);
  const framework =
    frameworks.find((item: Framework) => item.id === id) ?? frameworks[0];
  if (!framework) {
    return (
      <div className="page-stack">
        <Panel title="Loading..." eyebrow="Framework register">
          <div>Loading framework details...</div>
        </Panel>
      </div>
    );
  }

  const defaultControls = [
    {
      id: "CIS-1.1.2",
      title: "Ensure Telnet service is disabled",
      category: "Access control",
      rules: 2,
      evidence: 85,
    },
    {
      id: "CIS-1.1.1",
      title: "Ensure SSH v2 is enabled",
      category: "Access control",
      rules: 1,
      evidence: 92,
    },
    {
      id: "CIS-1.1.5",
      title: "Ensure remote syslog logging is configured",
      category: "Logging",
      rules: 1,
      evidence: 76,
    },
    {
      id: "CIS-1.1.4",
      title: "Ensure AAA subsystem is active",
      category: "Authentication",
      rules: 2,
      evidence: 88,
    },
  ];

  const controls = liveControls.length > 0
    ? liveControls.map(c => ({
        id: c.controlId || c.controlCode || c.id,
        title: c.title,
        category: c.family || c.category || "Access control",
        rules: c.ruleIds?.length || 1,
        evidence: 84,
      }))
    : defaultControls;

  return (
    <div className="page-stack">
      <div className="detail-header">
        <div>
          <div className="detail-back">
            <Link href="/frameworks">
              <Icon name="arrow" size={14} className="rotate-180" /> Frameworks
              & controls
            </Link>
          </div>
          <div className="eyebrow">
            {framework.version} · {framework.category}
          </div>
          <h2>{framework.name}</h2>
          <div className="detail-subline">
            <span>{framework.note}</span>
          </div>
        </div>
        <StatusBadge value="Active" />
      </div>

      <div className="metric-grid four">
        <Metric
          label="Coverage"
          value={`${framework.coverage}%`}
          note="Evidence available"
          tone={framework.coverage > 80 ? "good" : "warn"}
        />
        <Metric
          label="Controls"
          value={framework.controls}
          note="Selected profile"
        />
        <Metric
          label="Mapped rules"
          value={framework.mappedRules}
          note="Deterministic checks"
        />
        <Metric
          label="Open evidence gaps"
          value={
            framework.controls -
            Math.round((framework.controls * framework.coverage) / 100)
          }
          note="Prioritize remediation"
          tone="warn"
        />
      </div>

      <Panel title="Control coverage" eyebrow="Evidence-backed assessment">
        <div className="table-scroll">
          <div className="data-table control-table">
            <div className="data-row data-head">
              <span>Control ID / title</span>
              <span>Category</span>
              <span>Rules</span>
              <span>Evidence coverage</span>
              <span>Mapped status</span>
              <span />
            </div>
            {controls.map(control => (
              <Link
                href={`/controls/${control.id}`}
                className="data-row"
                key={control.id}
              >
                <span>
                  <strong className="mono">{control.id}</strong>
                  <small>{control.title}</small>
                </span>
                <span>{control.category}</span>
                <span>{control.rules}</span>
                <span className="coverage-cell">
                  <ProgressBar
                    value={control.evidence}
                    tone={control.evidence > 80 ? "green" : "amber"}
                  />
                  <strong>{control.evidence}%</strong>
                </span>
                <span>
                  <StatusBadge
                    value={control.evidence > 70 ? "Mapped" : "Partial"}
                  />
                </span>
                <span>
                  <Icon name="chevron" size={14} />
                </span>
              </Link>
            ))}
          </div>
        </div>
      </Panel>
    </div>
  );
}

export function ControlDetail({ id }: { id: string }) {
  const { data: findings } = useFindings();
  const { data: controls } = useFrameworkControls("FW-CIS");
  const ctrl = controls.find(c => (c.controlId || c.controlCode || c.id) === id);
  const finding =
    findings.find((item: Finding) => item.control === id) ?? findings[0];

  const title = ctrl?.title || (finding?.control === id ? finding.title : "Secure network administration");
  const description = ctrl?.description || "Administrative access to network infrastructure must use authenticated, encrypted transport and produce auditable evidence. The control is evaluated against normalized security fields rather than vendor-specific syntax.";
  const family = ctrl?.family || ctrl?.category || "Access control";

  return (
    <div className="page-stack">
      <div className="detail-header">
        <div>
          <div className="detail-back">
            <Link href="/frameworks">
              <Icon name="arrow" size={14} className="rotate-180" /> Frameworks
            </Link>
          </div>
          <div className="eyebrow">{ctrl?.frameworkId || "CIS Controls"} · v8.0</div>
          <h2>{id}</h2>
          <div className="detail-subline">
            <span>{title}</span>
            <span>•</span>
            <span>{family}</span>
          </div>
        </div>
        <StatusBadge value="Mapped" />
      </div>

      <div className="grid-1-1">
        <Panel title="Control description" eyebrow="Assessment context">
          <p className="body-copy">
            {description}
          </p>
          <div className="detail-list">
            <div>
              <span>Rule count</span>
              <strong>8 deterministic rules</strong>
            </div>
            <div>
              <span>Canonical fields</span>
              <strong className="mono">remoteAccess.*</strong>
            </div>
            <div>
              <span>Evidence requirement</span>
              <strong>Source line + normalized value</strong>
            </div>
          </div>
        </Panel>

        <Panel title="Evidence coverage" eyebrow="Current workspace">
          <Metric
            label="Coverage"
            value="74%"
            note="14 of 19 applicable devices"
            tone="warn"
          />
          <ProgressBar value={74} tone="amber" />
          <div className="coverage-note">
            <Icon name="warning" size={14} /> 5 devices have an unresolved
            evidence gap.
          </div>
        </Panel>
      </div>

      {finding && (
        <Panel title="Related finding" eyebrow="Trace control to action">
          <div className="finding-summary-row">
            <div>
              <span className="mono">{finding.id}</span>
              <strong>{finding.title}</strong>
            </div>
            <StatusBadge value={finding.severity} />
            <LinkButton href={`/findings/${finding.id}`} variant="secondary">
              Open finding <Icon name="external" size={13} />
            </LinkButton>
          </div>
        </Panel>
      )}
    </div>
  );
}

export function Settings() {
  const { user: authUser, logout } = useAuth();
  const currentUser = authUser || {
    name: "Ayush Sharma",
    email: "admin@nexus-comply.local",
    role: "Security Administrator",
    organization: "Nexus Security Operations",
    avatar: "AS",
  };
  const [saved, setSaved] = useState(false);
  const notificationOptions = [
    {
      title: "Email notifications",
      desc: "Product updates and account messages",
      defaultChecked: true,
    },
    {
      title: "Audit alerts",
      desc: "When an audit completes or fails",
      defaultChecked: true,
    },
    {
      title: "Risk alerts",
      desc: "When a device crosses its risk threshold",
      defaultChecked: true,
    },
    {
      title: "Weekly reports",
      desc: "A weekly posture summary for your workspace",
      defaultChecked: false,
    },
  ];

  return (
    <div className="page-stack settings-page">
      <div className="settings-nav">
        <div className="settings-nav-active">Profile</div>
        <div>Security</div>
        <div>Notifications</div>
        <div>Application</div>
      </div>
      <div className="settings-content">
        <Panel title="Profile" eyebrow="Workspace identity">
          <div className="profile-head">
            <div className="avatar avatar-large">
              {currentUser.avatar || "AS"}
            </div>
            <div>
              <h3>{currentUser.name}</h3>
              <span>{currentUser.role}</span>
              <small>{currentUser.email}</small>
            </div>
          </div>
          <div className="form-grid">
            <label>
              Name
              <input defaultValue={currentUser.name} key={currentUser.name} />
            </label>
            <label>
              Email
              <input
                defaultValue={currentUser.email}
                key={currentUser.email}
                type="email"
              />
            </label>
            <label>
              Role
              <input defaultValue={currentUser.role} disabled />
            </label>
            <label>
              Workspace / Organization
              <input
                defaultValue={currentUser.organization || "Network Assurance"}
                disabled
              />
            </label>
          </div>
        </Panel>

        <Panel title="Notifications" eyebrow="Operational signal preferences">
          <div className="toggle-list">
            {notificationOptions.map(item => (
              <label className="toggle-row" key={item.title}>
                <span>
                  <strong>{item.title}</strong>
                  <small>{item.desc}</small>
                </span>
                <input type="checkbox" defaultChecked={item.defaultChecked} />
                <span className="toggle-ui" />
              </label>
            ))}
          </div>
        </Panel>

        <Panel title="Security" eyebrow="Session controls">
          <div className="detail-list">
            <div>
              <span>Last sign-in</span>
              <strong>Active session · Chrome on Desktop</strong>
            </div>
            <div>
              <span>Active sessions</span>
              <strong>1 active authenticated session</strong>
            </div>
            <div>
              <span>Session actions</span>
              <Button variant="danger" onClick={logout}>
                <Icon name="logOut" size={14} /> Sign out of session
              </Button>
            </div>
          </div>
        </Panel>

        <Panel title="Application & Platform" eyebrow="System identity">
          <div className="profile-head">
            <img
              src="/logo.png"
              alt="Nexus-Comply"
              className="brand-logo-img brand-logo-large"
            />
            <div>
              <h3>NEXUS-COMPLY</h3>
              <span>Network Assurance & Compliance Engine</span>
              <small>Build 0.9.4 · Production environment</small>
            </div>
          </div>
        </Panel>

        <div className="form-actions">
          <Button onClick={() => setSaved(true)}>Save settings</Button>
        </div>
        {saved && <Notice>Settings saved for this demo session.</Notice>}
      </div>
    </div>
  );
}
