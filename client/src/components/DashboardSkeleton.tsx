import React from "react";

export function SkeletonBlock({
  className = "",
  style,
}: {
  className?: string;
  style?: React.CSSProperties;
}) {
  return <div className={`skeleton-shimmer ${className}`} style={style} />;
}

export function DashboardSkeleton() {
  return (
    <div
      className="dashboard-skeleton-stack"
      aria-label="Loading dashboard data..."
      role="status"
    >
      {/* 4-Card Summary Grid Skeleton */}
      <div className="summary-grid">
        {/* Card 1: Total Devices */}
        <div className="summary-card card-devices skeleton-card-shell">
          <div className="summary-card-head">
            <SkeletonBlock className="h-3 w-24" />
            <SkeletonBlock className="h-5 w-5 rounded-full" />
          </div>
          <SkeletonBlock className="h-8 w-16 mt-2 mb-1" />
          <SkeletonBlock className="h-3 w-28 mb-3" />
          <div className="devices-mini-bars skeleton-bars-wrap">
            {[40, 65, 35, 80, 50, 90, 45].map((height, i) => (
              <div key={i} className="mini-bar-col">
                <div className="bar-track">
                  <div
                    className="bar-fill skeleton-lime-shimmer"
                    style={{ height: `${height}%` }}
                  />
                </div>
                <SkeletonBlock className="h-2.5 w-4 mt-1 mx-auto" />
              </div>
            ))}
          </div>
        </div>

        {/* Card 2: Overall Compliance */}
        <div className="summary-card card-compliance skeleton-card-shell">
          <div className="summary-card-head">
            <SkeletonBlock className="h-3 w-32" />
            <SkeletonBlock className="h-5 w-5 rounded-full" />
          </div>
          <div className="compliance-feature-content mt-3">
            <div className="skeleton-donut-circle skeleton-shimmer" />
            <div className="compliance-details">
              <SkeletonBlock className="h-8 w-20 mb-1" />
              <SkeletonBlock className="h-3 w-24 mb-1" />
              <SkeletonBlock className="h-2.5 w-28" />
            </div>
          </div>
        </div>

        {/* Card 3: Open Findings */}
        <div className="summary-card card-findings skeleton-card-shell">
          <div className="summary-card-head">
            <SkeletonBlock className="h-3 w-28" />
            <SkeletonBlock className="h-5 w-5 rounded-full" />
          </div>
          <SkeletonBlock className="h-8 w-14 mt-2 mb-1" />
          <SkeletonBlock className="h-3 w-40 mb-3" />
          <div className="severity-stack-inline">
            <SkeletonBlock className="h-5 w-20 rounded-full" />
            <SkeletonBlock className="h-5 w-16 rounded-full" />
            <SkeletonBlock className="h-5 w-20 rounded-full" />
          </div>
        </div>

        {/* Card 4: Overall Risk */}
        <div className="summary-card card-risk skeleton-card-shell">
          <div className="summary-card-head">
            <SkeletonBlock className="h-3 w-24" />
            <SkeletonBlock className="h-5 w-5 rounded-full" />
          </div>
          <SkeletonBlock className="h-8 w-14 mt-2" />
          <div className="risk-foot-row mt-2">
            <SkeletonBlock className="h-3 w-24" />
            <SkeletonBlock className="h-7 w-24 rounded" />
          </div>
        </div>
      </div>

      {/* Row 2: Analytical Chart + Donut Distribution Skeleton */}
      <div className="dashboard-content-grid">
        {/* Posture Analysis Chart Skeleton */}
        <div className="panel posture-analysis-card skeleton-panel">
          <div className="panel-header">
            <div>
              <SkeletonBlock className="h-2.5 w-48 mb-1.5" />
              <SkeletonBlock className="h-4.5 w-40" />
            </div>
            <div className="card-actions">
              <SkeletonBlock className="h-6 w-20 rounded-full" />
              <SkeletonBlock className="h-6 w-6 rounded-full" />
            </div>
          </div>
          <div className="panel-body">
            <div className="analysis-highlight">
              <SkeletonBlock className="h-10 w-24" />
              <div className="flex flex-col gap-1">
                <SkeletonBlock className="h-3.5 w-44" />
                <SkeletonBlock className="h-3 w-28" />
              </div>
            </div>

            {/* Shimmer Chart Simulation Box */}
            <div className="skeleton-chart-box">
              <div className="skeleton-chart-lines">
                <div className="chart-grid-line" />
                <div className="chart-grid-line" />
                <div className="chart-grid-line" />
                <div className="chart-grid-line" />
              </div>
              <div className="skeleton-wave-area skeleton-lime-shimmer" />
            </div>

            <div className="analysis-footer">
              <SkeletonBlock className="h-3 w-32" />
              <SkeletonBlock className="h-3 w-32" />
              <SkeletonBlock className="h-3 w-36 ml-auto" />
            </div>
          </div>
        </div>

        {/* Risk Distribution Donut Skeleton */}
        <div className="panel distribution-card skeleton-panel">
          <div className="panel-header">
            <div>
              <SkeletonBlock className="h-2.5 w-32 mb-1.5" />
              <SkeletonBlock className="h-4.5 w-36" />
            </div>
            <SkeletonBlock className="h-6 w-6 rounded-full" />
          </div>
          <div className="panel-body">
            <div className="distribution-body">
              <div className="skeleton-donut-big skeleton-shimmer" />
              <div className="distribution-legend">
                {[1, 2, 3, 4].map(item => (
                  <div key={item} className="flex items-center gap-2">
                    <SkeletonBlock className="h-2 w-2 rounded-full" />
                    <SkeletonBlock className="h-3 w-16" />
                    <SkeletonBlock className="h-3 w-6 ml-auto" />
                  </div>
                ))}
              </div>
            </div>
            <SkeletonBlock className="h-4 w-32 mt-4" />
          </div>
        </div>
      </div>

      {/* Row 3: Recent Audits & Critical Findings Skeleton */}
      <div className="dashboard-lists-grid">
        {/* Recent Audits Skeleton */}
        <div className="panel recent-audits-card skeleton-panel">
          <div className="panel-header">
            <div>
              <SkeletonBlock className="h-2.5 w-32 mb-1.5" />
              <SkeletonBlock className="h-4.5 w-28" />
            </div>
            <SkeletonBlock className="h-3 w-16" />
          </div>
          <div className="panel-body">
            <div className="audit-list">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="audit-list-row skeleton-row">
                  <div className="flex-1">
                    <SkeletonBlock className="h-3.5 w-32 mb-1.5" />
                    <SkeletonBlock className="h-2.5 w-52" />
                  </div>
                  <div className="audit-row-side">
                    <SkeletonBlock className="h-4 w-10" />
                    <SkeletonBlock className="h-5 w-16 rounded-full" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Critical Findings Skeleton */}
        <div className="panel critical-findings-card skeleton-panel">
          <div className="panel-header">
            <div>
              <SkeletonBlock className="h-2.5 w-44 mb-1.5" />
              <SkeletonBlock className="h-4.5 w-32" />
            </div>
            <SkeletonBlock className="h-3 w-20" />
          </div>
          <div className="panel-body">
            <div className="finding-mini-list">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="finding-mini skeleton-row">
                  <div className="flex-1">
                    <SkeletonBlock className="h-3.5 w-64 mb-1.5" />
                    <SkeletonBlock className="h-2.5 w-32" />
                  </div>
                  <SkeletonBlock className="h-5 w-16 rounded-full" />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Row 4: Framework Readiness Cards Skeleton */}
      <div className="framework-section skeleton-frameworks">
        <div className="section-heading">
          <div>
            <SkeletonBlock className="h-2.5 w-28 mb-1.5" />
            <SkeletonBlock className="h-5 w-44" />
          </div>
          <SkeletonBlock className="h-3 w-28" />
        </div>
        <div className="framework-editorial-grid">
          {[1, 2, 3, 4].map(i => (
            <div
              key={i}
              className="framework-editorial-card skeleton-editorial-card"
            >
              <SkeletonBlock className="h-2.5 w-20 mb-2" />
              <SkeletonBlock className="h-7 w-16 mb-3" />
              <SkeletonBlock className="h-1.5 w-full rounded mb-3" />
              <SkeletonBlock className="h-3 w-36" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
