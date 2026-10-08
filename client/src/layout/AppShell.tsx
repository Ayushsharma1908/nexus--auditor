import { useState, type ReactNode } from "react";
import { Link, useLocation } from "wouter";
import { Icon } from "@/components/Icon";
import { useAuth } from "@/contexts/AuthContext";
import { useDevices } from "@/lib/useApi";

const groups = [
  {
    label: "Overview",
    items: [{ label: "Dashboard", href: "/dashboard", icon: "grid" as const }],
  },
  {
    label: "Network",
    items: [
      { label: "Devices", href: "/devices", icon: "router" as const },
      {
        label: "Configurations",
        href: "/configurations",
        icon: "file" as const,
      },
    ],
  },
  {
    label: "Compliance",
    items: [
      { label: "Audits", href: "/audits", icon: "clipboard" as const },
      { label: "Findings", href: "/findings", icon: "warning" as const },
      { label: "Risk Center", href: "/risk", icon: "trend" as const },
    ],
  },
  {
    label: "Intelligence",
    items: [
      { label: "Drift Intelligence", href: "/drift", icon: "layers" as const },
      { label: "What-If Analysis", href: "/what-if", icon: "sliders" as const },
      { label: "AI Analyst", href: "/ai-analyst", icon: "spark" as const },
    ],
  },
  {
    label: "Governance",
    items: [
      { label: "Reports", href: "/reports", icon: "report" as const },
      {
        label: "Frameworks & Controls",
        href: "/frameworks",
        icon: "shield" as const,
      },
    ],
  },
];

const titles: Record<string, string> = {
  "/dashboard": "Dashboard",
  "/devices": "Devices",
  "/configurations": "Configurations",
  "/audits": "Audits",
  "/findings": "Findings",
  "/risk": "Risk Center",
  "/drift": "Drift Intelligence",
  "/what-if": "What-If Analysis",
  "/ai-analyst": "AI Analyst",
  "/reports": "Reports",
  "/frameworks": "Frameworks",
  "/settings": "Settings",
};

function isActive(path: string, href: string) {
  return (
    path === href || (href !== "/dashboard" && path.startsWith(`${href}/`))
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const [location, navigate] = useLocation();
  const [searchTerm, setSearchTerm] = useState("");
  const [isExpanded, setIsExpanded] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showAccount, setShowAccount] = useState(false);
  const [hoveredLabel, setHoveredLabel] = useState<string | null>(null);


  const deviceId = location.startsWith("/devices/")
    ? location.split("/devices/")[1]?.split("/")[0]
    : null;
  const { data: allDevices } = useDevices();
  const currentDevice = deviceId ? allDevices.find(d => d.id === deviceId) : null;
  const isDetailPage =
    /^\/(devices|configurations|audits|findings|reports|frameworks|controls)\/[^/]+/.test(
      location
    ) ||
    location === "/devices/new" ||
    location === "/audits/new";

  const { user: authUser, logout } = useAuth();
  const currentUser = authUser ?? { name: "Analyst", role: "User", avatar: "AN" };

  const currentTitle =
    titles[location] ??
    (currentDevice
      ? currentDevice.hostname
      : location.includes("/devices/")
        ? "Device details"
        : location.includes("/configurations/")
          ? "Configuration details"
          : location.includes("/audits/")
            ? "Audit details"
            : location.includes("/findings/")
              ? "Finding details"
              : location.includes("/reports/")
                ? "Report preview"
                : location.includes("/frameworks/") ||
                    location.includes("/controls/")
                  ? "Control detail"
                  : "Workspace");

  // What should display in the top header hover area
  const displayLabel = hoveredLabel || currentTitle;

  return (
    <div
      className={`app-shell ${isExpanded ? "layout-expanded" : "layout-collapsed"}`}
    >
      <aside
        className={`sidebar ${isExpanded ? "sidebar-expanded" : "sidebar-collapsed"} ${mobileOpen ? "sidebar-open" : ""}`}
      >
        <button
          className="mobile-close"
          onClick={() => setMobileOpen(false)}
          aria-label="Close navigation"
        >
          <Icon name="close" size={16} />
        </button>

        {/* Top Header / Toggle Action & Hover Name Display */}
        <div className="sidebar-header">
          <button
            type="button"
            className={`sidebar-toggle-btn ${isExpanded ? "toggle-active" : ""}`}
            onClick={() => setIsExpanded(prev => !prev)}
            onMouseEnter={() =>
              setHoveredLabel(isExpanded ? "Close Sidebar" : "Open Sidebar")
            }
            onMouseLeave={() => setHoveredLabel(null)}
            aria-label={isExpanded ? "Collapse sidebar" : "Open sidebar"}
            title={isExpanded ? "Collapse sidebar" : "Open sidebar"}
          >
            <div className="toggle-icon-wrap">
              <Icon name={isExpanded ? "panelClose" : "panelOpen"} size={18} />
            </div>
            {!isExpanded && (
              <div className="sidebar-tooltip">
                <span className="tooltip-title">Open navigation</span>
                <span className="tooltip-category">Toggle Sidebar</span>
              </div>
            )}
          </button>

          {isExpanded ? (
            <div className="workspace-branding">
              <img
                src="/logo.png"
                alt="Nexus"
                className="workspace-brand-logo"
              />
              <div className="workspace-meta">
                <div className="workspace-label">Workspace</div>
                <div className="workspace-name">
                  {hoveredLabel || "Network Assurance"}
                </div>
              </div>
            </div>
          ) : (
            /* Collapsed mode top display: shows name of what user is hovering */
            <div className="sidebar-top-hover-badge" title={displayLabel}>
              <span className="top-hover-text">{displayLabel}</span>
            </div>
          )}
        </div>

        {/* Navigation Group Items */}
        <nav className="nav-groups" aria-label="Primary navigation">
          {groups.map(group => (
            <div className="nav-group" key={group.label}>
              {isExpanded && <div className="nav-label">{group.label}</div>}
              <div className="nav-group-items">
                {group.items.map(item => {
                  const active = isActive(location, item.href);
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={() => setMobileOpen(false)}
                      onMouseEnter={() => setHoveredLabel(item.label)}
                      onMouseLeave={() => setHoveredLabel(null)}
                      aria-label={item.label}
                      className={`nav-item ${active ? "nav-item-active" : ""}`}
                    >
                      <div className="nav-item-icon-container">
                        <Icon name={item.icon} size={17} />
                      </div>
                      <span className="nav-item-label">{item.label}</span>

                      {!isExpanded && (
                        <div className="sidebar-tooltip">
                          <span className="tooltip-title">{item.label}</span>
                          <span className="tooltip-category">
                            {group.label}
                          </span>
                        </div>
                      )}
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </nav>

        {/* Sidebar Bottom (Settings) */}
        <div className="sidebar-bottom">
          <Link
            href="/settings"
            onClick={() => setMobileOpen(false)}
            onMouseEnter={() => setHoveredLabel("Settings")}
            onMouseLeave={() => setHoveredLabel(null)}
            aria-label="Settings"
            className={`nav-item ${isActive(location, "/settings") ? "nav-item-active" : ""}`}
          >
            <div className="nav-item-icon-container">
              <Icon name="sliders" size={17} />
            </div>
            <span className="nav-item-label">Settings</span>

            {!isExpanded && (
              <div className="sidebar-tooltip">
                <span className="tooltip-title">Settings</span>
                <span className="tooltip-category">Governance & Profile</span>
              </div>
            )}
          </Link>
        </div>
      </aside>

      {mobileOpen && (
        <button
          className="mobile-scrim"
          onClick={() => setMobileOpen(false)}
          aria-label="Close navigation overlay"
        />
      )}

      <main className="main-workspace">
        <header className="topbar">
          <div className="topbar-left">
            <div className="topbar-brand">
              <img
                src="/logo.png"
                alt="NEXUS-COMPLY"
                className="brand-logo-img"
              />
              <div>
                <div className="brand-name">NEXUS-COMPLY</div>
                <div className="brand-id">SIH26155</div>
              </div>
            </div>
            <button
              className="menu-button"
              onClick={() => setMobileOpen(true)}
              aria-label="Open navigation"
            >
              <Icon name="menu" size={18} />
            </button>
            <div className="breadcrumbs">
              <span className="crumb-root">Workspace</span>
              {location.startsWith("/devices/") && (
                <>
                  <span className="crumb-sep">/</span>
                  <Link href="/devices" className="crumb-root">
                    Devices
                  </Link>
                </>
              )}
              <span className="crumb-sep">/</span>
              <span className="crumb-current">{currentTitle}</span>
            </div>
          </div>
          <div className="topbar-actions">
            <div className="global-search">
              <Icon name="search" size={15} />
              <input
                aria-label="Search"
                placeholder="Search workspace (Enter to go)"
                value={searchTerm}
                onChange={e => setSearchTerm(e.target.value)}
                onKeyDown={e => {
                  if (e.key === "Enter" && searchTerm.trim()) {
                    const q = searchTerm.trim().toLowerCase();
                    if (q.includes("dev") || q.includes("rtr") || q.includes("cisco") || q.includes("forti")) navigate("/devices");
                    else if (q.includes("aud")) navigate("/audits");
                    else if (q.includes("find")) navigate("/findings");
                    else if (q.includes("risk")) navigate("/risk");
                    else if (q.includes("drift")) navigate("/drift");
                    else if (q.includes("what") || q.includes("sim")) navigate("/what-if");
                    else if (q.includes("ai")) navigate("/ai-analyst");
                    else if (q.includes("rep")) navigate("/reports");
                    else if (q.includes("frame") || q.includes("cis") || q.includes("nist")) navigate("/frameworks");
                    else if (q.includes("set")) navigate("/settings");
                    else navigate("/devices");
                    setSearchTerm("");
                  }
                }}
              />
            </div>

            <div className="topbar-popover-wrap">
              <button
                className="icon-button topbar-icon"
                aria-label="Notifications"
                onClick={() => setShowNotifications(value => !value)}
              >
                <Icon name="bell" size={17} />
                <span className="notification-dot" />
              </button>
              {showNotifications && (
                <div className="topbar-popover">
                  <strong>Workspace alerts</strong>
                  <span>EDGE-RTR-01 risk score changed 12 min ago.</span>
                  <span>
                    Configuration evidence is ready for normalization.
                  </span>
                </div>
              )}
            </div>
            <div className="topbar-popover-wrap">
              <button
                className="topbar-user"
                onClick={() => setShowAccount(value => !value)}
                aria-label="Open account menu"
              >
                <div className="avatar avatar-small">
                  {currentUser.avatar || "AS"}
                </div>
                <span>{currentUser.name.split(" ")[0]}</span>
                <Icon name="chevron" size={13} />
              </button>
              {showAccount && (
                <div className="topbar-popover account-popover">
                  <strong>{currentUser.name}</strong>
                  <span>{currentUser.role}</span>
                  <Link href="/settings" onClick={() => setShowAccount(false)}>
                    Open settings <Icon name="arrow" size={12} />
                  </Link>
                  <div className="account-popover-divider" />
                  <button
                    type="button"
                    className="account-popover-logout-btn"
                    onClick={() => {
                      setShowAccount(false);
                      logout();
                    }}
                  >
                    <Icon name="logOut" size={14} />
                    <span>Sign out</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        <div className="page-content">
          {location !== "/dashboard" && !isDetailPage && (
            <div className="page-heading">
              <div>
                <div className="eyebrow">Security operations workspace</div>
                <h1>{currentTitle}</h1>
              </div>
              <div className="context-label">NEXUS-COMPLY · Network Assurance</div>
            </div>
          )}
          {children}
        </div>
      </main>
    </div>
  );
}
