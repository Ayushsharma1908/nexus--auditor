import { useState, useEffect } from "react";
import { useLocation } from "wouter";
import { Icon } from "@/components/Icon";
import { useAuth, DEMO_PROFILES, type AuthUser } from "@/contexts/AuthContext";

export default function Login() {
  const [, navigate] = useLocation();
  const {
    user: currentUser,
    isAuthenticated,
    login,
    selectedDemoProfile,
    setSelectedDemoProfile,
  } = useAuth();

  const [username, setUsername] = useState(
    selectedDemoProfile?.email || "admin@nexus-comply.local"
  );
  const [passcode, setPasscode] = useState("nexus-admin-2026");
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [isMockSelected, setIsMockSelected] = useState(true);
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showProfileSwitcher, setShowProfileSwitcher] = useState(false);

  useEffect(() => {
    const originalBodyOverflow = document.body.style.overflow;
    const originalHtmlOverflow = document.documentElement.style.overflow;
    const originalBodyHeight = document.body.style.height;
    const originalHtmlHeight = document.documentElement.style.height;

    document.body.style.overflow = "hidden";
    document.documentElement.style.overflow = "hidden";
    document.body.style.height = "100vh";
    document.documentElement.style.height = "100vh";

    return () => {
      document.body.style.overflow = originalBodyOverflow;
      document.documentElement.style.overflow = originalHtmlOverflow;
      document.body.style.height = originalBodyHeight;
      document.documentElement.style.height = originalHtmlHeight;
    };
  }, []);

  const handleSelectProfile = (profile: AuthUser) => {
    setSelectedDemoProfile(profile);
    setUsername(profile.email);
    setPasscode("nexus-admin-2026");
    setIsMockSelected(true);
    setError("");
    setShowProfileSwitcher(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !passcode.trim()) {
      setError("Please enter both username/email and passcode.");
      return;
    }

    setError("");
    setIsSubmitting(true);

    const matchedProfile = DEMO_PROFILES.find(
      p => p.email.toLowerCase() === username.trim().toLowerCase()
    );

    login(
      matchedProfile || {
        email: username.trim(),
        name: username.split("@")[0],
        role: "Security Analyst",
        organization: "Nexus Security Operations",
        avatar: username.slice(0, 2).toUpperCase(),
      },
      rememberMe
    );

    setTimeout(() => {
      navigate("/dashboard");
    }, 250);
  };

  return (
    <div className="login-root-canvas">
      {/* Ambient background glow effects */}
      <div className="login-ambient-glow login-glow-top-left" />
      <div className="login-ambient-glow login-glow-bottom-right" />

      {/* Top Header */}
      <header className="login-topbar">
        <div className="login-brand-group">
          <img
            src="/logo.png"
            alt="NEXUS-COMPLY"
            className="login-brand-logo"
          />
          <div className="login-brand-text">
            <div className="login-brand-title">
              NEXUS-COMPLY
            </div>
            <div className="login-brand-tagline">
              Empower Your Security Operations, Simplify Compliance!
            </div>
          </div>
        </div>
      </header>

      {/* Main Dual-Panel Container */}
      <main className="login-main-container">
        <div className="login-card-shell">
          {/* ============================================================
              LEFT PANEL: SECURITY ILLUSTRATION & SMALL DESCRIPTION ONLY
              ============================================================ */}
          <section
            className="login-preview-panel"
            aria-label="Security overview and architecture"
          >
            <div className="preview-illustration-frame">
              <div className="illustration-glow-backdrop" />

              {/* Cybersecurity PNG Illustration */}
              <img
                src="/security-illustration.png"
                alt="Nexus Security Operations Illustration"
                className="security-ill-img"
              />

              {/* Small Description below illustration */}
              <div className="illustration-content-wrap">
                <div className="illustration-badge-pill">
                  <Icon name="shield" size={13} className="ill-tag-icon" />
                  <span>Deterministic Rule Engine</span>
                </div>
                <h3 className="illustration-title">
                  Enterprise Network Assurance
                </h3>
                <p className="illustration-description">
                  Continuous multi-vendor configuration compliance,
                  deterministic risk analysis, and automated audit trails.
                </p>
              </div>
            </div>
          </section>

          {/* ============================================================
              RIGHT PANEL: AUTHENTICATION FORM & DEMO USER SELECTOR
              ============================================================ */}
          <section className="login-form-panel" aria-label="Sign in form">
            <div className="login-form-content">
              <div className="login-header-group">
                <h2 className="login-title">Sign in to Nexus</h2>
                <p className="login-subtitle">
                  Empower Your Security Operations, Simplify Compliance!
                </p>
              </div>

              {/* Authenticated Banner if already logged in */}
              {isAuthenticated && currentUser && (
                <div className="login-already-auth-banner">
                  <div className="already-auth-info">
                    <span className="auth-dot" />
                    <span>
                      Currently signed in as <strong>{currentUser.name}</strong>
                    </span>
                  </div>
                  <button
                    type="button"
                    className="already-auth-btn"
                    onClick={() => navigate("/dashboard")}
                  >
                    Go to Workspace <Icon name="arrow" size={12} />
                  </button>
                </div>
              )}

              {/* Security Operator Profile Selector Card */}
              <div className="mock-selector-section">
                <div className="mock-selector-header">
                  <span className="mock-section-label">OPERATOR PROFILE</span>
                  <button
                    type="button"
                    className="mock-switch-toggle-btn"
                    onClick={() => setShowProfileSwitcher(v => !v)}
                  >
                    {showProfileSwitcher
                      ? "Hide Profiles"
                      : `Switch Role (${DEMO_PROFILES.length})`}
                  </button>
                </div>

                {/* Main Active Security Profile Card */}
                <button
                  type="button"
                  className={`mock-user-card ${isMockSelected ? "mock-card-active" : ""}`}
                  onClick={() => handleSelectProfile(selectedDemoProfile)}
                  aria-pressed={isMockSelected}
                >
                  <div className="mock-avatar-wrap">
                    <div className="mock-avatar">
                      {selectedDemoProfile.avatar}
                    </div>
                    <span className="mock-online-indicator" />
                  </div>
                  <div className="mock-user-info">
                    <div className="mock-user-name-row">
                      <strong className="mock-name">
                        {selectedDemoProfile.name}
                      </strong>
                      <span className="mock-active-pill">
                        {isMockSelected ? "✓ Active Operator" : "Click to select"}
                      </span>
                    </div>
                    <div className="mock-role">
                      {selectedDemoProfile.role} ·{" "}
                      {selectedDemoProfile.organization}
                    </div>
                  </div>
                </button>

                {/* Additional demo profile choices if switcher expanded */}
                {showProfileSwitcher && (
                  <div className="demo-profiles-dropdown">
                    {DEMO_PROFILES.map(profile => (
                      <button
                        key={profile.id}
                        type="button"
                        className={`demo-profile-item ${profile.id === selectedDemoProfile.id ? "active-item" : ""}`}
                        onClick={() => handleSelectProfile(profile)}
                      >
                        <div className="demo-item-avatar">{profile.avatar}</div>
                        <div className="demo-item-meta">
                          <strong className="demo-item-name">
                            {profile.name}
                          </strong>
                          <span className="demo-item-role">{profile.role}</span>
                        </div>
                        {profile.id === selectedDemoProfile.id && (
                          <Icon
                            name="check"
                            size={14}
                            className="demo-item-check"
                          />
                        )}
                      </button>
                    ))}
                  </div>
                )}
              </div>

              {/* Error Alert */}
              {error && (
                <div className="login-error-alert" role="alert">
                  <Icon name="warning" size={15} />
                  <span>{error}</span>
                </div>
              )}

              {/* Credentials Form */}
              <form onSubmit={handleSubmit} className="login-form">
                <div className="form-field-group">
                  <label htmlFor="username-input" className="form-field-label">
                    Username / Email{" "}
                    <span className="required-asterisk">*</span>
                  </label>
                  <div className="form-input-container">
                    <div className="input-icon-left">
                      <Icon name="user" size={15} />
                    </div>
                    <input
                      id="username-input"
                      type="text"
                      value={username}
                      onChange={e => {
                        setUsername(e.target.value);
                        setIsMockSelected(false);
                      }}
                      placeholder="e.g. admin@nexus-comply.local"
                      className="form-text-input"
                      autoComplete="username"
                      required
                    />
                  </div>
                </div>

                <div className="form-field-group">
                  <label htmlFor="passcode-input" className="form-field-label">
                    Passcode / Master Key{" "}
                    <span className="required-asterisk">*</span>
                  </label>
                  <div className="form-input-container">
                    <div className="input-icon-left">
                      <Icon name="shield" size={15} />
                    </div>
                    <input
                      id="passcode-input"
                      type={showPassword ? "text" : "password"}
                      value={passcode}
                      onChange={e => {
                        setPasscode(e.target.value);
                        setIsMockSelected(false);
                      }}
                      placeholder="••••••••••••"
                      className="form-text-input"
                      autoComplete="current-password"
                      required
                    />
                    <button
                      type="button"
                      className="input-icon-btn-right"
                      onClick={() => setShowPassword(prev => !prev)}
                      title={showPassword ? "Hide passcode" : "Show passcode"}
                      aria-label={
                        showPassword ? "Hide passcode" : "Show passcode"
                      }
                    >
                      <Icon name={showPassword ? "eyeOff" : "eye"} size={16} />
                    </button>
                  </div>
                </div>

                {/* Remember & Options */}
                <div className="login-options-row">
                  <label className="remember-checkbox-label">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={e => setRememberMe(e.target.checked)}
                      className="remember-checkbox"
                    />
                    <span>Remember this session</span>
                  </label>
                  <button
                    type="button"
                    className="forgot-pass-btn"
                    onClick={() => handleSelectProfile(DEMO_PROFILES[0])}
                  >
                    Reset Demo
                  </button>
                </div>

                {/* Submit Actions */}
                <div className="login-action-buttons-group">
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className={`login-submit-btn ${isSubmitting ? "submitting" : ""}`}
                  >
                    <span>
                      {isSubmitting ? "Signing in..." : "Sign in to Workspace"}
                    </span>
                    <Icon name="arrow" size={16} className="btn-arrow-icon" />
                  </button>
                </div>
              </form>

              {/* Bottom Security Footer */}
              <div className="login-security-footer">
                <div className="security-icon-dot">
                  <Icon name="shield" size={13} />
                </div>
                <span>
                  Protected by Deterministic Rule Engine & Multi-Vendor
                  Normalization.
                </span>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
