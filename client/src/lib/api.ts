/**
 * NEXUS-COMPLY API Client
 *
 * Typed fetch helpers for all backend endpoints.
 * Base URL is relative so Vite proxy handles `/api` → `http://localhost:8080`.
 * All responses follow the { data, requestId, timestamp } envelope.
 */

const rawBase = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim().replace(/\/$/, "");
const BASE = rawBase
  ? (rawBase.endsWith("/api/v1") ? rawBase : (rawBase.endsWith("/api") ? `${rawBase}/v1` : `${rawBase}/api/v1`))
  : "/api/v1";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: unknown,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const url = `${BASE}${path}`;
  const res = await fetch(url, {
    headers: { "Content-Type": "application/json", ...init?.headers },
    ...init,
  });

  const text = await res.text().catch(() => "");
  let json: unknown = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    throw new ApiError(
      res.status,
      text,
      `API returned non-JSON response from ${url}. Check if VITE_API_BASE_URL is set to your Render backend URL.`
    );
  }

  if (!res.ok) {
    throw new ApiError(res.status, json, `API ${res.status}: ${path}`);
  }
  // Unwrap the ApiResponse envelope { data: T }
  return ((json as Record<string, unknown>)?.data ?? json) as T;
}

// ────────────────────────────────────────────────────────────────
// Dashboard
// ────────────────────────────────────────────────────────────────
export const api = {
  dashboard: {
    summary: () => request<Record<string, unknown>>("/dashboard/summary"),
    compliance: () => request<Record<string, unknown>>("/dashboard/compliance"),
    findings: () => request<Record<string, unknown>>("/dashboard/findings"),
    risk: () => request<Record<string, unknown>>("/dashboard/risk"),
    drift: () => request<Record<string, unknown>>("/dashboard/drift"),
    activity: () => request<Record<string, unknown>>("/dashboard/activity"),
    frameworks: () => request<Record<string, unknown>>("/dashboard/frameworks"),
  },

  // ────────────────────────────────────────────────────────────────
  // Audits
  // ────────────────────────────────────────────────────────────────
  audits: {
    list: () => request<unknown[]>("/audits"),
    get: (id: string) => request<unknown>(`/audits/${id}`),
    create: (body: unknown) =>
      request<unknown>("/audits", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    status: (id: string) => request<unknown>(`/audits/${id}/status`),
    cancel: (id: string) =>
      request<unknown>(`/audits/${id}/cancel`, { method: "POST" }),
    rerun: (id: string) =>
      request<unknown>(`/audits/${id}/rerun`, { method: "POST" }),
    summary: (id: string) => request<unknown>(`/audits/${id}/summary`),
    frameworkResults: (id: string) =>
      request<unknown[]>(`/audits/${id}/framework-results`),
    findings: (id: string) => request<string[]>(`/audits/${id}/findings`),
    risk: (id: string) => request<unknown>(`/audits/${id}/risk`),
    report: (id: string) =>
      request<unknown>(`/audits/${id}/report`, { method: "POST" }),
  },

  // ────────────────────────────────────────────────────────────────
  // Findings
  // ────────────────────────────────────────────────────────────────
  findings: {
    list: (params?: {
      auditId?: string;
      severity?: string;
      status?: string;
      deviceId?: string;
    }) => {
      const q = new URLSearchParams(
        Object.entries(params ?? {}).filter(([, v]) => Boolean(v)) as [
          string,
          string,
        ][]
      ).toString();
      return request<unknown[]>(`/findings${q ? `?${q}` : ""}`);
    },
    get: (id: string) => request<unknown>(`/findings/${id}`),
    updateStatus: (id: string, status: string) =>
      request<unknown>(`/findings/${id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status }),
      }),
    updateSeverity: (id: string, severity: string) =>
      request<unknown>(`/findings/${id}/severity`, {
        method: "PATCH",
        body: JSON.stringify({ severity }),
      }),
    history: (id: string) => request<unknown[]>(`/findings/${id}/history`),
    related: (id: string) => request<unknown[]>(`/findings/${id}/related`),
    acknowledge: (id: string, note?: string) =>
      request<unknown>(`/findings/${id}/acknowledge`, {
        method: "POST",
        body: JSON.stringify({ note }),
      }),
    resolve: (id: string, note?: string) =>
      request<unknown>(`/findings/${id}/resolve`, {
        method: "POST",
        body: JSON.stringify({ note }),
      }),
    evidence: (id: string) => request<unknown[]>(`/findings/${id}/evidence`),
  },

  // ────────────────────────────────────────────────────────────────
  // Frameworks & Controls
  // ────────────────────────────────────────────────────────────────
  frameworks: {
    list: () => request<unknown[]>("/frameworks"),
    get: (id: string) => request<unknown>(`/frameworks/${id}`),
    controls: (id: string) => request<unknown[]>(`/frameworks/${id}/controls`),
    mappings: () => request<unknown[]>("/framework-mappings"),
  },
  controls: {
    get: (id: string) => request<unknown>(`/controls/${id}`),
    rules: (id: string) => request<unknown[]>(`/controls/${id}/rules`),
  },

  // ────────────────────────────────────────────────────────────────
  // Devices
  // ────────────────────────────────────────────────────────────────
  devices: {
    list: () => request<unknown[]>("/devices"),
    get: (id: string) => request<unknown>(`/devices/${id}`),
    create: (body: unknown) =>
      request<unknown>("/devices", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    update: (id: string, body: unknown) =>
      request<unknown>(`/devices/${id}`, {
        method: "PUT",
        body: JSON.stringify(body),
      }),
    delete: (id: string) =>
      request<unknown>(`/devices/${id}`, {
        method: "DELETE",
      }),
  },

  // ────────────────────────────────────────────────────────────────
  // Configurations
  // ────────────────────────────────────────────────────────────────
  configurations: {
    list: (deviceId?: string) =>
      request<unknown[]>(`/configurations${deviceId ? `?deviceId=${deviceId}` : ""}`),
    get: (id: string) => request<unknown>(`/configurations/${id}`),
    create: (body: unknown) =>
      request<unknown>("/configurations", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },


  // ────────────────────────────────────────────────────────────────
  // Drift
  // ────────────────────────────────────────────────────────────────
  drift: {
    list: (page = 0, size = 50) =>
      request<unknown>(`/drift?page=${page}&size=${size}`),
    get: (id: string) => request<unknown>(`/drift/${id}`),
    compare: (body: unknown) =>
      request<unknown>("/drift/compare", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },

  // ────────────────────────────────────────────────────────────────
  // Parser / Configurations
  // ────────────────────────────────────────────────────────────────
  parser: {
    jobs: () => request<unknown[]>("/cyber/parse-jobs"),
    job: (id: string) => request<unknown>(`/cyber/parse-jobs/${id}`),
  },

  // ────────────────────────────────────────────────────────────────
  // Compliance
  // ────────────────────────────────────────────────────────────────
  compliance: {
    evaluate: (body: unknown) =>
      request<unknown>("/compliance/evaluate", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    results: (auditId: string) =>
      request<unknown[]>(`/compliance/results?auditId=${auditId}`),
  },

  // ────────────────────────────────────────────────────────────────
  // Risk
  // ────────────────────────────────────────────────────────────────
  risk: {
    summary: () => request<unknown>("/risk/summary"),
    device: (id: string) => request<unknown>(`/risk/devices/${id}`),
  },

  // ────────────────────────────────────────────────────────────────
  // Reports
  // ────────────────────────────────────────────────────────────────
  reports: {
    list: () => request<unknown[]>("/reports"),
    get: (id: string) => request<unknown>(`/reports/${id}`),
    generate: (body: unknown) =>
      request<unknown>("/reports/generate", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },

  // ────────────────────────────────────────────────────────────────
  // AI Analyst
  // ────────────────────────────────────────────────────────────────
  ai: {
    mappings: () => request<unknown[]>("/ai/mappings"),
    approve: (id: string) =>
      request<unknown>(`/ai/mappings/${id}/approve`, { method: "POST" }),
    reject: (id: string) =>
      request<unknown>(`/ai/mappings/${id}/reject`, { method: "POST" }),
    inquire: (body: {
      question: string;
      syntax?: string;
      vendor?: string;
      canonicalField?: string;
      suggestedValue?: string;
    }) =>
      request<{ reply: string; model: string; provider: string }>("/ai/inquire", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    chat: (body: {
      userMessage: string;
      context?: Record<string, unknown>;
      history?: Array<{ role: "user" | "ai"; content: string }>;
    }) =>
      request<{ reply?: string; response?: string; content?: string }>("/ai/chat", {
        method: "POST",
        body: JSON.stringify(body),
      }),
  },

  // ────────────────────────────────────────────────────────────────
  // Simulations
  // ────────────────────────────────────────────────────────────────
  simulations: {
    run: (body: unknown) =>
      request<unknown>("/what-if/simulate", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    simulate: (body: unknown) =>
      request<unknown>("/what-if/simulate", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    list: () => request<unknown[]>("/simulations"),
  },
};
