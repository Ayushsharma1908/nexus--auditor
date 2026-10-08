# NEXUS-COMPLY

> **Next-Generation Autonomous Network Assurance, Multi-Vendor Compliance & Security Audit Platform**

[![Live Frontend](https://img.shields.io/badge/Frontend-Vercel-black?style=for-the-badge&logo=vercel)](https://client-sigma-virid-90.vercel.app/)
[![Live Backend API](https://img.shields.io/badge/Backend%20API-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://nexus-auditor-gdwe.onrender.com/)
[![Swagger API Docs](https://img.shields.io/badge/API%20Docs-Swagger%20UI-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)](https://nexus-auditor-gdwe.onrender.com/swagger-ui.html)
[![Java](https://img.shields.io/badge/Java-17%20%7C%20Spring%20Boot%203.3.4-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19%20%7C%20Vite%20%7C%20TypeScript-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB%20Atlas-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/atlas)
[![Gemini AI](https://img.shields.io/badge/AI%20Engine-Google%20Gemini-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://ai.google.dev/)

---

## 🌐 Live Deployments

| Component | Platform | URL |
| :--- | :--- | :--- |
| **Frontend Application** | **Vercel** | [https://client-sigma-virid-90.vercel.app/](https://client-sigma-virid-90.vercel.app/) |
| **Backend REST API** | **Render** | [https://nexus-auditor-gdwe.onrender.com/](https://nexus-auditor-gdwe.onrender.com/) |
| **Interactive API Documentation** | **Swagger UI** | [https://nexus-auditor-gdwe.onrender.com/swagger-ui.html](https://nexus-auditor-gdwe.onrender.com/swagger-ui.html) |
| **Cloud Database** | **MongoDB Atlas** | Managed Cluster (M0 Sandbox / Dedicated) |

---

## 🚀 Key Features

- **Multi-Vendor Configuration Parsers**: Native AST parsers for **Cisco IOS/IOS-XE**, **Fortinet FortiOS**, **Juniper JunOS**, and **Palo Alto PAN-OS**.
- **Canonical Schema Normalization**: Normalizes disparate vendor configuration syntaxes into a unified canonical security posture document.
- **Compliance Rule Engine**: Evaluates configurations against standard cybersecurity frameworks including:
  - **NIST SP 800-53 (Rev 5)**
  - **ISO/IEC 27001:2022**
  - **CIS Benchmarks** (Level 1 & Level 2)
  - **PCI-DSS 4.0**
- **AI Analyst Layer (Google Gemini 1.5/2.5 Flash)**:
  - Automatic resolution and semantic mapping of unknown or custom vendor syntax.
  - Interactive multi-turn chat for syntax explanation, risk rationalization, and remediation planning.
  - Intelligent noise filtering and secret sanitization before LLM processing.
- **Continuous Configuration Drift Detection**: Line-by-line and semantic drift analysis across configuration versions with timestamped timeline diffs.
- **"What-If" Simulation Sandbox**: Simulate proposed security and firewall rule changes to predict compliance posture impact before applying to production.
- **Automated Remediation Playbooks**: Deterministic CLI remediation scripts with dry-run validation and post-remediation verification checks.
- **Fleet Risk Scoring & Interactive Dashboards**: Real-time fleet health metrics, vulnerability heatmaps, and downloadable executive audit reports.

---

## 🏗️ System Architecture

```
                                  ┌────────────────────────┐
                                  │     Client Browser     │
                                  └───────────┬────────────┘
                                              │ HTTPS
                                              ▼
                                 ┌─────────────────────────┐
                                 │     Vercel Frontend     │
                                 │   (React 19 / Vite SPA) │
                                 └────────────┬────────────┘
                                              │ REST API / CORS
                                              ▼
                                 ┌─────────────────────────┐
                                 │      Render Backend     │
                                 │  (Spring Boot 3.3.4)    │
                                 └───┬─────────────────┬───┘
                                     │                 │
             ┌───────────────────────┘                 └─────────────────────────┐
             │ MongoDB Wire (TLS)                                                │ REST / gRPC
             ▼                                                                   ▼
┌─────────────────────────┐                                         ┌─────────────────────────┐
│      MongoDB Atlas      │                                         │    Google Gemini API    │
│  (Cloud NoSQL Storage)  │                                         │ (AI Analyst & Mappings) │
└─────────────────────────┘                                         └─────────────────────────┘
```

---

## 🛠️ Technology Stack

### Frontend (`client/`)
- **Core**: React 19, TypeScript, Vite 7
- **Styling**: Tailwind CSS, Custom Glassmorphism Design System
- **Routing**: Wouter (Client-side lightweight routing)
- **Visualizations**: Recharts (Compliance Trends, Risk Breakdown, Donut Charts)
- **Icons**: Lucide React
- **Hosting**: Vercel SPA with security headers & fallback rewrites

### Backend (`server/`)
- **Framework**: Spring Boot 3.3.4 (Java 17)
- **Security**: Spring Security 6 (Stateless, Request ID Tracing Filter, Dynamic CORS)
- **Database Layer**: Spring Data MongoDB with automatic index creation
- **Documentation**: SpringDoc OpenAPI 2.5.0 with Swagger UI
- **AI Integration**: Google Gemini 1.5 / 2.5 Flash SDK via REST client
- **Hosting**: Render Container Web Service with automatic port binding

---

## 📁 Repository Structure

```text
nexus-auditor/
├── client/                     # Frontend Single Page Application (React / Vite)
│   ├── public/                 # Static assets and icons
│   ├── src/
│   │   ├── components/         # Reusable UI widgets, charts, and error boundaries
│   │   ├── contexts/           # Authentication and demo profile contexts
│   │   ├── layout/             # App shell, navigation sidebar, and topbar
│   │   ├── lib/                # API client with automatic URL normalization & data hooks
│   │   ├── pages/              # Workspace, Compliance, Audits, Drift, Intelligence, AI Analyst
│   │   └── types/              # TypeScript canonical interface definitions
│   ├── package.json            # Frontend dependencies & build scripts
│   ├── vercel.json             # Vercel deployment & routing rewrite configuration
│   └── vite.config.ts          # Vite build configuration & local dev proxy
│
├── server/                     # Backend API & Engine (Java 17 / Spring Boot)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/nexuscomply/
│   │   │   │   ├── ai/         # AI analysis services & Gemini chat controller
│   │   │   │   ├── audit/      # Audit execution and orchestration
│   │   │   │   ├── common/     # SecurityConfig, CorsConfig, RootController, ApiResponse
│   │   │   │   ├── compliance/ # Compliance rule evaluation engine & seeders
│   │   │   │   ├── cyber/      # Vendor parsers, evidence creation, drift detection
│   │   │   │   ├── device/     # Device fleet inventory management
│   │   │   │   ├── finding/    # Findings persistence & status workflows
│   │   │   │   ├── remediation/# Remediation template generator & verifier
│   │   │   │   ├── report/     # Report generation & aggregation
│   │   │   │   ├── risk/       # Risk matrix calculation algorithms
│   │   │   │   └── simulation/ # What-if sandbox simulation engine
│   │   │   └── resources/
│   │   │       ├── application.yml # Dynamic environment-driven configuration
│   │   │       └── seed/           # Baseline rules, controls, and sample configurations
│   │   └── test/               # Comprehensive unit & integration test suites
│   ├── Dockerfile              # Multi-stage container build for Render
│   └── pom.xml                 # Maven dependencies and build plugins
│
├── .gitignore                  # Production-grade Git ignore (Zero-secret leakage)
├── .env.example                # Global environment variables template
└── README.md                   # Project documentation
```

---

## ⚡ Quick Start / Local Development

### 1. Prerequisites
- **Java**: JDK 17 or later
- **Node.js**: Node 18+ (Node 20 or 22 recommended)
- **Maven**: Maven 3.8+ (or use IDE bundled Maven)
- **MongoDB**: Local MongoDB instance (port `27017`) or free [MongoDB Atlas](https://www.mongodb.com/atlas) cluster

---

### 2. Backend Setup (`server`)

1. Open a terminal in the `server` directory:
   ```bash
   cd server
   ```
2. Create your local `.env` file (copied from `.env.example`):
   ```bash
   cp .env.example .env
   ```
3. Configure your local variables:
   ```properties
   PORT=8080
   SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/nexus_comply
   NEXUS_AI_PROVIDER=gemini
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
4. Build and start the Spring Boot server:
   ```bash
   mvn clean spring-boot:run
   ```
5. Verify the backend is running:
   - Root Health Check: `http://localhost:8080/`
   - Swagger API Documentation: `http://localhost:8080/swagger-ui.html`

---

### 3. Frontend Setup (`client`)

1. Open a second terminal in the `client` directory:
   ```bash
   cd client
   ```
2. Install npm dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 🔐 Environment Variables Reference

| Variable Name | Required | Default / Example | Purpose |
| :--- | :---: | :--- | :--- |
| `PORT` | Optional | `8080` (Local) / `10000` (Render) | Server HTTP listening port |
| `SPRING_DATA_MONGODB_URI` | **Yes** | `mongodb+srv://user:pass@cluster.mongodb.net/nexus_comply` | MongoDB Atlas database connection string |
| `SPRING_DATA_MONGODB_DATABASE` | Optional | `nexus_comply` | Target MongoDB database name |
| `NEXUS_AI_PROVIDER` | Optional | `gemini` | AI suggestion provider (`gemini` or `stub`) |
| `GEMINI_API_KEY` | Optional | `AIzaSy...` | Google Gemini API key for AI Analyst |
| `GEMINI_API_MODEL` | Optional | `gemini-1.5-flash` | Gemini model version |
| `CORS_ALLOWED_ORIGINS` | Optional | `*` or `https://*.vercel.app,http://localhost:3000` | Allowed CORS origins for the backend |
| `VITE_API_BASE_URL` | Optional | `/api/v1` (Local) / `https://<render-url>/api/v1` (Vercel) | Backend API base URL for frontend |

---

## ☁️ Deployment Guide

### A. Database (MongoDB Atlas)
1. Create a free **M0 Cluster** on MongoDB Atlas.
2. Create a Database User with read/write permissions.
3. In **Network Access**, add `0.0.0.0/0` (Allow from anywhere).
4. Copy the connection string: `mongodb+srv://<username>:<password>@<cluster>.mongodb.net/nexus_comply?retryWrites=true&w=majority`.

### B. Backend (Render)
1. Create a new **Web Service** on Render and connect your repository.
2. Select **Root Directory**: `server`
3. Select **Runtime**: `Docker` *(Render will build using `server/Dockerfile`)*
4. Under **Environment Variables**, add:
   - `SPRING_DATA_MONGODB_URI`: `<Your MongoDB Atlas URI>`
   - `SPRING_DATA_MONGODB_DATABASE`: `nexus_comply`
   - `NEXUS_AI_PROVIDER`: `gemini`
   - `GEMINI_API_KEY`: `<Your Gemini API Key>`
   - `CORS_ALLOWED_ORIGINS`: `*`
5. Click **Create Web Service**.

### C. Frontend (Vercel)
1. Import the repository into Vercel.
2. Set **Root Directory** to `client`.
3. Framework preset will automatically detect `Vite`.
4. Under **Environment Variables**, add:
   - `VITE_API_BASE_URL`: `https://<your-render-backend>.onrender.com/api/v1`
5. Click **Deploy**.

---

## 🛡️ Security Best Practices

- **Zero Secret Exposure**: Real API keys and database credentials are fully gitignored and must only be supplied via host environment variables.
- **Request Tracking**: Every HTTP request receives a unique `X-Request-Id` header for traceability and audit logs.
- **Sanitized AI Ingestion**: Configuration inputs are stripped of passwords, private keys, and secrets before evaluation by external LLMs.
- **Stateless Session Management**: Backed by JWT/Stateless Security filters, making the service resilient to horizontal scaling.

---

## 📜 License

This project is licensed under the **MIT License**.
