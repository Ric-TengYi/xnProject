# Demo Readiness Mobile Surface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the minimum demo-ready package for the formal client video by adding a runnable mini/mobile demo surface, replacing mock permit content with real data, and removing outward-facing mock wording from integration pages.

**Architecture:** Add a dedicated frontend mini demo route that calls existing `/api/mini/*` APIs with its own token, reuse existing permit APIs in project detail, and keep backend changes minimal or zero unless a small integration gap is discovered. The implementation is deliberately narrow: demo usability first, no new domain expansion.

**Tech Stack:** React 19, Vite, TypeScript, Ant Design, existing REST APIs under `/api` and `/api/mini`.

---

### Task 1: Refine The Internal Demo Checklist

**Files:**
- Modify: `docs/bid-solution/04-内部版-差距分析/03_演示必补功能清单.md`

- [x] **Step 1: Rewrite P0 to the minimum necessary set**
- [x] **Step 2: Move already-validated PC flows into P1**
- [x] **Step 3: Add demo account/data preparation as a first-class blocker**

### Task 2: Add Mini Demo API Layer

**Files:**
- Create: `xngl-web/src/utils/miniApi.ts`

- [ ] **Step 1: Add a dedicated request helper using `fetch` and explicit mini token injection**
- [ ] **Step 2: Add auth methods for send code, login, openId login, bind account, load profile**
- [ ] **Step 3: Add work-order methods for photos, manual disposals, events, feedbacks**
- [ ] **Step 4: Add vehicle methods for realtime list and track history**
- [ ] **Step 5: Add lightweight TypeScript response types for the page to consume**

### Task 3: Build The Mobile Demo Surface

**Files:**
- Create: `xngl-web/src/pages/MiniProgramDemo.tsx`

- [ ] **Step 1: Build the page shell with phone-style presentation and mini token state**
- [ ] **Step 2: Add login and account binding section**
- [ ] **Step 3: Add on-site operations section for photo/manual disposal/event/feedback**
- [ ] **Step 4: Add vehicle tracking section**
- [ ] **Step 5: Add “submitted records” views so every action has a local callback result**

### Task 4: Expose The Demo Route In The App

**Files:**
- Modify: `xngl-web/src/App.tsx`
- Modify: `xngl-web/src/layouts/MainLayout.tsx`

- [ ] **Step 1: Register the new route**
- [ ] **Step 2: Add a visible but contained navigation entry for demo use**
- [ ] **Step 3: Ensure the label is formal and client-facing**

### Task 5: Replace Mock Permit Content With Real Data

**Files:**
- Modify: `xngl-web/src/pages/ProjectDetail.tsx`
- Reuse: `xngl-web/src/utils/permitApi.ts`

- [ ] **Step 1: Load permit data by project ID**
- [ ] **Step 2: Remove the local `permitMock` data generation**
- [ ] **Step 3: Render real permit rows with project-linked filtering**
- [ ] **Step 4: Add a jump to the permit management page for full drill-down**

### Task 6: Remove Outward-Facing Mock Wording

**Files:**
- Modify: `xngl-web/src/pages/PlatformIntegrations.tsx`
- Modify: `xngl-web/src/pages/ProjectsPermits.tsx`

- [ ] **Step 1: Replace visible `mock` wording with formal sync wording**
- [ ] **Step 2: Keep existing API calls but align success messages with formal demo language**
- [ ] **Step 3: Review descriptive helper text for demo suitability**

### Task 7: Verify Demo Readiness

**Files:**
- None required unless a small smoke artifact is needed

- [ ] **Step 1: Run `cd xngl-web && npm run build`**
- [ ] **Step 2: Run `cd xngl-web && npm run lint` if the new page introduces no repo-wide unrelated blockers**
- [ ] **Step 3: Run one local API smoke to confirm mini auth still works**
- [ ] **Step 4: Manually inspect the new route and the project permit section in the running app**
