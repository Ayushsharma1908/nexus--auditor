export type Severity = "Critical" | "High" | "Medium" | "Low";

export type FindingStatus =
  | "OPEN"
  | "ACKNOWLEDGED"
  | "IN_REVIEW"
  | "REMEDIATION_PLANNED"
  | "RESOLVED"
  | "FALSE_POSITIVE";

export type AuditStatus =
  | "COMPLETED"
  | "CHECKING"
  | "RUNNING"
  | "FAILED"
  | "QUEUED"
  | "PARSING"
  | "NORMALIZING";

export type DeviceStatus = "Healthy" | "At risk" | "Critical" | "Unknown";

export interface Device {
  id: string;
  hostname: string;
  vendor: string;
  platform: string;
  ip: string;
  model: string;
  osVersion: string;
  environment: string;
  location: string;
  status: DeviceStatus;
  lastAudit: string;
  risk: number;
  compliance: number;
  criticality: "Critical" | "High" | "Medium" | "Low";
  findings: number;
}

export interface Configuration {
  id: string;
  deviceId: string;
  version: number;
  filename: string;
  uploadedBy: string;
  uploadedAt: string;
  size: string;
  status: string;
  hash: string;
  lines: string[];
}

export interface Audit {
  id: string;
  deviceId: string;
  configurationId: string;
  date: string;
  createdAt: string;
  duration: string;
  findings: number;
  frameworks: string[];
  compliance: number;
  status: AuditStatus;
}

export interface Finding {
  id: string;
  deviceId: string;
  auditId: string;
  control: string;
  title: string;
  severity: Severity;
  status: FindingStatus;
  line: number;
  framework: string;
  createdAt: string;
  rule: string;
  description: string;
  expected: string;
  actual: string;
  impact: string;
  canonicalField: string;
  remediation: string[];
}

export interface Framework {
  id: string;
  name: string;
  version: string;
  category: string;
  mappedRules: number;
  coverage: number;
  controls: number;
  note: string;
}

export interface DriftEvent {
  id: string;
  deviceId: string;
  version: number;
  date: string;
  change: string;
  impact: "Increased" | "Decreased";
  controls: string[];
  riskBefore: number;
  riskAfter: number;
  finding?: string;
}

export interface AiMapping {
  id: string;
  syntax: string;
  vendor: string;
  confidence: number;
  canonicalField: string;
  suggestedValue: string;
  reason: string;
  rationale?: string;
  provider?: "GEMINI" | "FALLBACK_STUB" | string;
  latencyText?: string;
  latencyMs?: number;
  status: "Needs review" | "Approved" | "Rejected";
  reviewer: string;
  date: string;
}

export interface Report {
  id: string;
  title: string;
  type: string;
  deviceId: string;
  auditId: string;
  date: string;
  status: "Ready" | "Processing" | "Archived";
  compliance: number;
}

export interface DashboardData {
  riskTrend: { month: string; score: number }[];
  complianceTrend: { month: string; score: number }[];
  frameworkScores: { label: string; score: number; name: string }[];
}

export interface Control {
  id: string;
  frameworkId?: string;
  controlId?: string;
  controlCode?: string;
  title: string;
  description?: string;
  family?: string;
  category?: string;
  version?: string;
  severity?: string;
  remediationGuidance?: string;
  sourceReference?: string;
  sourceUrl?: string;
  status?: string;
  ruleIds?: string[];
  rules?: number;
  evidence?: number;
}

export interface WhatIfSimulationResult {
  deviceId?: string;
  baseConfigurationVersionId?: string;
  status?: string;
  before?: {
    complianceScore?: number | null;
    riskScore?: number | null;
    passedControls?: number | null;
    failedControls?: number | null;
    passedCount?: number;
    failedCount?: number;
  };
  after?: {
    complianceScore?: number | null;
    riskScore?: number | null;
    passedControls?: number | null;
    failedControls?: number | null;
    passedCount?: number;
    failedCount?: number;
  };
  findingDelta?: number;
  riskDelta?: number;
  impact?: string;
  changes?: Array<{
    canonicalField: string;
    oldValue: unknown;
    newValue: unknown;
    classification: string;
  }>;
  affectedControlIds?: string[];
  affectedFindingIds?: string[];
  ruleResultsBefore?: Record<string, string>;
  ruleResultsAfter?: Record<string, string>;
}
