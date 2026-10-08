/**
 * NEXUS-COMPLY Data Hooks
 *
 * React hooks that fetch live data from the backend.
 * On network failure they silently fall back to the local mock dataset,
 * keeping the UI functional during development without the backend running.
 *
 * Pattern:
 *   const { data, loading, error } = useAudits();
 */

import { useEffect, useState } from "react";
import { api } from "./api";
import type {
  AiMapping,
  Audit,
  Configuration,
  Control,
  DashboardData,
  Device,
  DriftEvent,
  Finding,
  Framework,
  Report,
  WhatIfSimulationResult,
} from "@/types";

// ---------- helpers ---------------------------------------------------------

function useApiData<T>(
  fetcher: () => Promise<T>,
  fallback: T,
  deps: unknown[] = []
): { data: T; loading: boolean; error: string | null } {
  const [data, setData] = useState<T>(fallback);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(
    () => {
      let cancelled = false;
      setLoading(true);
      fetcher()
        .then(result => {
          if (!cancelled) {
            let finalData: unknown = result;
            if (result && typeof result === "object" && "content" in (result as Record<string, unknown>)) {
              finalData = (result as unknown as { content: unknown }).content;
            }
            if (finalData !== null && finalData !== undefined) {
              setData(finalData as T);
            }
            setError(null);
          }
        })
        .catch((err: unknown) => {
          if (!cancelled) {
            console.warn("[useApiData] error fetching live data:", err);
            setData(fallback);
            setError(err instanceof Error ? err.message : String(err));
          }
        })
        .finally(() => {
          if (!cancelled) setLoading(false);
        });
      return () => {
        cancelled = true;
      };
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    deps
  );

  return { data, loading, error };
}

// ---------- public hooks ----------------------------------------------------

export function useAudits() {
  return useApiData<Audit[]>(
    () => api.audits.list() as Promise<Audit[]>,
    []
  );
}

export function useAudit(id: string) {
  return useApiData<Audit | undefined>(
    () => api.audits.get(id) as Promise<Audit>,
    undefined,
    [id]
  );
}

export function useFindings(params?: {
  auditId?: string;
  severity?: string;
  status?: string;
  deviceId?: string;
}) {
  const key = JSON.stringify(params ?? {});
  return useApiData<Finding[]>(
    () => api.findings.list(params) as Promise<Finding[]>,
    [],
    [key]
  );
}

export function useFinding(id: string) {
  return useApiData<Finding | undefined>(
    () => api.findings.get(id) as Promise<Finding>,
    undefined,
    [id]
  );
}

export function useDevices() {
  return useApiData<Device[]>(
    () => api.devices.list() as Promise<Device[]>,
    []
  );
}

export function useDevice(id: string) {
  return useApiData<Device | undefined>(
    () => api.devices.get(id) as Promise<Device>,
    undefined,
    [id]
  );
}

export function useConfigurations(deviceId?: string) {
  return useApiData<Configuration[]>(
    () => api.configurations.list(deviceId) as Promise<Configuration[]>,
    [],
    [deviceId]
  );
}


export function useDashboard() {
  // Merge several dashboard endpoints into one object matching DashboardData
  const [data, setData] = useState<DashboardData>({
    riskTrend: [],
    complianceTrend: [],
    frameworkScores: []
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    Promise.all([
      api.dashboard.compliance(),
      api.dashboard.risk(),
      api.dashboard.frameworks(),
    ])
      .then(([compliance, risk, frameworks]) => {
        if (!cancelled) {
          // Map backend aggregate shape to DashboardData type (best-effort)
          const riskTrend = (risk as Record<string, unknown>)?.riskTrend ?? [];
          const complianceTrend = (compliance as Record<string, unknown>)?.complianceTrend ?? [];
          const frameworkScores = (frameworks as Record<string, unknown>)?.frameworkScores ?? [];
          setData({
            riskTrend: riskTrend as DashboardData["riskTrend"],
            complianceTrend:
              complianceTrend as DashboardData["complianceTrend"],
            frameworkScores:
              frameworkScores as DashboardData["frameworkScores"],
          });
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          console.warn("[useDashboard] fetch failed:", err);
          setError(err instanceof Error ? err.message : String(err));
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return { data, loading, error };
}

export function useFrameworks() {
  return useApiData<Framework[]>(
    () => api.frameworks.list() as Promise<Framework[]>,
    []
  );
}

export function useDriftEvents(deviceId?: string) {
  const key = deviceId ?? "all";
  return useApiData<DriftEvent[]>(
    async () => {
      const result = (await api.drift.list()) as {
        content?: DriftEvent[];
        items?: DriftEvent[];
      };
      const items = result?.content ?? result?.items ?? (Array.isArray(result) ? result : []);
      return deviceId ? items.filter(e => e.deviceId === deviceId) : items;
    },
    [],
    [key]
  );
}

export function useReports() {
  return useApiData<Report[]>(
    async () => {
      const res = (await api.reports.list()) as { content?: Report[] } | Report[];
      return (res && "content" in res && Array.isArray(res.content)) ? res.content : (Array.isArray(res) ? res : []);
    },
    []
  );
}

export function useAiMappings() {
  return useApiData<AiMapping[]>(
    async () => {
      const res = (await api.ai.mappings()) as { content?: AiMapping[] } | AiMapping[];
      return (res && "content" in res && Array.isArray(res.content)) ? res.content : (Array.isArray(res) ? res : []);
    },
    []
  );
}

export function useFrameworkControls(frameworkId?: string) {
  const key = frameworkId ?? "all";
  return useApiData<Control[]>(
    async () => {
      if (!frameworkId) return [];
      try {
        const res = (await api.frameworks.controls(frameworkId)) as Control[];
        if (Array.isArray(res) && res.length > 0) return res;
      } catch {
        // Continue to fallback check
      }
      if (frameworkId.startsWith("FW-CIS") || frameworkId.includes("cis")) {
        try {
          const res = (await api.frameworks.controls("FW-CIS")) as Control[];
          if (Array.isArray(res) && res.length > 0) return res;
        } catch {
          // Continue
        }
      }
      return [];
    },
    [],
    [key]
  );
}


