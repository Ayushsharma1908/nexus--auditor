import type { SVGProps } from "react";

type IconName =
  | "grid"
  | "router"
  | "file"
  | "clipboard"
  | "warning"
  | "trend"
  | "spark"
  | "report"
  | "sliders"
  | "search"
  | "plus"
  | "arrow"
  | "chevron"
  | "chevronLeft"
  | "chevronRight"
  | "filter"
  | "download"
  | "menu"
  | "close"
  | "check"
  | "play"
  | "clock"
  | "layers"
  | "terminal"
  | "user"
  | "bell"
  | "shield"
  | "external"
  | "panelOpen"
  | "panelClose"
  | "eye"
  | "eyeOff"
  | "logOut"
  | "refresh";

const paths: Record<IconName, React.ReactNode> = {
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </>
  ),
  router: (
    <>
      <rect x="3" y="6" width="18" height="12" rx="2" />
      <path d="M7 18v3m10-3v3M8 11h.01M12 11h.01M16 11h.01M8 14h8" />
    </>
  ),
  file: (
    <>
      <path d="M6 3h8l4 4v14H6z" />
      <path d="M14 3v5h5M9 13h6M9 17h6" />
    </>
  ),
  clipboard: (
    <>
      <path d="M9 4h6l1 2h3v15H5V6h3z" />
      <path d="M9 4V3h6v1M8 12l2 2 5-5" />
    </>
  ),
  warning: (
    <>
      <path d="m12 4 9 16H3z" />
      <path d="M12 9v5M12 17h.01" />
    </>
  ),
  trend: (
    <>
      <path d="M4 17 9 12l3 3 7-8" />
      <path d="M15 7h4v4" />
    </>
  ),
  spark: (
    <>
      <path d="m12 3 1.4 5.6L19 10l-5.6 1.4L12 17l-1.4-5.6L5 10l5.6-1.4z" />
      <path d="m19 16 .6 2.4L22 19l-2.4.6L19 22l-.6-2.4L16 19l2.4-.6z" />
    </>
  ),
  report: (
    <>
      <path d="M5 3h10l4 4v14H5z" />
      <path d="M15 3v5h4M8 12h8M8 16h5" />
    </>
  ),
  sliders: (
    <>
      <path d="M4 6h16M4 12h16M4 18h16" />
      <circle cx="9" cy="6" r="2" />
      <circle cx="15" cy="12" r="2" />
      <circle cx="11" cy="18" r="2" />
    </>
  ),
  search: (
    <>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m16 16 5 5" />
    </>
  ),
  plus: (
    <>
      <path d="M12 5v14M5 12h14" />
    </>
  ),
  arrow: (
    <>
      <path d="M5 12h13M13 7l5 5-5 5" />
    </>
  ),
  chevron: <path d="m9 6 6 6-6 6" />,
  chevronLeft: <path d="m15 18-6-6 6-6" />,
  chevronRight: <path d="m9 18 6-6-6-6" />,
  panelOpen: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M9 3v18M13 9l3 3-3 3" />
    </>
  ),
  panelClose: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M9 3v18M16 15l-3-3 3-3" />
    </>
  ),
  filter: (
    <>
      <path d="M4 5h16l-6 7v6l-4 2v-8z" />
    </>
  ),
  download: (
    <>
      <path d="M12 3v12M7 11l5 5 5-5M5 21h14" />
    </>
  ),
  menu: (
    <>
      <path d="M4 7h16M4 12h16M4 17h16" />
    </>
  ),
  close: (
    <>
      <path d="m6 6 12 12M18 6 6 18" />
    </>
  ),
  check: <path d="m5 12 4 4L19 7" />,
  play: <path d="m8 5 11 7-11 7z" />,
  clock: (
    <>
      <circle cx="12" cy="12" r="8" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  layers: (
    <>
      <path d="m12 3 9 5-9 5-9-5zM3 12l9 5 9-5M3 16l9 5 9-5" />
    </>
  ),
  terminal: (
    <>
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <path d="m7 9 3 3-3 3M13 15h4" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="3" />
      <path d="M5 20c.7-3.2 3-5 7-5s6.3 1.8 7 5" />
    </>
  ),
  bell: (
    <>
      <path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </>
  ),
  shield: (
    <>
      <path d="M12 3 19 6v5c0 5-3 8-7 10-4-2-7-5-7-10V6z" />
      <path d="m9 12 2 2 4-4" />
    </>
  ),
  external: (
    <>
      <path d="M14 4h6v6M20 4l-9 9" />
      <path d="M18 13v6H5V6h6" />
    </>
  ),
  eye: (
    <>
      <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </>
  ),
  eyeOff: (
    <>
      <path d="M9.88 9.88a3 3 0 1 0 4.24 4.24M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61M2 2l20 20" />
    </>
  ),
  logOut: (
    <>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </>
  ),
  refresh: (
    <>
      <path d="M21 2v6h-6M3 12a9 9 0 0 1 15.5-6.36L21 8M3 22v-6h6M21 12a9 9 0 0 1-15.5 6.36L3 16" />
    </>
  ),
};

export function Icon({
  name,
  size = 16,
  strokeWidth = 1.7,
  className = "",
}: {
  name: IconName;
  size?: number;
  strokeWidth?: number;
  className?: string;
} & SVGProps<SVGSVGElement>) {
  return (
    <svg
      aria-hidden="true"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      {paths[name]}
    </svg>
  );
}
