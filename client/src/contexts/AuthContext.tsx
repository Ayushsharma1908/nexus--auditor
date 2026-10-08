import React, { createContext, useContext, useState } from "react";

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: string;
  organization: string;
  avatar: string;
  lastLogin?: string;
}

export const DEMO_PROFILES: AuthUser[] = [
  {
    id: "USR-001",
    name: "Ayush Sharma",
    email: "admin@nexus-comply.local",
    role: "Security Administrator",
    organization: "Nexus Security Operations",
    avatar: "AS",
  },
  {
    id: "USR-002",
    name: "Priya Nair",
    email: "priya.nair@nexus-comply.local",
    role: "Compliance & Risk Auditor",
    organization: "Assurance & Governance",
    avatar: "PN",
  },
  {
    id: "USR-003",
    name: "Vikram Malhotra",
    email: "vikram.m@nexus-comply.local",
    role: "Network Infrastructure Lead",
    organization: "Core Engineering",
    avatar: "VM",
  },
];

interface AuthContextType {
  user: AuthUser | null;
  isAuthenticated: boolean;
  selectedDemoProfile: AuthUser;
  setSelectedDemoProfile: (profile: AuthUser) => void;
  login: (userData?: Partial<AuthUser> | null, remember?: boolean) => void;
  instantDemoLogin: (profile?: AuthUser) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const AUTH_STORAGE_KEY = "nexus_auth_user";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [selectedDemoProfile, setSelectedDemoProfile] = useState<AuthUser>(
    DEMO_PROFILES[0]
  );
  const [user, setUser] = useState<AuthUser | null>(() => {
    try {
      const storedLocal = localStorage.getItem(AUTH_STORAGE_KEY);
      if (storedLocal) return JSON.parse(storedLocal);
      const storedSession = sessionStorage.getItem(AUTH_STORAGE_KEY);
      if (storedSession) return JSON.parse(storedSession);
    } catch {
      // ignore JSON parse error
    }
    return null;
  });

  const isAuthenticated = !!user;

  const login = (
    userData?: Partial<AuthUser> | null,
    remember: boolean = true
  ) => {
    let authUser: AuthUser;
    if (userData && userData.email) {
      const match = DEMO_PROFILES.find(
        p => p.email.toLowerCase() === userData.email?.toLowerCase()
      );
      authUser = {
        id:
          userData.id ||
          match?.id ||
          `USR-${Math.floor(100 + Math.random() * 900)}`,
        name: userData.name || match?.name || userData.email.split("@")[0],
        email: userData.email,
        role: userData.role || match?.role || "Security Analyst",
        organization:
          userData.organization ||
          match?.organization ||
          "Nexus Security Operations",
        avatar:
          userData.avatar ||
          match?.avatar ||
          (userData.name ? userData.name.slice(0, 2).toUpperCase() : "US"),
        lastLogin: new Date().toISOString(),
      };
    } else {
      authUser = {
        ...selectedDemoProfile,
        lastLogin: new Date().toISOString(),
      };
    }

    setUser(authUser);
    try {
      if (remember) {
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser));
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
      } else {
        sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authUser));
        localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    } catch {
      // storage error fallback
    }
  };

  const instantDemoLogin = (profile?: AuthUser) => {
    const target = profile || selectedDemoProfile;
    login(target, true);
  };

  const logout = () => {
    setUser(null);
    try {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      sessionStorage.removeItem(AUTH_STORAGE_KEY);
    } catch {
      // storage error fallback
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        selectedDemoProfile,
        setSelectedDemoProfile,
        login,
        instantDemoLogin,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
