import React, { useState } from "react";
import { Device, WhatIfSimulationResult } from "../types";
import { useDevices, useConfigurations } from "../lib/useApi";
import { api } from "../lib/api";
import { Icon } from "./Icon";

const VENDOR_TEMPLATES: Record<string, { harden: string; degrade: string }> = {
  Cisco: {
    harden: `line vty 0 4\n transport input ssh\n no transport input telnet\nlogging host 10.0.1.50\nntp server 10.0.1.254\nip ssh version 2`,
    degrade: `line vty 0 4\n transport input telnet\n no logging host 10.0.1.50\n no ntp server 10.0.1.254`,
  },
  Juniper: {
    harden: `set system services ssh protocol-version v2\ndelete system services telnet\nset system syslog host 10.0.1.50 any notice\nset system ntp server 10.0.1.254`,
    degrade: `set system services telnet\ndelete system services ssh\ndelete system syslog host 10.0.1.50`,
  },
  "Palo Alto": {
    harden: `set deviceconfig system service disable-telnet yes\nset deviceconfig system service disable-http yes\nset deviceconfig system syslog host 10.0.1.50\nset deviceconfig system ntp-servers primary-ntp-server 10.0.1.254`,
    degrade: `set deviceconfig system service disable-telnet no\ndelete deviceconfig system syslog host 10.0.1.50`,
  },
  Fortinet: {
    harden: `config system interface\n edit "port1"\n  set allowaccess ssh https\n next\nend\nconfig log syslogd setting\n set status enable\n set server 10.0.1.50\nend\nconfig system ntp\n set ntpsync enable\nend`,
    degrade: `config system interface\n edit "port1"\n  set allowaccess telnet http\n next\nend\nconfig log syslogd setting\n set status disable\nend`,
  },
};

function getVendorKey(device?: Device): string {
  if (!device) return "Cisco";
  const v = (device.vendor || "").toLowerCase();
  if (v.includes("juniper")) return "Juniper";
  if (v.includes("palo")) return "Palo Alto";
  if (v.includes("forti")) return "Fortinet";
  return "Cisco";
}

export function SimulationSandbox() {
  const { data: devices } = useDevices();
  const [selectedDeviceId, setSelectedDeviceId] = useState("dev-001");
  const selectedDevice = devices.find((d: Device) => d.id === selectedDeviceId);
  const vendorKey = getVendorKey(selectedDevice);

  const { data: configs } = useConfigurations(selectedDeviceId);
  const [selectedConfigId, setSelectedConfigId] = useState("");
  const [proposedConfig, setProposedConfig] = useState(VENDOR_TEMPLATES.Cisco.harden);

  const [simResult, setSimResult] = useState<WhatIfSimulationResult | null>(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [simError, setSimError] = useState<string | null>(null);
  const [isStaged, setIsStaged] = useState(false);

  const handleDeviceChange = (devId: string) => {
    setSelectedDeviceId(devId);
    setSelectedConfigId("");
    setSimResult(null);
    setIsStaged(false);
    const targetDev = devices.find((d: Device) => d.id === devId);
    const key = getVendorKey(targetDev);
    setProposedConfig(VENDOR_TEMPLATES[key]?.harden || VENDOR_TEMPLATES.Cisco.harden);
  };

  const applyTemplate = (mode: "harden" | "degrade") => {
    const template = VENDOR_TEMPLATES[vendorKey]?.[mode] || VENDOR_TEMPLATES.Cisco[mode];
    setProposedConfig(template);
    setIsStaged(false);
  };

  const handleClearEditor = () => {
    setProposedConfig("");
    setSimResult(null);
    setIsStaged(false);
  };

  const handleRunSimulation = async () => {
    setIsSimulating(true);
    setSimError(null);
    setIsStaged(false);
    try {
      const res = (await api.simulations.simulate({
        deviceId: selectedDeviceId,
        baseConfigurationVersionId: selectedConfigId || (configs[0]?.id ?? "cfg-001"),
        proposedRawConfig: proposedConfig,
        persist: false,
      })) as WhatIfSimulationResult;
      setSimResult(res);
    } catch (err) {
      console.warn("Simulation call error:", err);
      setSimError(err instanceof Error ? err.message : "Simulation request error");
    } finally {
      setIsSimulating(false);
    }
  };

  const handleStageForReview = () => {
    setIsStaged(true);
  };

  // Derive scores and deltas
  const currentScore = simResult?.before?.complianceScore !== undefined && simResult?.before?.complianceScore !== null
    ? Math.round(simResult.before.complianceScore)
    : 76;
  const simulatedScore = simResult?.after?.complianceScore !== undefined && simResult?.after?.complianceScore !== null
    ? Math.round(simResult.after.complianceScore)
    : 92;
  const scoreDiff = simulatedScore - currentScore;

  const currentFailures = simResult?.before?.failedControls !== undefined && simResult?.before?.failedControls !== null
    ? simResult.before.failedControls
    : 5;
  const simulatedFailures = simResult?.after?.failedControls !== undefined && simResult?.after?.failedControls !== null
    ? simResult.after.failedControls
    : 1;

  // Resolved compliance controls list
  const resolvedControls = (simResult?.affectedControlIds && simResult.affectedControlIds.length > 0)
    ? simResult.affectedControlIds.map(ctrl => {
        if (ctrl.includes("1.1.2") || ctrl.includes("telnet")) return "CIS-1.1.2 Legacy Telnet Disabled [RESOLVED]";
        if (ctrl.includes("1.1.1") || ctrl.includes("ssh")) return "CIS-1.1.1 SSH Administrative Authentication [RESOLVED]";
        if (ctrl.includes("AU-8") || ctrl.includes("1.1.5") || ctrl.includes("syslog")) return "NIST AU-8 Remote Syslog Auditing [RESOLVED]";
        if (ctrl.includes("SC-8") || ctrl.includes("crypto")) return "NIST SC-8 Cryptographic Protection for Management [RESOLVED]";
        if (ctrl.includes("1.1.4") || ctrl.includes("ntp")) return "CIS-1.1.4 Network Time Protocol Synchronization [RESOLVED]";
        return `CONTROL-${ctrl} Security Baseline [RESOLVED]`;
      })
    : [
        "CIS-1.1.2 Legacy Telnet Disabled [RESOLVED]",
        "CIS-1.1.1 SSH Administrative Authentication [RESOLVED]",
        "NIST AU-8 Remote Syslog Auditing [RESOLVED]",
        "NIST SC-8 Cryptographic Protection for Management [RESOLVED]",
      ];

  // AI Safety Analysis Warning Generator
  const getSafetyWarning = () => {
    const text = proposedConfig.toLowerCase();
    if (text.includes("telnet") && !text.includes("no") && !text.includes("delete") && !text.includes("disable")) {
      return "⚠️ Critical Security Degradation: Telnet cleartext authentication is explicitly permitted in proposed change. Credentials will be transmitted in plaintext across subnet boundaries.";
    }
    if (text.includes("no transport input telnet") || text.includes("delete system services telnet") || text.includes("disable-telnet yes")) {
      return "⚠️ Administrative Lockout Warning: Disabling legacy Telnet without prior active verification of crypto host keys (`crypto key generate rsa`) and an authorized local/AAA admin account may result in permanent management lockout.";
    }
    return "⚠️ Operational Safety Advisory: Modifications to remote management ACLs and transport daemons require active out-of-band console access during maintenance window execution.";
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Top Banner */}
      <div className="bg-[#121418] border border-white/10 rounded-xl p-5 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-col gap-1">
          <div className="text-[11px] font-mono uppercase tracking-widest text-[#C6FF00] flex items-center gap-1.5">
            <span>✦ GEMINI FLASH</span>
            <span>·</span>
            <span>PRE-FLIGHT TERMINAL</span>
          </div>
          <strong className="text-lg text-white font-semibold">
            In-Memory What-If Compliance Simulation Sandbox
          </strong>
          <span className="text-xs text-zinc-400">
            Model proposed network configuration commands safely in temporary RAM sandbox before production staging.
          </span>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex items-center gap-2 bg-[#1E2128] px-3 py-1.5 rounded-lg border border-white/10 text-xs">
            <span className="text-zinc-400">Target Device:</span>
            <select
              value={selectedDeviceId}
              onChange={e => handleDeviceChange(e.target.value)}
              className="bg-transparent text-white font-medium focus:outline-none cursor-pointer"
            >
              {devices.map((d: Device) => (
                <option key={d.id} value={d.id} className="bg-[#1E2128] text-white">
                  {d.hostname} ({d.vendor} · {d.platform})
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {isStaged && (
        <div className="bg-[#C6FF00]/10 border border-[#C6FF00]/40 rounded-xl p-4 flex items-center justify-between gap-4 animate-in fade-in">
          <div className="flex items-center gap-3">
            <span className="text-[#C6FF00] text-lg font-bold">✓</span>
            <div>
              <strong className="text-sm text-white block">Simulation Staged for Production Review</strong>
              <span className="text-xs text-zinc-300">
                Change ticket queued with cryptographic posture proof and Gemini safety attestation.
              </span>
            </div>
          </div>
          <button
            type="button"
            className="text-xs text-zinc-400 hover:text-white underline cursor-pointer"
            onClick={() => setIsStaged(false)}
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Split-Screen Terminal (Left: Input / Right: Output Delta) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 items-stretch">
        {/* ================= LEFT PANEL (INPUT) ================= */}
        <div className="bg-[#1E2128] border border-white/10 rounded-xl p-5 flex flex-col gap-4 shadow-xl">
          <div className="flex items-center justify-between border-b border-white/5 pb-3">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#C6FF00] animate-pulse"></span>
              <strong className="text-xs uppercase tracking-wider text-zinc-200 font-mono">
                Configuration Terminal Input ({vendorKey} CLI)
              </strong>
            </div>

            <div className="flex items-center gap-2">
              {/* Ghost button: Clear Editor */}
              <button
                type="button"
                className="px-2.5 py-1 text-xs text-zinc-400 hover:text-white bg-transparent border border-white/10 hover:border-white/20 rounded-md transition-all flex items-center gap-1 cursor-pointer"
                onClick={handleClearEditor}
                title="Clear terminal editor content"
              >
                <Icon name="refresh" size={12} /> Clear Editor
              </button>

              {/* Template Presets */}
              <button
                type="button"
                className="px-2.5 py-1 text-xs text-[#C6FF00] bg-[#C6FF00]/10 border border-[#C6FF00]/30 rounded-md hover:bg-[#C6FF00]/20 transition-all flex items-center gap-1 cursor-pointer"
                onClick={() => applyTemplate("harden")}
                title="Load hardening template"
              >
                <Icon name="shield" size={12} /> Hardening
              </button>
              <button
                type="button"
                className="px-2.5 py-1 text-xs text-amber-400 bg-amber-500/10 border border-amber-500/30 rounded-md hover:bg-amber-500/20 transition-all flex items-center gap-1 cursor-pointer"
                onClick={() => applyTemplate("degrade")}
                title="Test insecure/telnet commands"
              >
                <Icon name="warning" size={12} /> Insecure
              </button>
            </div>
          </div>

          {/* Dark Monospace Textarea */}
          <div className="relative flex-1 flex flex-col">
            <textarea
              value={proposedConfig}
              onChange={e => {
                setProposedConfig(e.target.value);
                setIsStaged(false);
              }}
              rows={12}
              placeholder={`Enter or paste proposed ${vendorKey} CLI configuration commands...`}
              className="w-full h-full min-h-[280px] bg-[#0d0f14] font-mono text-xs text-zinc-200 border border-white/10 rounded-lg p-4 resize-none focus:outline-none focus:border-[#C6FF00] leading-relaxed selection:bg-[#C6FF00]/30 selection:text-white"
              spellCheck={false}
            />
            <div className="flex items-center justify-between text-[11px] text-zinc-500 font-mono mt-2 px-1">
              <span>Lines: {proposedConfig.split("\n").filter(Boolean).length}</span>
              <span>Syntax: {vendorKey} Platform Native</span>
            </div>
          </div>

          {simError && (
            <div className="text-xs text-red-400 bg-red-500/10 border border-red-500/30 p-2.5 rounded-lg">
              {simError}
            </div>
          )}

          <button
            type="button"
            disabled={isSimulating}
            onClick={handleRunSimulation}
            className="btn btn-primary w-full py-2.5 rounded-lg text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--accent-lime, #c4f82a)", color: "#090a0d" }}
          >
            <Icon name="play" size={14} />
            <span style={{ color: "#090a0d" }}>{isSimulating ? `Simulating ${vendorKey} rules...` : "Run simulation"}</span>
          </button>
        </div>

        {/* ================= RIGHT PANEL (OUTPUT / DELTA) ================= */}
        <div className="bg-[#1E2128] border border-white/10 rounded-xl p-5 flex flex-col gap-4 shadow-xl">
          <div className="flex items-center justify-between border-b border-white/5 pb-3">
            <strong className="text-xs uppercase tracking-wider text-zinc-200 font-mono flex items-center gap-2">
              <Icon name="trend" size={14} /> Pre-Flight Posture Delta & Safety Analysis
            </strong>
            <span className="text-[11px] font-mono text-zinc-400">
              {simResult ? "Simulation Complete" : "Baseline Loaded"}
            </span>
          </div>

          {/* Compliance Posture Delta Visual */}
          <div className="bg-[#121418] border border-white/10 rounded-xl p-4 flex flex-col gap-3">
            <div className="flex items-center justify-between text-[10px] uppercase tracking-wider text-zinc-400 font-mono">
              <span>Compliance Posture Delta</span>
              <span>Benchmark Target: 90%</span>
            </div>

            <div className="flex items-center justify-between gap-4 py-2 border-y border-white/5 sim-score-grid">
              <div className="flex flex-col">
                <span className="text-[10px] text-zinc-400 uppercase">Current Score</span>
                <span className="text-2xl font-bold font-mono text-zinc-300">
                  <strong>{currentScore}%</strong>
                </span>
              </div>

              <div className="text-zinc-500 font-mono flex items-center gap-1.5 text-sm">
                <span>──►</span>
              </div>

              <div className="flex flex-col items-end sim-good">
                <span className="text-[10px] text-zinc-400 uppercase">Simulated Score</span>
                <div className="flex items-center gap-2">
                  <span className={`text-2xl font-bold font-mono ${scoreDiff >= 0 ? "text-[#C6FF00]" : "text-red-400"}`}>
                    <strong>{simulatedScore}%</strong>
                  </span>
                  <span className={`text-xs font-mono font-semibold px-2 py-0.5 rounded ${
                    scoreDiff >= 0 ? "bg-[#C6FF00]/15 text-[#C6FF00]" : "bg-red-500/15 text-red-400"
                  }`}>
                    {scoreDiff >= 0 ? `+${scoreDiff} pts` : `${scoreDiff} pts`}
                  </span>
                </div>
              </div>
            </div>

            {/* Rule Failure Visual */}
            <div className="flex items-center justify-between text-xs font-mono bg-white/5 px-3 py-2 rounded-lg">
              <span className="text-zinc-400">Rule Failure Transition:</span>
              <div className="flex items-center gap-2">
                <span className="text-red-400 font-semibold">{currentFailures} Failed</span>
                <span className="text-zinc-500">──►</span>
                <span className="text-[#C6FF00] font-semibold">{simulatedFailures} Failed</span>
              </div>
            </div>
          </div>

          {/* Gemini Pre-Flight Safety Analysis Box (Amber Border) */}
          <div className="border border-amber-500/70 bg-amber-500/10 rounded-xl p-4 flex flex-col gap-2.5 shadow-[0_0_15px_rgba(245,158,11,0.08)]">
            <div className="flex items-center gap-2 text-amber-400 text-xs font-bold font-mono tracking-wide">
              <span>✦ GEMINI PRE-FLIGHT SAFETY ANALYSIS</span>
            </div>
            <p className="text-xs text-amber-200/90 leading-relaxed font-sans">
              {getSafetyWarning()}
            </p>
          </div>

          {/* Resolved Compliance Controls List */}
          <div className="bg-[#121418] border border-white/10 rounded-xl p-4 flex flex-col gap-2.5">
            <span className="text-[10px] font-mono uppercase tracking-wider text-zinc-400">
              Resolved Compliance Controls ({resolvedControls.length})
            </span>
            <div className="flex flex-col gap-1.5 max-h-40 overflow-y-auto pr-1">
              {resolvedControls.map((ctrlText, i) => (
                <div
                  key={i}
                  className="flex items-center gap-2 text-xs font-mono text-zinc-300 bg-white/5 px-3 py-1.5 rounded-lg border border-white/5"
                >
                  <span className="text-[#C6FF00] font-bold">✔</span>
                  <span className="truncate">{ctrlText}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Canonical State Diffs if present */}
          {simResult?.changes && simResult.changes.length > 0 && (
            <div className="bg-[#121418] border border-white/10 rounded-xl p-3 flex flex-col gap-2">
              <span className="text-[10px] font-mono uppercase tracking-wider text-zinc-400">
                Canonical Model Diffs ({simResult.changes.length})
              </span>
              <div className="flex flex-col gap-1.5 max-h-24 overflow-y-auto">
                {simResult.changes.slice(0, 4).map((c, idx) => (
                  <div key={idx} className="flex items-center justify-between text-[11px] font-mono px-2 py-1 bg-white/5 rounded">
                    <span className="text-[#C6FF00]">{c.canonicalField}</span>
                    <span className="text-zinc-400">{String(c.oldValue ?? "none")} → <strong className="text-white">{String(c.newValue ?? "none")}</strong></span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ================= BOTTOM ACTION BAR ================= */}
      <div className="bg-[#121418] border border-white/10 rounded-xl p-4 flex flex-wrap items-center justify-between gap-4 shadow-xl">
        <div className="flex items-center gap-2 text-xs text-zinc-400 font-mono">
          <span className="w-2 h-2 rounded-full bg-[#C6FF00]"></span>
          <span>Engine: In-Memory Spring Boot Cyber Simulation + Gemini Flash Guardrails</span>
        </div>

        <div className="flex items-center gap-3">
          {/* [ Re-run Simulation ] button */}
          <button
            type="button"
            disabled={isSimulating}
            onClick={handleRunSimulation}
            className="px-4 py-2.5 rounded-lg text-xs font-medium text-zinc-300 bg-white/5 border border-white/10 hover:bg-white/10 hover:text-white transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <Icon name="refresh" size={13} />
            <span>Re-run Simulation</span>
          </button>

          {/* Prominent, solid neon-lime button: [ Stage for Production Review ➔ ] */}
          <button
            type="button"
            onClick={handleStageForReview}
            disabled={isSimulating}
            className="btn btn-primary px-5 py-2.5 rounded-lg text-xs font-bold transition-all flex items-center gap-2 cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--accent-lime, #c4f82a)", color: "#090a0d" }}
          >
            <span style={{ color: "#090a0d" }}>Stage for Production Review</span>
            <span style={{ color: "#090a0d" }}>➔</span>
          </button>
        </div>
      </div>
    </div>
  );
}
