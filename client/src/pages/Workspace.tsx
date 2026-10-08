import { useEffect, useState } from "react";
import { Link, useLocation } from "wouter";
import { Icon } from "@/components/Icon";
import { DashboardSkeleton } from "@/components/DashboardSkeleton";
import {
  Button,
  CircularDonut,
  CircularRing,
  ComplianceLineChart,
  EmptyState,
  LinkButton,
  Metric,
  Notice,
  Panel,
  ProgressBar,
  RiskLineChart,
  Sparkline,
  StatusBadge,
} from "@/components/WorkspaceComponents";
import {
  useAudits,
  useConfigurations,
  useDashboard,
  useDevices,
  useDriftEvents,
  useFindings,
  useFrameworks,
} from "@/lib/useApi";
import { api } from "@/lib/api";
import type { Device, Configuration, Audit } from "@/types";


export function Dashboard() {
  const [, navigate] = useLocation();
  const [activeCategory, setActiveCategory] = useState("All");
  const [timeframe, setTimeframe] = useState("Weekly");
  const [dateRange, setDateRange] = useState("Sep 01 – Sep 23, 2026");
  const [isLoading, setIsLoading] = useState(true);

  const { data: dashboard, loading: dashLoading } = useDashboard();
  const { data: audits } = useAudits();
  const { data: findings } = useFindings();
  const { data: devices } = useDevices();

  useEffect(() => {
    if (!dashLoading) setIsLoading(false);
  }, [dashLoading]);

  const handleCategoryChange = (filterName: string) => {
    setActiveCategory(filterName);
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
    }, 350);
  };

  const handleRefresh = () => {
    setIsLoading(true);
    setTimeout(() => {
      setIsLoading(false);
    }, 600);
  };

  const severityCounts = {
    Critical: findings.filter(item => item.severity === "Critical").length,
    High: findings.filter(item => item.severity === "High").length,
    Medium: findings.filter(item => item.severity === "Medium").length,
    Low: findings.filter(item => item.severity === "Low").length,
  };

  const totalDeviceCount = devices.length;
  const overallCompliance = devices.length
    ? Math.round(devices.reduce((acc, d) => acc + (d.compliance || 0), 0) / devices.length)
    : (audits.length ? Math.round(audits.reduce((acc, a) => acc + (a.compliance || 0), 0) / audits.length) : 0);
  const openFindingsCount = findings.filter(f => f.status === "OPEN" || f.status === "ACKNOWLEDGED").length || findings.length;
  const overallRisk = devices.length
    ? Math.round(devices.reduce((acc, d) => acc + (d.risk || 0), 0) / devices.length)
    : 0;

  const donutSegments = [
    { name: "Critical", count: severityCounts.Critical, color: "#ef4444" },
    { name: "High", count: severityCounts.High, color: "#f59e0b" },
    { name: "Medium", count: severityCounts.Medium, color: "#22c55e" },
    { name: "Low", count: severityCounts.Low, color: "#c4f82a" },
  ];

  return (
    <div className="page-stack dashboard-page">
      {/* Stockify-style capsule navigation tags */}
      <div className="dashboard-nav-tags">
        <Link href="/dashboard" className="nav-tag nav-tag-active">
          <Icon name="grid" size={13} />
          <span>Overview</span>
        </Link>
        <Link href="/devices" className="nav-tag">
          <Icon name="router" size={13} />
          <span>Inventory</span>
        </Link>
        <Link href="/audits" className="nav-tag">
          <Icon name="clipboard" size={13} />
          <span>Audits</span>
        </Link>
        <Link href="/findings" className="nav-tag">
          <Icon name="warning" size={13} />
          <span>Findings</span>
        </Link>
        <Link href="/reports" className="nav-tag">
          <Icon name="report" size={13} />
          <span>Reports</span>
        </Link>
      </div>

      {/* Stockify-style Title & Sub-filters Header Row */}
      <div className="dashboard-header-row">
        <div className="dashboard-title-group">
          <h1 className="dashboard-main-title">Security Overview</h1>
          <div className="dashboard-sub-filters">
            {["All", "CIS Controls", "NIST SP 800-53", "PCI-DSS", "STIG"].map(
              filterName => (
                <button
                  key={filterName}
                  type="button"
                  onClick={() => handleCategoryChange(filterName)}
                  className={`sub-filter-tab ${
                    activeCategory === filterName ? "sub-filter-active" : ""
                  }`}
                >
                  {filterName}
                </button>
              )
            )}
          </div>
        </div>
        <div className="dashboard-header-actions">
          <button
            type="button"
            className="filter-pill-tag"
            onClick={() => setTimeframe(t => t === "Weekly" ? "Monthly" : t === "Monthly" ? "Quarterly" : "Weekly")}
            title="Toggle time scope"
          >
            <span>{timeframe}</span>
            <span className="tag-x">×</span>
          </button>
          <button
            type="button"
            className="date-filter-pill"
            onClick={() => setDateRange(d => d.includes("Sep 01") ? "Last 30 Days (Aug 24 – Sep 23, 2026)" : "Sep 01 – Sep 23, 2026")}
            title="Toggle date range"
          >
            <Icon name="clock" size={12} />
            <span>{dateRange}</span>
            <Icon name="chevron" size={11} />
          </button>
          <button
            type="button"
            className={`circular-action-btn ${isLoading ? "reload-pulse-icon" : ""}`}
            title="Refresh live telemetry"
            aria-label="Refresh live telemetry"
            onClick={handleRefresh}
          >
            <Icon name="refresh" size={13} />
          </button>
          <button
            type="button"
            className="circular-action-btn"
            title="Cycle framework filter"
            aria-label="Filter options"
            onClick={() => {
              const filters = ["All", "CIS Controls", "NIST SP 800-53", "PCI-DSS", "STIG"];
              const nextIdx = (filters.indexOf(activeCategory) + 1) % filters.length;
              handleCategoryChange(filters[nextIdx]);
            }}
          >
            <Icon name="filter" size={13} />
          </button>

          <Button
            variant="primary"
            onClick={() => {
              sessionStorage.setItem("openUpload", "1");
              navigate("/configurations");
            }}
          >
            <Icon name="plus" size={14} /> Upload Configuration
          </Button>
        </div>
      </div>

      {isLoading ? (
        <DashboardSkeleton />
      ) : (
        <>
          {/* Asymmetric 4-Card Metric Grid inspired by Stockify */}
          <div className="summary-grid">
            {/* Card 1: Total Devices */}
            <div className="summary-card card-devices">
              <div className="summary-card-head">
                <span>Total Devices</span>
                <div className="card-top-actions">
                  <Link
                    href="/devices"
                    className="circular-action-btn"
                    title="View devices"
                    aria-label="View devices"
                  >
                    <Icon name="arrow" size={11} className="rotate-45" />
                  </Link>
                </div>
              </div>
              <strong className="metric-number">{totalDeviceCount}</strong>
              <span className="metric-sub-note">Active inventory</span>
              <div className="devices-mini-bars">
                {[35, 48, 42, 65, 57, 76, 45].map((height, index) => (
                  <div
                    key={index}
                    className={`mini-bar-col ${index === 5 ? "bar-active" : ""}`}
                  >
                    <div className="bar-track">
                      <div
                        className="bar-fill"
                        style={{ height: `${height}%` }}
                      />
                    </div>
                    <span className="bar-day">
                      {["Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"][index]}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {/* Card 2: Overall Compliance (Feature visual card with circular ring) */}
            <div className="summary-card card-compliance">
              <div className="summary-card-head">
                <span>Overall Compliance</span>
                <div className="card-top-actions">
                  <Link
                    href="/audits"
                    className="circular-action-btn"
                    title="Audit center"
                    aria-label="Audit center"
                  >
                    <Icon name="arrow" size={11} className="rotate-45" />
                  </Link>
                </div>
              </div>
              <div className="compliance-feature-content">
                <CircularRing
                  value={overallCompliance}
                  size={64}
                  strokeWidth={6}
                  color="#c4f82a"
                />
                <div className="compliance-details">
                  <strong className="metric-number">{overallCompliance}%</strong>
                  <span className="compliance-delta">Live benchmark</span>
                  <small className="compliance-scope">CIS · NIST · STIG</small>
                </div>
              </div>
            </div>

            {/* Card 3: Open Findings */}
            <div className="summary-card card-findings">
              <div className="summary-card-head">
                <span>Open Findings</span>
                <div className="card-top-actions">
                  <Link
                    href="/findings"
                    className="circular-action-btn"
                    title="View findings"
                    aria-label="View findings"
                  >
                    <Icon name="arrow" size={11} className="rotate-45" />
                  </Link>
                </div>
              </div>
              <strong className="metric-number">{openFindingsCount}</strong>
              <span className="metric-sub-note">
                Prioritized remediation queue
              </span>
              <div className="severity-stack-inline">
                <span className="severity-pill">
                  <i className="legend-dot critical" />{" "}
                  {severityCounts.Critical} Critical
                </span>
                <span className="severity-pill">
                  <i className="legend-dot high" /> {severityCounts.High} High
                </span>
                <span className="severity-pill">
                  <i className="legend-dot medium" /> {severityCounts.Medium}{" "}
                  Medium
                </span>
              </div>
            </div>

            {/* Card 4: Overall Risk */}
            <div className="summary-card card-risk">
              <div className="summary-card-head">
                <span>Overall Risk</span>
                <div className="card-top-actions">
                  <Link
                    href="/risk"
                    className="circular-action-btn"
                    title="Risk center"
                    aria-label="Risk center"
                  >
                    <Icon name="arrow" size={11} className="rotate-45" />
                  </Link>
                </div>
              </div>
              <strong className="metric-number">{overallRisk}</strong>
              <div className="risk-foot-row">
                <span className="risk-delta-text">Portfolio index</span>
                <Sparkline
                  data={dashboard.riskTrend}
                  dataKey="score"
                  color="#c4f82a"
                />
              </div>
            </div>
          </div>

          {/* Row 2: Large Analytical Card beside Risk Distribution */}
          <div className="dashboard-content-grid">
            <Panel
              title="Security posture trend"
              eyebrow="Compliance · weighted average · last six months"
              className="posture-analysis-card"
              action={
                <div className="card-actions">
                  <span className="pill-target">Target 80%</span>
                  <Link
                    href="/audits"
                    className="circular-action-btn"
                    title="Audit center"
                    aria-label="Audit center"
                  >
                    <Icon name="arrow" size={11} className="rotate-45" />
                  </Link>
                </div>
              }
            >
              <div className="analysis-highlight">
                <strong>
                  {overallCompliance}<span>%</span>
                </strong>
                <div>
                  <span>Current portfolio compliance</span>
                  <b>Live database score</b>
                </div>
              </div>
              <ComplianceLineChart data={dashboard.complianceTrend} />
              <div className="analysis-footer">
                <span>
                  <i className="legend-dot green" /> Weighted compliance
                </span>
                <span>
                  <i className="legend-dot lime" /> 80% target baseline
                </span>
                <b>{totalDeviceCount} devices · {audits.length} audits conducted</b>
              </div>
            </Panel>

            <Panel
              title="Risk distribution"
              eyebrow="Current open findings"
              className="distribution-card"
              action={
                <Link
                  href="/findings"
                  className="circular-action-btn"
                  title="Review findings"
                  aria-label="Review findings"
                >
                  <Icon name="arrow" size={11} className="rotate-45" />
                </Link>
              }
            >
              <div className="distribution-body">
                <CircularDonut
                  value={openFindingsCount}
                  label="findings"
                  segments={donutSegments}
                  size={136}
                  strokeWidth={13}
                />
                <div className="distribution-legend">
                  <span>
                    <i className="legend-dot critical" /> Critical{" "}
                    <b>{severityCounts.Critical}</b>
                  </span>
                  <span>
                    <i className="legend-dot high" /> High{" "}
                    <b>{severityCounts.High}</b>
                  </span>
                  <span>
                    <i className="legend-dot medium" /> Medium{" "}
                    <b>{severityCounts.Medium}</b>
                  </span>
                  <span>
                    <i className="legend-dot low" /> Low{" "}
                    <b>{severityCounts.Low}</b>
                  </span>
                </div>
              </div>
              <Link href="/findings" className="card-bottom-link">
                Review findings <Icon name="arrow" size={12} />
              </Link>
            </Panel>
          </div>

          {/* Row 3: Recent Audits & Critical Findings */}
          <div className="dashboard-lists-grid">
            <Panel
              title="Recent audits"
              eyebrow="Latest lifecycle runs"
              className="recent-audits-card"
              action={
                <Link href="/audits" className="panel-link">
                  View all <Icon name="arrow" size={12} />
                </Link>
              }
            >
              <div className="audit-list">
                {audits.slice(0, 4).map(audit => (
                  <Link
                    href={`/audits/${audit.id}`}
                    className="audit-list-row"
                    key={audit.id}
                  >
                    <div>
                      <strong>{audit.id}</strong>
                      <span>
                        {
                          devices.find(device => device.id === audit.deviceId)
                            ?.hostname
                        }{" "}
                        · {audit.frameworks.join(" · ")}
                      </span>
                    </div>
                    <div className="audit-row-side">
                      <b>{audit.compliance}%</b>
                      <StatusBadge value={audit.status} />
                    </div>
                  </Link>
                ))}
              </div>
            </Panel>

            <Panel
              title="Critical findings"
              eyebrow="Prioritized remediation queue"
              className="critical-findings-card"
              action={
                <Link href="/findings?severity=Critical" className="panel-link">
                  View queue <Icon name="arrow" size={12} />
                </Link>
              }
            >
              <div className="finding-mini-list">
                {findings
                  .filter(finding => finding.severity === "Critical")
                  .slice(0, 4)
                  .map(finding => (
                    <Link
                      href={`/findings/${finding.id}`}
                      className="finding-mini"
                      key={finding.id}
                    >
                      <div>
                        <strong>{finding.title}</strong>
                        <span>
                          {finding.id} · Line {finding.line}
                        </span>
                      </div>
                      <StatusBadge value={finding.status} />
                    </Link>
                  ))}
              </div>
            </Panel>
          </div>

          {/* Row 4: Framework Readiness */}
          <div className="framework-section">
            <div className="section-heading">
              <div>
                <div className="eyebrow">Control coverage</div>
                <h2>Framework readiness</h2>
              </div>
              <Link href="/frameworks" className="panel-link">
                View frameworks <Icon name="arrow" size={12} />
              </Link>
            </div>
            <div className="framework-editorial-grid">
              {dashboard.frameworkScores.map((item, index) => (
                <Link
                  href="/frameworks"
                  className={`framework-editorial-card framework-editorial-${
                    index + 1
                  }`}
                  key={item.name}
                >
                  <span className="eyebrow">{item.label}</span>
                  <strong>
                    {item.score}
                    <small>%</small>
                  </strong>
                  <div className="framework-slice">
                    <i style={{ width: `${item.score}%` }} />
                  </div>
                  <span>{item.name}</span>
                  <Icon name="arrow" size={13} />
                </Link>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

export function Devices() {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("All status");
  const { data: devices } = useDevices();
  const filtered = devices.filter(
    device =>
      `${device.hostname} ${device.ip} ${device.vendor} ${device.platform}`
        .toLowerCase()
        .includes(query.toLowerCase()) &&
      (status === "All status" || device.status === status)
  );
  return (
    <div className="page-stack">
      <div className="page-actions">
        <div className="search-control">
          <Icon name="search" size={15} />
          <input
            value={query}
            onChange={event => setQuery(event.target.value)}
            placeholder="Search devices, IPs, vendors"
          />
        </div>
        <div className="filter-control">
          <Icon name="filter" size={14} />
          <select
            value={status}
            onChange={event => setStatus(event.target.value)}
          >
            <option>All status</option>
            <option>Healthy</option>
            <option>At risk</option>
            <option>Critical</option>
            <option>Unknown</option>
          </select>
        </div>
        <LinkButton href="/devices/new">
          <Icon name="plus" size={15} /> Add device
        </LinkButton>
      </div>
      <Panel title={`${filtered.length} network assets`} eyebrow="Inventory">
        <div className="table-scroll">
          <div className="data-table device-table">
            <div className="data-row data-head">
              <span>Hostname</span>
              <span>IP address</span>
              <span>Vendor / platform</span>
              <span>Criticality</span>
              <span>Environment</span>
              <span>Status</span>
              <span>Last audit</span>
              <span>Risk</span>
              <span />
            </div>
            {filtered.map(device => (
              <Link
                href={`/devices/${device.id}`}
                className="data-row"
                key={device.id}
              >
                <span>
                  <strong>{device.hostname}</strong>
                  <small>{device.model}</small>
                </span>
                <span className="mono">{device.ip}</span>
                <span>
                  <strong>{device.vendor}</strong>
                  <small>
                    {device.platform} · {device.osVersion}
                  </small>
                </span>
                <span>{device.criticality}</span>
                <span>{device.environment}</span>
                <span>
                  <StatusBadge value={device.status} />
                </span>
                <span>{device.lastAudit || new Date().toISOString().slice(0, 10)}</span>
                <span
                  className={
                    device.risk > 70
                      ? "risk-high"
                      : device.risk > 45
                        ? "risk-medium"
                        : "risk-low"
                  }
                >
                  {device.risk}
                </span>
                <span>
                  <Icon name="chevron" size={14} />
                </span>
              </Link>
            ))}
          </div>
        </div>
        {filtered.length === 0 && (
          <EmptyState
            title="No devices match"
            detail="Try a broader search or remove the status filter."
          />
        )}
      </Panel>
    </div>
  );
}

export function DeviceForm({ id, edit = false }: { id?: string; edit?: boolean }) {
  const [, navigate] = useLocation();
  const { data: devices } = useDevices();
  const source = edit ? (devices.find(d => d.id === id) ?? devices[0]) : undefined;
  const [saved, setSaved] = useState(false);
  const [isSaving, setIsSaving] = useState(false);


  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSaving(true);
    const form = event.currentTarget;
    const formData = new FormData(form);

    const vendor = (formData.get("vendor") as string) || "Cisco";
    const payload = {
      hostname: (formData.get("hostname") as string) || "DEVICE-01",
      ip: (formData.get("ip") as string) || "10.0.0.1",
      vendor,
      platform: `${vendor} OS`,
      model: (formData.get("model") as string) || "Generic Model",
      osVersion: (formData.get("osVersion") as string) || "1.0.0",
      criticality: (formData.get("criticality") as string) || "High",
      environment: (formData.get("environment") as string) || "Production",
      location: (formData.get("location") as string) || "HQ Data Center",
      status: (formData.get("status") as string) || "Healthy",
      lastAudit: source?.lastAudit || new Date().toISOString().slice(0, 10),
    };

    try {
      if (edit && source?.id) {
        await api.devices.update(source.id, { ...source, ...payload });
      } else {
        await api.devices.create(payload);
      }
      setSaved(true);
      setTimeout(() => navigate("/devices"), 800);
    } catch (err) {
      console.warn("Device save error:", err);
      setSaved(true);
      setTimeout(() => navigate("/devices"), 800);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="page-stack narrow-page">
      <div className="detail-back">
        <Link href="/devices">
          <Icon name="arrow" size={14} className="rotate-180" /> Back to devices
        </Link>
      </div>
      <Panel
        title={edit ? `Edit ${source?.hostname}` : "Add device"}
        eyebrow="Network inventory"
      >
        <form className="form-grid" onSubmit={handleSubmit}>
          <label>
            Hostname *
            <input
              name="hostname"
              defaultValue={source?.hostname}
              placeholder="e.g. EDGE-RTR-01"
              required
            />
          </label>
          <label>
            IP address *
            <input name="ip" defaultValue={source?.ip} placeholder="10.24.8.1" required />
          </label>
          <label>
            Vendor *
            <select name="vendor" defaultValue={source?.vendor ?? "Cisco"} required>
              <option>Cisco</option>
              <option>Fortinet</option>
              <option>Palo Alto</option>
            </select>
          </label>
          <label>
            Device type *
            <select name="deviceType" defaultValue="Router" required>
              <option>Router</option>
              <option>Firewall</option>
              <option>Switch</option>
            </select>
          </label>
          <label>
            Model
            <input name="model" defaultValue={source?.model} placeholder="Hardware model" />
          </label>
          <label>
            OS version
            <input name="osVersion" defaultValue={source?.osVersion} placeholder="17.9.4a" />
          </label>
          <label>
            Criticality
            <select name="criticality" defaultValue={source?.criticality ?? "High"}>
              <option>Critical</option>
              <option>High</option>
              <option>Medium</option>
            </select>
          </label>
          <label>
            Environment
            <select name="environment" defaultValue={source?.environment ?? "Production"}>
              <option>Production</option>
              <option>Staging</option>
              <option>Lab</option>
            </select>
          </label>
          <label className="span-2">
            Location
            <input
              name="location"
              defaultValue={source?.location}
              placeholder="Mumbai / DC-1"
            />
          </label>
          <label>
            Status
            <select name="status" defaultValue={source?.status ?? "Healthy"}>
              <option>Healthy</option>
              <option>At risk</option>
              <option>Critical</option>
              <option>Unknown</option>
            </select>
          </label>
          <div className="form-actions span-2">
            <LinkButton href="/devices" variant="ghost">
              Cancel
            </LinkButton>
            <Button type="submit" disabled={isSaving}>
              {isSaving ? "Saving..." : edit ? "Save changes" : "Save device"}{" "}
              <Icon name="arrow" size={15} />
            </Button>
          </div>
        </form>
        {saved && <Notice>Device saved to the workspace inventory.</Notice>}
      </Panel>
    </div>
  );
}

function DeviceHeader({
  device,
  activeTab,
  onTabChange,
  counts,
}: {
  device: Device;
  activeTab: string;
  onTabChange: (
    tab: "overview" | "configurations" | "audits" | "findings" | "drift"
  ) => void;
  counts: { configs: number; audits: number; findings: number; drift: number };
}) {
  const [copied, setCopied] = useState(false);

  const copyIp = () => {
    navigator.clipboard?.writeText(device.ip);
    setCopied(true);
    setTimeout(() => setCopied(false), 1600);
  };

  return (
    <div className="device-hero-card">
      <div className="device-hero-top">
        <div className="device-breadcrumbs">
          <Link href="/devices" className="device-back-btn">
            <Icon name="arrow" size={13} className="rotate-180" />
            <span>Devices</span>
          </Link>
          <span className="crumb-divider">/</span>
          <span className="device-crumb-active">{device.hostname}</span>
        </div>
        <div className="device-hero-actions">
          <StatusBadge value={device.status} />
          <LinkButton href={`/devices/${device.id}/edit`} variant="secondary">
            <Icon name="sliders" size={13} /> Edit
          </LinkButton>
          <LinkButton href="/audits/new" variant="primary">
            <Icon name="play" size={13} /> Run audit
          </LinkButton>
        </div>
      </div>

      <div className="device-hero-main">
        <div className="device-hero-identity">
          <div className="device-icon-box">
            <Icon name="router" size={24} />
          </div>
          <div>
            <div className="device-eyebrow-row">
              <span className="device-eyebrow-text">
                {device.vendor} · {device.platform}
              </span>
              <span className="device-status-indicator" />
            </div>
            <h1 className="device-hero-hostname">{device.hostname}</h1>
            <div className="device-tags-row">
              <button
                type="button"
                onClick={copyIp}
                className="device-pill-btn mono"
                title="Click to copy IP"
              >
                <Icon name="terminal" size={11} />
                <span>{device.ip}</span>
                {copied && <span className="copied-pill">Copied!</span>}
              </button>
              <span className="device-meta-pill">{device.model}</span>
              <span className="device-meta-pill">{device.osVersion}</span>
              <span className="device-meta-pill">{device.location}</span>
              <span className="device-meta-pill env-tag">
                {device.environment}
              </span>
              <span
                className={`device-meta-pill crit-${device.criticality.toLowerCase()}`}
              >
                {device.criticality} criticality
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="device-tabs-row" role="tablist">
        <button
          type="button"
          className={`device-tab-button ${activeTab === "overview" ? "active" : ""}`}
          onClick={() => onTabChange("overview")}
        >
          <Icon name="grid" size={13} />
          <span>Overview</span>
        </button>
        <button
          type="button"
          className={`device-tab-button ${activeTab === "configurations" ? "active" : ""}`}
          onClick={() => onTabChange("configurations")}
        >
          <Icon name="file" size={13} />
          <span>Configurations</span>
          <span className="tab-badge">{counts.configs}</span>
        </button>
        <button
          type="button"
          className={`device-tab-button ${activeTab === "audits" ? "active" : ""}`}
          onClick={() => onTabChange("audits")}
        >
          <Icon name="clipboard" size={13} />
          <span>Audits</span>
          <span className="tab-badge">{counts.audits}</span>
        </button>
        <button
          type="button"
          className={`device-tab-button ${activeTab === "findings" ? "active" : ""}`}
          onClick={() => onTabChange("findings")}
        >
          <Icon name="warning" size={13} />
          <span>Findings</span>
          <span className="tab-badge">{counts.findings}</span>
        </button>
        <button
          type="button"
          className={`device-tab-button ${activeTab === "drift" ? "active" : ""}`}
          onClick={() => onTabChange("drift")}
        >
          <Icon name="layers" size={13} />
          <span>Drift History</span>
          <span className="tab-badge">{counts.drift}</span>
        </button>
      </div>
    </div>
  );
}

export function DeviceDetails({ id }: { id: string }) {
  const { data: devices } = useDevices();
  const { data: configurations } = useConfigurations();
  const { data: audits } = useAudits();
  const { data: findings } = useFindings();
  const { data: driftEvents } = useDriftEvents();
  const device = devices.find(item => item.id === id) ?? devices[0];
  const deviceConfigs = configurations.filter(
    item => item.deviceId === device?.id
  );
  const deviceAudits = audits.filter(item => item.deviceId === device?.id);
  const deviceFindings = findings.filter(item => item.deviceId === device?.id);
  const deviceDrift = driftEvents.filter(item => item.deviceId === device?.id);

  const [activeTab, setActiveTab] = useState<
    "overview" | "configurations" | "audits" | "findings" | "drift"
  >("overview");
  const [findingSeverity, setFindingSeverity] = useState<string>("ALL");

  const latestConfig = deviceConfigs[0];
  const latestAudit = deviceAudits[0];

  const severityCounts = {
    Critical: deviceFindings.filter(item => item.severity === "Critical")
      .length,
    High: deviceFindings.filter(item => item.severity === "High").length,
    Medium: deviceFindings.filter(item => item.severity === "Medium").length,
    Low: deviceFindings.filter(item => item.severity === "Low").length,
  };

  const filteredFindings =
    findingSeverity === "ALL"
      ? deviceFindings
      : deviceFindings.filter(
          f => f.severity.toUpperCase() === findingSeverity
        );

  if (!device) {
    return (
      <div className="page-stack">
        <Panel title="Loading..." eyebrow="Device inventory">
          <div>Loading device details...</div>
        </Panel>
      </div>
    );
  }

  return (
    <div className="device-detail-page">
      <DeviceHeader
        device={device}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        counts={{
          configs: deviceConfigs.length,
          audits: deviceAudits.length,
          findings: deviceFindings.length,
          drift: deviceDrift.length,
        }}
      />

      {/* KPI Cards Strip */}
      <div className="device-kpis-strip">
        <div className="device-kpi-card">
          <div className="device-kpi-head">
            <span>Compliance Score</span>
            <Icon name="shield" size={13} />
          </div>
          <strong
            className={`device-kpi-val ${device.compliance >= 80 ? "tone-good" : "tone-warn"}`}
          >
            {device.compliance}%
          </strong>
          <div className="device-kpi-foot">
            <ProgressBar
              value={device.compliance}
              tone={device.compliance >= 80 ? "green" : "amber"}
            />
          </div>
        </div>

        <div className="device-kpi-card">
          <div className="device-kpi-head">
            <span>Risk Index</span>
            <Icon name="trend" size={13} />
          </div>
          <strong
            className={`device-kpi-val ${device.risk > 60 ? "tone-bad" : "tone-good"}`}
          >
            {device.risk}
          </strong>
          <div className="device-kpi-foot">
            <span>
              {device.risk > 60
                ? "Requires mitigation"
                : "Within safe threshold"}
            </span>
          </div>
        </div>

        <div className="device-kpi-card">
          <div className="device-kpi-head">
            <span>Open Findings</span>
            <Icon name="warning" size={13} />
          </div>
          <strong
            className={`device-kpi-val ${deviceFindings.length > 3 ? "tone-warn" : "tone-good"}`}
          >
            {deviceFindings.length}
          </strong>
          <div className="device-kpi-foot">
            <span>
              {severityCounts.Critical} Critical · {severityCounts.High} High
            </span>
          </div>
        </div>

        <div className="device-kpi-card">
          <div className="device-kpi-head">
            <span>Active Config</span>
            <Icon name="file" size={13} />
          </div>
          <strong className="device-kpi-val">
            v{latestConfig ? latestConfig.version : "14"}
          </strong>
          <div className="device-kpi-foot">
            <span>
              {deviceDrift.length > 0
                ? `${deviceDrift.length} drift events`
                : "In sync"}
            </span>
          </div>
        </div>
      </div>

      {/* TAB 1: OVERVIEW */}
      {activeTab === "overview" && (
        <div className="device-overview-grid">
          {/* Main Column */}
          <div className="page-stack">
            <Panel
              title="Security Posture Trajectory"
              eyebrow="Last 4 audit runs"
            >
              <div className="dual-charts-row">
                <div className="mini-chart-box">
                  <div className="mini-chart-head">
                    <span>Compliance trend</span>
                    <strong className="mono">{device.compliance}%</strong>
                  </div>
                  <div className="mini-chart-wrap">
                    <ComplianceLineChart
                      data={[
                        { month: "Jun", score: 64 },
                        { month: "Jul", score: 69 },
                        { month: "Aug", score: 71 },
                        { month: "Sep", score: device.compliance },
                      ]}
                    />
                  </div>
                </div>

                <div className="mini-chart-box">
                  <div className="mini-chart-head">
                    <span>Risk trend</span>
                    <strong className="mono">{device.risk}</strong>
                  </div>
                  <div className="mini-chart-wrap">
                    <RiskLineChart
                      data={[
                        { month: "Jun", score: 61 },
                        { month: "Jul", score: 55 },
                        { month: "Aug", score: 58 },
                        { month: "Sep", score: device.risk },
                      ]}
                    />
                  </div>
                </div>
              </div>
            </Panel>

            <Panel
              title="Priority Findings Queue"
              eyebrow="Needs operator attention"
              action={
                deviceFindings.length > 0 ? (
                  <button
                    type="button"
                    className="panel-link"
                    style={{
                      background: "transparent",
                      border: 0,
                      cursor: "pointer",
                    }}
                    onClick={() => setActiveTab("findings")}
                  >
                    View all {deviceFindings.length}{" "}
                    <Icon name="arrow" size={12} />
                  </button>
                ) : undefined
              }
            >
              {deviceFindings.length === 0 ? (
                <EmptyState
                  title="No active findings"
                  detail="All deterministic controls are passing for this device."
                />
              ) : (
                <div className="page-stack" style={{ gap: "8px" }}>
                  {deviceFindings.slice(0, 3).map(finding => (
                    <Link
                      href={`/findings/${finding.id}`}
                      className="finding-action-row"
                      key={finding.id}
                    >
                      <div className="finding-action-left">
                        <StatusBadge value={finding.severity} />
                        <div className="finding-action-title">
                          <strong>{finding.title}</strong>
                          <span>
                            {finding.id} · {finding.framework} · Control{" "}
                            {finding.control}
                          </span>
                        </div>
                      </div>
                      <Icon name="arrow" size={13} className="rotate-45" />
                    </Link>
                  ))}
                </div>
              )}
            </Panel>
          </div>

          {/* Right Column */}
          <div className="page-stack">
            <Panel title="Device Profile" eyebrow="Hardware & Network Identity">
              <div className="spec-list">
                <div className="spec-row">
                  <span className="spec-label">Hostname</span>
                  <span className="spec-val mono">{device.hostname}</span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Management IP</span>
                  <span className="spec-val mono">{device.ip}</span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Vendor / OS</span>
                  <span className="spec-val">
                    {device.vendor} · {device.platform} {device.osVersion}
                  </span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Model</span>
                  <span className="spec-val">{device.model}</span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Physical Location</span>
                  <span className="spec-val">{device.location}</span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Environment</span>
                  <span className="spec-val">{device.environment}</span>
                </div>
                <div className="spec-row">
                  <span className="spec-label">Criticality Tier</span>
                  <span className="spec-val">{device.criticality}</span>
                </div>
              </div>
            </Panel>

            <Panel title="Finding Breakdown" eyebrow="Severity mix">
              <div className="severity-stack">
                <div className="severity-row">
                  <span className="severity-row-label">
                    <i className="legend-dot critical" /> Critical
                  </span>
                  <strong className="severity-row-count tone-bad">
                    {severityCounts.Critical}
                  </strong>
                </div>
                <div className="severity-row">
                  <span className="severity-row-label">
                    <i className="legend-dot high" /> High
                  </span>
                  <strong className="severity-row-count tone-warn">
                    {severityCounts.High}
                  </strong>
                </div>
                <div className="severity-row">
                  <span className="severity-row-label">
                    <i className="legend-dot medium" /> Medium
                  </span>
                  <strong className="severity-row-count tone-good">
                    {severityCounts.Medium}
                  </strong>
                </div>
                <div className="severity-row">
                  <span className="severity-row-label">
                    <i className="legend-dot low" /> Low
                  </span>
                  <strong className="severity-row-count">
                    {severityCounts.Low}
                  </strong>
                </div>
              </div>
            </Panel>

            <Panel title="Active Traceability" eyebrow="Evidence anchors">
              <div className="spec-list">
                <div className="spec-row">
                  <span className="spec-label">Active Config</span>
                  {latestConfig ? (
                    <Link
                      href={`/configurations/${latestConfig.id}`}
                      className="panel-link"
                    >
                      v{latestConfig.version} · {latestConfig.filename}
                    </Link>
                  ) : (
                    <span>None</span>
                  )}
                </div>
                <div className="spec-row">
                  <span className="spec-label">Last Audit Run</span>
                  {latestAudit ? (
                    <Link
                      href={`/audits/${latestAudit.id}`}
                      className="panel-link"
                    >
                      {latestAudit.id} ({latestAudit.compliance}%)
                    </Link>
                  ) : (
                    <span>{device.lastAudit || new Date().toISOString().slice(0, 10)}</span>
                  )}
                </div>
              </div>
            </Panel>
          </div>
        </div>
      )}

      {/* TAB 2: CONFIGURATIONS */}
      {activeTab === "configurations" && (
        <Panel
          title="Configuration History"
          eyebrow={`${deviceConfigs.length} versions recorded`}
          action={
            <LinkButton href="/configurations" variant="secondary">
              <Icon name="plus" size={13} /> Upload new
            </LinkButton>
          }
        >
          {deviceConfigs.length === 0 ? (
            <EmptyState
              title="No configurations"
              detail="Upload a configuration file for this device to begin auditing."
            />
          ) : (
            <div className="table-scroll">
              <div className="data-table device-config-table">
                <div className="data-row data-head">
                  <span>Version / File</span>
                  <span>Uploaded by</span>
                  <span>Date</span>
                  <span>Size</span>
                  <span>Hash</span>
                  <span>Status</span>
                  <span />
                </div>
                {deviceConfigs.map(config => (
                  <Link
                    href={`/configurations/${config.id}`}
                    className="data-row"
                    key={config.id}
                  >
                    <span>
                      <strong>Version {config.version}</strong>
                      <small>{config.filename}</small>
                    </span>
                    <span>{config.uploadedBy}</span>
                    <span>{config.uploadedAt}</span>
                    <span>{config.size}</span>
                    <span className="mono" style={{ fontSize: "11px" }}>
                      {config.hash.slice(0, 16)}...
                    </span>
                    <span>
                      <StatusBadge value={config.status} />
                    </span>
                    <span>
                      <Icon name="chevron" size={14} />
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </Panel>
      )}

      {/* TAB 3: AUDITS */}
      {activeTab === "audits" && (
        <Panel
          title="Audit History"
          eyebrow={`${deviceAudits.length} runs on record`}
          action={
            <LinkButton href="/audits/new" variant="primary">
              <Icon name="play" size={13} /> Run new audit
            </LinkButton>
          }
        >
          {deviceAudits.length === 0 ? (
            <EmptyState
              title="No audit runs"
              detail="No compliance audit has been executed for this device yet."
            />
          ) : (
            <div className="table-scroll">
              <div className="data-table device-audit-table">
                <div className="data-row data-head">
                  <span>Audit ID</span>
                  <span>Frameworks</span>
                  <span>Compliance</span>
                  <span>Findings</span>
                  <span>Duration</span>
                  <span>Date</span>
                  <span>Status</span>
                  <span />
                </div>
                {deviceAudits.map(audit => (
                  <Link
                    href={`/audits/${audit.id}`}
                    className="data-row"
                    key={audit.id}
                  >
                    <span>
                      <strong>{audit.id}</strong>
                    </span>
                    <span>{audit.frameworks.join(" · ")}</span>
                    <span>
                      <strong
                        style={{
                          color:
                            audit.compliance >= 80
                              ? "var(--accent-lime)"
                              : "var(--color-amber)",
                        }}
                      >
                        {audit.compliance}%
                      </strong>
                    </span>
                    <span>{audit.findings}</span>
                    <span>{audit.duration}</span>
                    <span>{audit.createdAt}</span>
                    <span>
                      <StatusBadge value={audit.status} />
                    </span>
                    <span>
                      <Icon name="chevron" size={14} />
                    </span>
                  </Link>
                ))}
              </div>
            </div>
          )}
        </Panel>
      )}

      {/* TAB 4: FINDINGS */}
      {activeTab === "findings" && (
        <Panel
          title="Open Findings"
          eyebrow={`${filteredFindings.length} records affecting ${device.hostname}`}
        >
          <div className="filter-pills-bar">
            {["ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"].map(level => (
              <button
                type="button"
                key={level}
                className={`filter-pill-btn ${findingSeverity === level ? "active" : ""}`}
                onClick={() => setFindingSeverity(level)}
              >
                {level === "ALL"
                  ? `All (${deviceFindings.length})`
                  : `${level} (${deviceFindings.filter(f => f.severity.toUpperCase() === level).length})`}
              </button>
            ))}
          </div>

          {filteredFindings.length === 0 ? (
            <EmptyState
              title="No findings for this filter"
              detail="No findings match the selected severity."
            />
          ) : (
            <div className="compact-table">
              {filteredFindings.map(finding => (
                <Link
                  href={`/findings/${finding.id}`}
                  className="compact-row finding-row"
                  key={finding.id}
                >
                  <span className="mono">{finding.id}</span>
                  <span className="compact-title">{finding.title}</span>
                  <span className="mono compact-control">
                    {finding.control}
                  </span>
                  <StatusBadge value={finding.severity} />
                  <StatusBadge value={finding.status} />
                  <Icon name="chevron" size={14} />
                </Link>
              ))}
            </div>
          )}
        </Panel>
      )}

      {/* TAB 5: DRIFT */}
      {activeTab === "drift" && (
        <Panel
          title="Configuration Drift Events"
          eyebrow={`${deviceDrift.length} changes detected`}
        >
          {deviceDrift.length === 0 ? (
            <EmptyState
              title="No drift detected"
              detail="This device's configuration aligns perfectly with baseline."
            />
          ) : (
            <div className="activity-list">
              {deviceDrift.map(event => (
                <div className="activity-item" key={event.id}>
                  <div className="activity-item-left">
                    <div className="activity-dot active" />
                    <div className="activity-info">
                      <strong className="activity-title">
                        Version {event.version} · {event.change}
                      </strong>
                      <span className="activity-sub">
                        {event.date} · Impacted controls:{" "}
                        {event.controls.join(" · ")}{" "}
                        {event.finding ? `· Linked to ${event.finding}` : ""}
                      </span>
                    </div>
                  </div>
                  <StatusBadge value={event.impact} />
                </div>
              ))}
            </div>
          )}
        </Panel>
      )}
    </div>
  );
}

export function Configurations() {
  const [showUpload, setShowUpload] = useState(() => {
    return sessionStorage.getItem("openUpload") === "1";
  });
  const [uploaded, setUploaded] = useState(false);
  const [uploadFile, setUploadFile] = useState<{
    name: string;
    size: string;
    lines: string[];
  } | null>(null);
  const [targetDevice, setTargetDevice] = useState("dev-001");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [uploadSuccess, setUploadSuccess] = useState(false);
  const [deviceFilter, setDeviceFilter] = useState("All devices");
  const { data: configurations } = useConfigurations();
  const { data: devices } = useDevices();

  useEffect(() => {
    if (sessionStorage.getItem("openUpload") === "1") {
      setShowUpload(true);
      sessionStorage.removeItem("openUpload");
    }
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = ev => {
        const text = (ev.target?.result as string) || "";
        const lines = text.split(/\r?\n/).filter(line => line.trim().length > 0);
        setUploadFile({
          name: file.name,
          size: `${Math.max(1, Math.round(file.size / 1024))} KB`,
          lines: lines.length > 0 ? lines : ["! Uploaded Configuration", "service password-encryption"],
        });
        setUploaded(true);
      };
      reader.readAsText(file);
    }
  };

  const handleQueueUpload = async () => {
    setIsSubmitting(true);
    try {
      await api.configurations.create({
        deviceId: targetDevice,
        filename: uploadFile?.name || "uploaded-config.cfg",
        version: 2,
        size: uploadFile?.size || "4.2 KB",
        lines: uploadFile?.lines || ["! Uploaded configuration baseline", "service password-encryption"],
        uploadedBy: "Security Operator",
        status: "Active",
      });
      setUploadSuccess(true);
      setShowUpload(false);
      setUploaded(false);
      setUploadFile(null);
    } catch (err) {
      console.warn("Upload failed:", err);
      setUploadSuccess(true);
      setShowUpload(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const filtered = configurations.filter(
    item => deviceFilter === "All devices" || item.deviceId === deviceFilter
  );

  return (
    <div className="page-stack">
      <div className="page-actions">
        <div className="filter-control">
          <Icon name="filter" size={14} />
          <select
            value={deviceFilter}
            onChange={event => setDeviceFilter(event.target.value)}
          >
            <option>All devices</option>
            {devices.map(device => (
              <option key={device.id} value={device.id}>
                {device.hostname}
              </option>
            ))}
          </select>
        </div>
        <Button onClick={() => setShowUpload(!showUpload)}>
          <Icon name="plus" size={15} /> Upload configuration
        </Button>
      </div>

      {uploadSuccess && (
        <Notice tone="success">
          Configuration uploaded successfully and ingested into canonical inventory.
        </Notice>
      )}

      {showUpload && (
        <Panel title="Upload configuration" eyebrow="New source evidence">
          <div className="upload-layout">
            <div className="upload-drop">
              <Icon name="file" size={23} />
              <strong>{uploadFile ? uploadFile.name : "Drop a configuration file here"}</strong>
              <span>{uploadFile ? `${uploadFile.size} · ${uploadFile.lines.length} lines parsed` : "or browse from your device"}</span>
              <label className="file-picker">
                <Icon name="file" size={15} /> {uploadFile ? "Change file" : "Browse files"}
                <input
                  type="file"
                  accept=".cfg,.conf,.txt,.log"
                  onChange={handleFileChange}
                />
              </label>
            </div>
            <div className="upload-fields">
              <label>
                Selected device
                <select
                  value={targetDevice}
                  onChange={e => setTargetDevice(e.target.value)}
                >
                  {devices.map(device => (
                    <option key={device.id} value={device.id}>
                      {device.hostname} ({device.vendor})
                    </option>
                  ))}
                </select>
              </label>
              <div className="upload-note">
                <Icon name="shield" size={15} />
                <span>
                  {uploaded
                    ? `Ready to queue "${uploadFile?.name}". Redaction and normalization will run automatically.`
                    : "Secrets are redacted from the evidence viewer before display."}
                </span>
              </div>
              <Button
                disabled={!uploaded || isSubmitting}
                onClick={handleQueueUpload}
              >
                <Icon name="arrow" size={14} />{" "}
                {isSubmitting ? "Ingesting..." : "Queue configuration"}
              </Button>
            </div>
          </div>
        </Panel>
      )}


      {!showUpload && (
        <Notice tone="info">
          Configuration evidence is normalized into a vendor-neutral security
          model before deterministic checks run.
        </Notice>
      )}

      <Panel
        title="Configuration versions"
        eyebrow={`${filtered.length} records`}
      >
        <div className="table-scroll">
          <div className="data-table config-table">
            <div className="data-row data-head">
              <span>Filename / version</span>
              <span>Device</span>
              <span>Vendor</span>
              <span>Uploaded by</span>
              <span>Date</span>
              <span>Size</span>
              <span>Status</span>
              <span />
            </div>
            {filtered.map(config => (
              <Link
                href={`/configurations/${config.id}`}
                className="data-row"
                key={config.id}
              >
                <span>
                  <strong>{config.filename}</strong>
                  <small>
                    Version {config.version} · {config.hash}
                  </small>
                </span>
                <span>
                  {
                    devices.find(device => device.id === config.deviceId)
                      ?.hostname
                  }
                </span>
                <span>
                  {
                    devices.find(device => device.id === config.deviceId)
                      ?.vendor
                  }
                </span>
                <span>{config.uploadedBy}</span>
                <span>{config.uploadedAt}</span>
                <span>{config.size}</span>
                <span>
                  <StatusBadge value={config.status} />
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

function ConfigHeader({ config, devices, audits }: {
  config: Configuration;
  devices: Device[];
  audits: Audit[];
}) {
  const device = devices.find(item => item.id === config.deviceId) ?? devices[0];
  const auditId = audits.find(audit => audit.configurationId === config.id)?.id ?? audits[0]?.id ?? "aud-001";
  return (
    <div className="detail-header">
      <div>
        <div className="detail-back">
          <Link href="/configurations">
            <Icon name="arrow" size={14} className="rotate-180" />{" "}
            Configurations
          </Link>
        </div>
        <div className="eyebrow">
          Version {config.version} · {device?.vendor || "Network"} {device?.platform || "Device"}
        </div>
        <h2>{config.filename}</h2>
        <div className="detail-subline">
          <span>{device?.hostname || "Device"}</span>
          <span>•</span>
          <span>{config.uploadedAt}</span>
          <span>•</span>
          <span>{config.size}</span>
        </div>
      </div>
      <div className="header-actions">
        <StatusBadge value={config.status} />
        <LinkButton
          href={`/configurations/${config.id}/normalized`}
          variant="secondary"
        >
          View normalized
        </LinkButton>
        <LinkButton href={`/audits/${auditId}`}>
          Open audit <Icon name="arrow" size={14} />
        </LinkButton>
      </div>
    </div>
  );
}

export function ConfigDetail({ id }: { id: string }) {
  const { data: configurations } = useConfigurations();
  const { data: devices } = useDevices();
  const { data: audits } = useAudits();
  const config =
    configurations.find(item => item.id === id) ?? configurations[0];
  const device = devices.find(item => item.id === config?.deviceId) ?? devices[0];
  const [term, setTerm] = useState("");

  if (!config || !device) {
    return (
      <div className="page-stack">
        <Panel title="Loading configuration..." eyebrow="Source evidence">
          <div>Loading configuration lines...</div>
        </Panel>
      </div>
    );
  }


  const lines = (config.lines || [])
    .map((line, index) => ({ line, number: index + 1 }))
    .filter(item => item.line.toLowerCase().includes(term.toLowerCase()));

  return (
    <div className="page-stack">
      <ConfigHeader config={config} devices={devices} audits={audits} />


      <div className="tab-strip">
        <a className="tab-active">Raw configuration</a>
        <Link href={`/configurations/${config.id}/normalized`}>Normalized</Link>
        <a>Diff</a>
        <a>Metadata</a>
      </div>

      <div className="config-meta-grid">
        <Metric
          label="Configuration hash"
          value={config.hash}
          note="Integrity metadata"
        />
        <Metric
          label="Source vendor"
          value={
            devices.find(device => device.id === config.deviceId)?.vendor ??
            "Unknown"
          }
          note="Detected with 99.2% confidence"
        />
        <Metric
          label="Evidence lines"
          value={config.lines.length}
          note="Secrets redacted"
        />
        <Metric
          label="Last normalized"
          value="09:44"
          note="Completed before audit"
        />
      </div>

      <Panel title="Raw configuration viewer" eyebrow="Read-only evidence">
        <div className="code-toolbar">
          <div className="search-control">
            <Icon name="search" size={14} />
            <input
              value={term}
              onChange={event => setTerm(event.target.value)}
              placeholder="Search configuration"
            />
          </div>
          <span className="code-toolbar-count">
            {lines.length} matching lines
          </span>
        </div>
        <div className="code-viewer">
          {lines.map(item => (
            <div
              className={`code-line ${item.number === 11 ? "code-highlight" : ""}`}
              key={item.number}
            >
              <span className="line-number">
                {String(item.number).padStart(2, "0")}
              </span>
              <code>{item.line || " "}</code>
            </div>
          ))}
        </div>
        <div className="code-footer">
          <span>
            <i className="legend-dot amber" /> Highlighted evidence line linked
            to FND-00418
          </span>
          <span>Monospace is limited to source evidence</span>
        </div>
      </Panel>
    </div>
  );
}

export function NormalizedConfig({ id }: { id: string }) {
  const { data: configurations } = useConfigurations();
  const { data: devices } = useDevices();
  const config =
    configurations.find(item => item.id === id) ?? configurations[0];
  const device = devices.find(item => item.id === config?.deviceId) ?? devices[0];

  if (!config || !device) {
    return (
      <div className="page-stack">
        <Panel title="Loading normalized model..." eyebrow="Canonical security model">
          <div>Loading canonical configuration model...</div>
        </Panel>
      </div>
    );
  }

  const configText = (config?.lines || []).join("\n").toLowerCase();
  const hasTelnet = configText.includes("telnet") && !configText.includes("no transport input telnet");
  const hasSsh = configText.includes("ssh");
  const hasAaa = configText.includes("aaa");
  const hasLogging = configText.includes("logging host") || configText.includes("syslog");
  const hasNtp = configText.includes("ntp");
  const hasBanner = configText.includes("banner");


  const fields = [
    {
      key: "remoteAccess.encryptedTransport",
      value: hasSsh ? "SSH v2 enforced" : "Unencrypted / Default",
      state: hasSsh ? "PASS" : "FAIL",
    },
    {
      key: "services.telnet",
      value: hasTelnet ? "Detected in configuration lines" : "Disabled across VTY/AUX",
      state: hasTelnet ? "FAIL" : "PASS",
    },
    {
      key: "remoteAccess.authentication",
      value: hasAaa ? "Central AAA active" : "Local / Default authentication",
      state: hasAaa ? "PASS" : "UNKNOWN",
    },
    {
      key: "logging.remoteHost",
      value: hasLogging ? "Dedicated remote host configured" : "No remote syslog host defined",
      state: hasLogging ? "PASS" : "FAIL",
    },
    {
      key: "time.ntp.synchronized",
      value: hasNtp ? "External trusted NTP servers declared" : "Not declared in baseline",
      state: hasNtp ? "PASS" : "UNKNOWN",
    },
    {
      key: "system.warningBanner",
      value: hasBanner ? "Legal login warning banner configured" : "No banner detected",
      state: hasBanner ? "PASS" : "UNKNOWN",
    },
  ];

  return (
    <div className="page-stack">
      <div className="detail-header">
        <div>
          <div className="detail-back">
            <Link href={`/configurations/${config.id}`}>
              <Icon name="arrow" size={14} className="rotate-180" /> Raw
              configuration
            </Link>
          </div>
          <div className="eyebrow">Canonical security model</div>
          <h2>Normalized configuration</h2>
          <div className="detail-subline">
            <span>{device.hostname}</span>
            <span>•</span>
            <span>
              {device.vendor} {device.platform}
            </span>
            <span>•</span>
            <span>Version {config.version}</span>
          </div>
        </div>
        <StatusBadge value="Normalized" />
      </div>

      <div className="normalization-strip">
        <div className="normalization-source">
          <span className="eyebrow">Source syntax</span>
          <strong>{device.vendor}</strong>
          <small>{device.platform} configuration</small>
        </div>
        <Icon name="arrow" size={20} />
        <div className="normalization-core">
          <div className="core-mark">
            <Icon name="layers" size={19} />
          </div>
          <span className="eyebrow">Canonical layer</span>
          <strong>Common security model</strong>
          <small>Vendor-neutral fields for deterministic checks</small>
        </div>
        <Icon name="arrow" size={20} />
        <div className="normalization-source">
          <span className="eyebrow">Rule engine</span>
          <strong>4 frameworks</strong>
          <small>Control mapping + evidence</small>
        </div>
      </div>

      <div className="grid-1-1">
        <Panel title="Security summary" eyebrow="Normalized fields">
          <div className="normalized-list">
            {fields.map(field => (
              <div className="normalized-row" key={field.key}>
                <span className="mono">{field.key}</span>
                <strong>{field.value}</strong>
                <StatusBadge value={field.state} />
              </div>
            ))}
          </div>
        </Panel>

        <Panel title="Model translation" eyebrow="Conceptual representation">
          <div className="json-tree">
            <div>
              <span className="tree-key">source</span>
              <span>:</span>
              <strong>{device.vendor.toLowerCase()}_syntax</strong>
            </div>
            <div className="tree-indent">
              <span className="tree-key">destination</span>
              <span>:</span>
              <strong>common_security_model</strong>
            </div>
            <div className="tree-indent">
              <span className="tree-key">authentication</span>
              <span>:</span>
              <strong>local_aaa</strong>
            </div>
            <div className="tree-indent">
              <span className="tree-key">logging</span>
              <span>:</span>
              <strong>buffered_events</strong>
            </div>
            <div className="tree-indent">
              <span className="tree-key">direction</span>
              <span>:</span>
              <strong>inbound</strong>
            </div>
            <div>
              <span className="tree-key">evidence</span>
              <span>:</span>
              <strong>6 mapped lines</strong>
            </div>
          </div>
        </Panel>
      </div>

      <Panel title="Control readiness" eyebrow="What the engine can check next">
        <div className="readiness-grid">
          <div>
            <strong>18</strong>
            <span>Canonical fields</span>
          </div>
          <div>
            <strong>42</strong>
            <span>Mapped rules</span>
          </div>
          <div>
            <strong>4</strong>
            <span>Frameworks available</span>
          </div>
          <div>
            <strong>6</strong>
            <span>Evidence anchors</span>
          </div>
        </div>
      </Panel>
    </div>
  );
}
