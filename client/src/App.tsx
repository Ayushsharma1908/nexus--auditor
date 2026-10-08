import { useState } from "react";
import { Route, Switch, Redirect, Link, useLocation } from "wouter";
import ErrorBoundary from "@/components/ErrorBoundary";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { AppShell } from "@/layout/AppShell";
import Login from "@/pages/Login";
import {
  Dashboard,
  DeviceDetails,
  DeviceForm,
  Devices,
  Configurations,
  ConfigDetail,
  NormalizedConfig,
} from "@/pages/Workspace";
import {
  Audits,
  AuditDetail,
  Findings,
  FindingDetail,
  RiskCenter,
} from "@/pages/CompliancePages";
import {
  AiAnalyst,
  ControlDetail,
  Drift,
  FrameworkDetail,
  Frameworks,
  ReportPreview,
  Reports,
  Settings,
  WhatIf,
} from "@/pages/IntelligencePages";
import { api } from "@/lib/api";
import { useDevices, useConfigurations } from "@/lib/useApi";
import type { Device, Configuration } from "@/types";



function Protected({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Redirect to="/login" />;
  }
  return <AppShell>{children}</AppShell>;
}

function Router() {
  return (
    <Switch>
      <Route path="/login" component={Login} />
      <Route path="/">
        {() => <Redirect to="/login" />}
      </Route>
      <Route path="/dashboard">
        {() => (
          <Protected>
            <Dashboard />
          </Protected>
        )}
      </Route>
      <Route path="/devices/new">
        {() => (
          <Protected>
            <DeviceForm />
          </Protected>
        )}
      </Route>
      <Route path="/devices/:id/edit">
        {params => (
          <Protected>
            <DeviceForm id={params.id} edit />
          </Protected>
        )}
      </Route>

      <Route path="/devices/:id">
        {params => (
          <Protected>
            <DeviceDetails id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/devices">
        {() => (
          <Protected>
            <Devices />
          </Protected>
        )}
      </Route>
      <Route path="/configurations/:id/normalized">
        {params => (
          <Protected>
            <NormalizedConfig id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/configurations/:id">
        {params => (
          <Protected>
            <ConfigDetail id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/configurations">
        {() => (
          <Protected>
            <Configurations />
          </Protected>
        )}
      </Route>
      <Route path="/audits/new">
        {() => (
          <Protected>
            <NewAudit />
          </Protected>
        )}
      </Route>
      <Route path="/audits/:id">
        {params => (
          <Protected>
            <AuditDetail id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/audits">
        {() => (
          <Protected>
            <Audits />
          </Protected>
        )}
      </Route>
      <Route path="/findings/:id/evidence">
        {params => (
          <Protected>
            <FindingDetail id={params.id} evidenceOnly />
          </Protected>
        )}
      </Route>
      <Route path="/findings/:id">
        {params => (
          <Protected>
            <FindingDetail id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/findings">
        {() => (
          <Protected>
            <Findings />
          </Protected>
        )}
      </Route>
      <Route path="/risk">
        {() => (
          <Protected>
            <RiskCenter />
          </Protected>
        )}
      </Route>
      <Route path="/drift">
        {() => (
          <Protected>
            <Drift />
          </Protected>
        )}
      </Route>
      <Route path="/what-if">
        {() => (
          <Protected>
            <WhatIf />
          </Protected>
        )}
      </Route>
      <Route path="/simulation">
        {() => (
          <Protected>
            <WhatIf />
          </Protected>
        )}
      </Route>
      <Route path="/ai-analyst">
        {() => (
          <Protected>
            <AiAnalyst />
          </Protected>
        )}
      </Route>
      <Route path="/reports/:id">
        {params => (
          <Protected>
            <ReportPreview id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/reports">
        {() => (
          <Protected>
            <Reports />
          </Protected>
        )}
      </Route>
      <Route path="/frameworks/:id">
        {params => (
          <Protected>
            <FrameworkDetail id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/frameworks">
        {() => (
          <Protected>
            <Frameworks />
          </Protected>
        )}
      </Route>
      <Route path="/controls/:id">
        {params => (
          <Protected>
            <ControlDetail id={params.id} />
          </Protected>
        )}
      </Route>
      <Route path="/settings">
        {() => (
          <Protected>
            <Settings />
          </Protected>
        )}
      </Route>
      <Route>
        {() => (
          <Protected>
            <Dashboard />
          </Protected>
        )}
      </Route>
    </Switch>
  );
}

function NewAudit() {
  const [, navigate] = useLocation();
  const { data: devices } = useDevices();
  const [selectedDevice, setSelectedDevice] = useState("dev-001");
  const { data: configs } = useConfigurations(selectedDevice);
  const [selectedConfig, setSelectedConfig] = useState("");
  const [selectedFramework, setSelectedFramework] = useState("CIS Controls + NIST SP 800-53");
  const [isStarting, setIsStarting] = useState(false);

  const handleStart = async () => {
    setIsStarting(true);
    try {
      const created = await api.audits.create({
        name: `Automated Compliance Run - ${selectedDevice}`,
        type: "Automated",
        deviceIds: [selectedDevice],
        configurationIds: selectedConfig ? [selectedConfig] : [],
        frameworkIds: ["FW-CIS", "FW-NIST"],
      }) as { id?: string };
      const targetId = created?.id || "aud-001";
      navigate(`/audits/${targetId}`);
    } catch (err) {
      console.warn("Audit initialization error, routing to aud-001:", err);
      navigate("/audits/aud-001");
    } finally {
      setIsStarting(false);
    }
  };


  return (
    <div className="page-stack narrow-page">
      <div className="detail-back">
        <Link href="/audits">← Back to audits</Link>
      </div>
      <div className="detail-header">
        <div>
          <div className="eyebrow">Create compliance run</div>
          <h2>New audit</h2>
          <div className="detail-subline">
            <span>
              Select context, review scope, and start the deterministic rule
              engine.
            </span>
          </div>
        </div>
      </div>
      <div className="audit-wizard">
        <div className="wizard-steps">
          <div className="wizard-step active">
            <span>1</span>
            <strong>Context</strong>
            <small>Device and version</small>
          </div>
          <div className="wizard-step">
            <span>2</span>
            <strong>Frameworks</strong>
            <small>Choose coverage</small>
          </div>
          <div className="wizard-step">
            <span>3</span>
            <strong>Review</strong>
            <small>Start audit</small>
          </div>
        </div>
        <div className="form-grid">
          <label>
            Device
            <select
              value={selectedDevice}
              onChange={e => {
                setSelectedDevice(e.target.value);
                setSelectedConfig("");
              }}
            >
              {devices.map((d: Device) => (
                <option key={d.id} value={d.id}>
                  {d.hostname} ({d.vendor} · {d.platform})
                </option>
              ))}
            </select>
          </label>
          <label>
            Configuration version
            <select
              value={selectedConfig}
              onChange={e => setSelectedConfig(e.target.value)}
            >
              {configs.length > 0 ? (
                configs.map((c: Configuration) => (
                  <option key={c.id} value={c.id}>
                    {c.filename} · Version {c.version} ({c.uploadedAt})
                  </option>
                ))
              ) : (
                <option value="">Baseline configuration</option>
              )}
            </select>

          </label>
          <label className="span-2">
            Frameworks
            <select
              value={selectedFramework}
              onChange={e => setSelectedFramework(e.target.value)}
            >
              <option>CIS Controls + NIST SP 800-53</option>
              <option>All configured frameworks (CIS, NIST, STIG, ISO)</option>
              <option>CIS Benchmarks only</option>
              <option>PCI-DSS v4.0</option>
            </select>
          </label>
          <div className="form-actions span-2">
            <Link className="btn btn-ghost" href="/audits">
              Cancel
            </Link>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleStart}
              disabled={isStarting}
            >
              {isStarting ? "Initializing run..." : "Review and start →"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <Router />
      </AuthProvider>
    </ErrorBoundary>
  );
}
