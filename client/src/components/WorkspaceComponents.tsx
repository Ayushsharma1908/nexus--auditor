import { Link } from "wouter";
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { ReactNode } from "react";
import type {
  AuditStatus,
  DeviceStatus,
  FindingStatus,
  Severity,
} from "@/types";
import { Icon } from "@/components/Icon";

export function Panel({
  title,
  eyebrow,
  action,
  children,
  className = "",
}: {
  title?: string;
  eyebrow?: string;
  action?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      {(title || eyebrow || action) && (
        <div className="panel-head">
          <div>
            {eyebrow && <div className="eyebrow">{eyebrow}</div>}
            {title && <h2 className="panel-title">{title}</h2>}
          </div>
          {action}
        </div>
      )}
      {children}
    </section>
  );
}

export function Button({
  children,
  variant = "primary",
  onClick,
  type = "button",
  className = "",
  disabled = false,
}: {
  children: ReactNode;
  variant?: "primary" | "secondary" | "ghost" | "danger";
  onClick?: () => void;
  type?: "button" | "submit";
  className?: string;
  disabled?: boolean;
}) {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`btn btn-${variant} ${className}`}
    >
      {children}
    </button>
  );
}

export function StatusBadge({
  value,
}: {
  value: Severity | FindingStatus | AuditStatus | DeviceStatus | string;
}) {
  const normalized = value.toLowerCase().replaceAll("_", " ");
  const tone =
    normalized.includes("critical") ||
    normalized.includes("fail") ||
    normalized === "open"
      ? "red"
      : normalized.includes("high") ||
          normalized.includes("risk") ||
          normalized.includes("running") ||
          normalized.includes("checking") ||
          normalized.includes("review") ||
          normalized.includes("planned")
        ? "amber"
        : normalized.includes("unknown") ||
            normalized.includes("queued") ||
            normalized.includes("parsing") ||
            normalized.includes("normalizing")
          ? "blue"
          : normalized.includes("resolved") ||
              normalized.includes("pass") ||
              normalized.includes("healthy") ||
              normalized.includes("completed") ||
              normalized.includes("approved") ||
              normalized.includes("ready") ||
              normalized.includes("decreased")
            ? "green"
            : "gray";
  return (
    <span className={`status status-${tone}`}>
      <span className="status-dot" />
      {value.replaceAll("_", " ")}
    </span>
  );
}

export function Metric({
  label,
  value,
  note,
  tone = "default",
}: {
  label: string;
  value: string | number;
  note?: string;
  tone?: "default" | "good" | "warn" | "bad";
}) {
  const isHashOrLong =
    typeof value === "string" &&
    (value.startsWith("sha256:") || value.length > 20);
  return (
    <div className="metric">
      <div className="metric-label">{label}</div>
      <div
        className={`metric-value metric-${tone} ${isHashOrLong ? "metric-mono-long" : ""}`}
        title={typeof value === "string" ? value : undefined}
      >
        {value}
      </div>
      {note && <div className="metric-note">{note}</div>}
    </div>
  );
}

export function ProgressBar({
  value,
  tone = "green",
}: {
  value: number;
  tone?: "green" | "blue" | "amber" | "red";
}) {
  return (
    <div className="progress-track" aria-label={`${value}%`}>
      <div
        className={`progress-fill fill-${tone}`}
        style={{ width: `${value}%` }}
      />
    </div>
  );
}

export function CircularRing({
  value = 81,
  size = 62,
  strokeWidth = 6,
  color = "#c4f82a",
}: {
  value?: number;
  size?: number;
  strokeWidth?: number;
  color?: string;
}) {
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (value / 100) * circumference;

  return (
    <div
      style={{
        width: size,
        height: size,
        position: "relative",
        display: "grid",
        placeItems: "center",
      }}
    >
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        style={{ transform: "rotate(-90deg)" }}
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="#1c1f28"
          strokeWidth={strokeWidth}
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          strokeLinecap="round"
        />
      </svg>
      <div
        style={{
          position: "absolute",
          inset: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <strong
          style={{
            fontSize: "16px",
            fontWeight: 700,
            color: "#f4f5f7",
            fontFamily: "Jost",
          }}
        >
          {value}%
        </strong>
      </div>
    </div>
  );
}

export function CircularDonut({
  value = 23,
  label = "findings",
  segments = [
    { name: "Critical", count: 2, color: "#ef4444" },
    { name: "High", count: 8, color: "#f59e0b" },
    { name: "Medium", count: 13, color: "#22c55e" },
    { name: "Low", count: 0, color: "#c4f82a" },
  ],
  size = 140,
  strokeWidth = 14,
}: {
  value?: number;
  label?: string;
  segments?: { name: string; count: number; color: string }[];
  size?: number;
  strokeWidth?: number;
}) {
  const total = segments.reduce((sum, s) => sum + s.count, 0) || 1;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  let accumulated = 0;
  const renderedSegments = segments
    .filter(s => s.count > 0)
    .map(segment => {
      const strokeDasharray = `${(segment.count / total) * circumference} ${circumference}`;
      const strokeDashoffset = -accumulated;
      accumulated += (segment.count / total) * circumference;
      return {
        ...segment,
        strokeDasharray,
        strokeDashoffset,
      };
    });

  return (
    <div
      className="donut-container"
      style={{ width: size, height: size, position: "relative" }}
    >
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${size} ${size}`}
        style={{ transform: "rotate(-90deg)" }}
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="#1c1f27"
          strokeWidth={strokeWidth}
        />
        {renderedSegments.map(segment => (
          <circle
            key={segment.name}
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={segment.color}
            strokeWidth={strokeWidth}
            strokeDasharray={segment.strokeDasharray}
            strokeDashoffset={segment.strokeDashoffset}
            strokeLinecap="round"
          />
        ))}
      </svg>
      <div
        className="donut-center"
        style={{
          position: "absolute",
          inset: 0,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          pointerEvents: "none",
        }}
      >
        <strong
          style={{
            fontSize: "30px",
            fontWeight: 700,
            color: "#f4f5f7",
            lineHeight: 1,
            fontFamily: "Jost",
            letterSpacing: "-0.04em",
          }}
        >
          {value}
        </strong>
        <span
          style={{
            fontSize: "10px",
            color: "#8b909e",
            textTransform: "uppercase",
            letterSpacing: "0.08em",
            marginTop: "4px",
            fontFamily: "Jost",
          }}
        >
          {label}
        </span>
      </div>
    </div>
  );
}

export function Sparkline({
  data,
  dataKey = "score",
  color = "#c4f82a",
}: {
  data: Record<string, string | number>[];
  dataKey?: string;
  color?: string;
}) {
  return (
    <div className="sparkline">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <Line
            type="monotone"
            dataKey={dataKey}
            stroke={color}
            strokeWidth={2}
            dot={false}
          />
          <YAxis hide domain={[0, "dataMax + 10"]} />
          <XAxis hide dataKey="month" />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export function ComplianceLineChart({
  data,
  color = "#c4f82a",
  label = "Compliance percentage trend",
}: {
  data: { month: string; score: number }[];
  color?: string;
  label?: string;
}) {
  return (
    <div className="chart-wrap" role="img" aria-label={label}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart
          data={data}
          margin={{ top: 8, right: 8, left: -24, bottom: 0 }}
        >
          <defs>
            <linearGradient
              id={`area-${color.replace("#", "")}`}
              x1="0"
              y1="0"
              x2="0"
              y2="1"
            >
              <stop offset="0%" stopColor={color} stopOpacity={0.22} />
              <stop offset="100%" stopColor={color} stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid
            stroke="#1b1f28"
            strokeDasharray="3 3"
            vertical={false}
          />
          <XAxis
            dataKey="month"
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <YAxis
            domain={[50, 100]}
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <Tooltip
            contentStyle={{
              background: "#13151b",
              border: "1px solid #20242e",
              borderRadius: 10,
              boxShadow: "0 10px 30px rgba(0,0,0,.6)",
              fontSize: 12,
              fontFamily: "Jost",
              color: "#f4f5f7",
            }}
          />
          <Area
            type="monotone"
            dataKey="score"
            stroke={color}
            strokeWidth={2.5}
            fill={`url(#area-${color.replace("#", "")})`}
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}

export function BarChartCard({
  data,
  label = "Bar chart",
}: {
  data: { name: string; count: number; tone?: string }[];
  label?: string;
}) {
  return (
    <div className="chart-wrap" role="img" aria-label={label}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={data}
          margin={{ top: 8, right: 8, left: -26, bottom: 0 }}
        >
          <CartesianGrid
            stroke="#1b1f28"
            strokeDasharray="3 3"
            vertical={false}
          />
          <XAxis
            dataKey="name"
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 10, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <YAxis
            allowDecimals={false}
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <Tooltip
            contentStyle={{
              background: "#13151b",
              border: "1px solid #20242e",
              borderRadius: 10,
              fontSize: 12,
              fontFamily: "Jost",
              color: "#f4f5f7",
            }}
          />
          <Bar
            dataKey="count"
            fill="#c4f82a"
            radius={[4, 4, 0, 0]}
            maxBarSize={28}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

export function RiskLineChart({
  data,
}: {
  data: { month: string; score: number }[];
}) {
  return (
    <div className="chart-wrap" role="img" aria-label="Overall risk trend">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart
          data={data}
          margin={{ top: 8, right: 8, left: -24, bottom: 0 }}
        >
          <CartesianGrid
            stroke="#1b1f28"
            strokeDasharray="3 3"
            vertical={false}
          />
          <XAxis
            dataKey="month"
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <YAxis
            domain={[0, 80]}
            tickLine={false}
            axisLine={false}
            tick={{ fontSize: 11, fill: "#8b909e", fontFamily: "Jost" }}
          />
          <Tooltip
            contentStyle={{
              background: "#13151b",
              border: "1px solid #20242e",
              borderRadius: 10,
              fontSize: 12,
              fontFamily: "Jost",
              color: "#f4f5f7",
            }}
          />
          <Line
            type="monotone"
            dataKey="score"
            stroke="#c4f82a"
            strokeWidth={2.5}
            dot={{ r: 3, fill: "#c4f82a", strokeWidth: 0 }}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

export function EmptyState({
  title = "No records found",
  detail = "Try adjusting your filters or upload a new configuration.",
}: {
  title?: string;
  detail?: string;
}) {
  return (
    <div className="empty-state">
      <Icon name="layers" size={18} />
      <strong>{title}</strong>
      <span>{detail}</span>
    </div>
  );
}

export function Notice({
  tone = "success",
  children,
}: {
  tone?: "success" | "error" | "info";
  children: ReactNode;
}) {
  return (
    <div className={`notice notice-${tone}`}>
      <span className="notice-marker" />
      {children}
    </div>
  );
}

export function LinkButton({
  href,
  children,
  variant = "secondary",
  className = "",
}: {
  href: string;
  children: ReactNode;
  variant?: "primary" | "secondary" | "ghost";
  className?: string;
}) {
  return (
    <Link href={href} className={`btn btn-${variant} ${className}`}>
      {children}
    </Link>
  );
}

export function TableHeader({ children }: { children: ReactNode }) {
  return <div className="table-header">{children}</div>;
}
