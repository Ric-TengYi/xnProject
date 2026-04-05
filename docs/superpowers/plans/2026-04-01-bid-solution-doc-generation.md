# Bid Solution Document Set Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the full bid-facing requirements and technical-solution document set, plus special-topic documents and an internal gap analysis set, all aligned to the cleaned bid module structure.

**Architecture:** Generate the document set from a normalized module map extracted from the bid DOCX, then enrich each module with a consistent requirements template, technical-solution template, and cross-cutting content for mobile, integrations, security, and operations. Internal gap-analysis documents will be generated separately from the client-facing set and will reference current repository capabilities and existing status documents.

**Tech Stack:** Markdown, Python 3 for structured extraction and bulk document generation, repository status docs, frontend/backend code structure, git for checkpoint commits

---

### Task 1: Normalize source inputs and target file registry

**Files:**
- Create: `docs/bid-solution-module-map.json`
- Modify: `docs/superpowers/plans/2026-04-01-bid-solution-doc-generation.md`
- Read: `渣土软件招标文件定稿版.docx`
- Read: `docs/superpowers/specs/2026-04-01-bid-solution-doc-set-design.md`
- Read: `docs/tech-plans/requirements_status_2026-03-19.md`

- [ ] **Step 1: Extract the bid module structure into normalized data**

Run: `python3 <normalization script>`
Expected: a structured `docs/bid-solution-module-map.json` containing the 25 target modules and their feature items.

- [ ] **Step 2: Verify module count and feature coverage**

Run: `python3 <validation script>`
Expected: output confirms the 25 module set matches the design spec and no required bid feature is dropped.

- [ ] **Step 3: Align client-facing and internal-writing sources**

Check:
- `docs/superpowers/specs/2026-04-01-bid-solution-doc-set-design.md`
- `docs/tech-plans/requirements_status_2026-03-19.md`

Expected: client-facing documents use the bid as the primary source; internal analysis uses repository status as the primary source.

### Task 2: Build the target directory structure and index files

**Files:**
- Create: `docs/bid-solution/01-甲方版-需求方案/00_需求方案总目录.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/00_技术方案总目录.md`
- Create: `docs/bid-solution/03-专项方案/01_第三方系统对接专项方案.md`
- Create: `docs/bid-solution/03-专项方案/02_安全合规方案.md`
- Create: `docs/bid-solution/03-专项方案/03_运维保障方案.md`
- Create: `docs/bid-solution/03-专项方案/04_总体架构与部署方案.md`
- Create: `docs/bid-solution/04-内部版-差距分析/00_现状与差距总表.md`
- Create: `docs/bid-solution/04-内部版-差距分析/01_按招标功能点差距分析.md`
- Create: `docs/bid-solution/04-内部版-差距分析/02_分阶段建设建议.md`

- [ ] **Step 1: Create the final directory tree**

Run: `mkdir -p docs/bid-solution/01-甲方版-需求方案 docs/bid-solution/02-甲方版-技术方案 docs/bid-solution/03-专项方案 docs/bid-solution/04-内部版-差距分析`
Expected: the four output directories exist and are ready for document generation.

- [ ] **Step 2: Generate the two top-level index files**

Expected content:
- module list
- document purpose
- mapping between bid modules and generated files

- [ ] **Step 3: Pre-create the special-topic and internal-analysis files**

Expected: all nine non-module files exist and match the approved document design.

### Task 3: Generate all client-facing requirement documents

**Files:**
- Create: `docs/bid-solution/01-甲方版-需求方案/01_合同结算需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/02_单位管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/03_项目管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/04_处置证需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/05_消纳场地需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/06_违规车辆清单需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/07_事件管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/08_系统预警需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/09_预警配置需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/10_违规车辆研判模型需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/11_信息查询需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/12_组织管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/13_组织人员管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/14_角色管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/15_系统日志需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/16_审核审批配置需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/17_数据字典需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/18_系统参数配置需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/19_统计分析需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/20_数据看板需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/21_平台对接需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/22_消息管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/23_小程序需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/24_安全管理需求方案.md`
- Create: `docs/bid-solution/01-甲方版-需求方案/25_车辆管理需求方案.md`

- [ ] **Step 1: Build a reusable requirements-document template**

Expected sections:
- module objective
- business positioning
- roles and scenarios
- feature-by-feature requirement analysis
- mobile requirements
- boundaries
- buttons and actions
- input/output limits

- [ ] **Step 2: Fill each module using bid-first language**

Expected: every feature from the bid appears in its module file, written as client-facing requirement understanding rather than current-system inventory.

- [ ] **Step 3: Verify mobile-related feature coverage**

Run: `rg -n "移动端|小程序|拍照|定位|轨迹|分享|短信|微信|电子围栏" docs/bid-solution/01-甲方版-需求方案`
Expected: mobile-related items appear in both the dedicated mobile document and the related business-module documents.

### Task 4: Generate all client-facing technical-solution documents

**Files:**
- Create: `docs/bid-solution/02-甲方版-技术方案/01_合同结算技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/02_单位管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/03_项目管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/04_处置证技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/05_消纳场地技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/06_违规车辆清单技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/07_事件管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/08_系统预警技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/09_预警配置技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/10_违规车辆研判模型技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/11_信息查询技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/12_组织管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/13_组织人员管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/14_角色管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/15_系统日志技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/16_审核审批配置技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/17_数据字典技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/18_系统参数配置技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/19_统计分析技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/20_数据看板技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/21_平台对接技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/22_消息管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/23_小程序技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/24_安全管理技术方案.md`
- Create: `docs/bid-solution/02-甲方版-技术方案/25_车辆管理技术方案.md`

- [ ] **Step 1: Build a reusable technical-solution template**

Expected sections:
- capability design
- feature implementation design
- interface design
- data objects
- workflow
- permissions
- exception handling
- observability

- [ ] **Step 2: Expand each bid feature into a technical implementation item**

Expected: every feature item includes business interfaces, process description, state handling, permissions, and exception strategy.

- [ ] **Step 3: Verify the required integration domains are fully covered**

Run: `rg -n "政务网|定位|GPS|地磅|视频|SSO|单点登录|异常处理|补偿|对账" docs/bid-solution/02-甲方版-技术方案 docs/bid-solution/03-专项方案`
Expected: all required integration topics are covered in both the module-level and special-topic materials.

### Task 5: Generate the special-topic document set

**Files:**
- Modify: `docs/bid-solution/03-专项方案/01_第三方系统对接专项方案.md`
- Modify: `docs/bid-solution/03-专项方案/02_安全合规方案.md`
- Modify: `docs/bid-solution/03-专项方案/03_运维保障方案.md`
- Modify: `docs/bid-solution/03-专项方案/04_总体架构与部署方案.md`

- [ ] **Step 1: Write the third-party integration special topic**

Expected: complete coverage of government network, positioning, weighbridge, video, interface norms, authentication, exception handling, compensation, reconciliation, and monitoring.

- [ ] **Step 2: Write the security-compliance special topic**

Expected: explicit sections for 等保二级配合, 信创, data security, permission control, audit logging, and transmission security.

- [ ] **Step 3: Write the operations-assurance and architecture special topics**

Expected: deployment, observability, alerting, incident response, backups, change management, and capacity planning are all covered.

### Task 6: Generate the internal gap-analysis document set

**Files:**
- Modify: `docs/bid-solution/04-内部版-差距分析/00_现状与差距总表.md`
- Modify: `docs/bid-solution/04-内部版-差距分析/01_按招标功能点差距分析.md`
- Modify: `docs/bid-solution/04-内部版-差距分析/02_分阶段建设建议.md`
- Read: `docs/tech-plans/requirements_status_2026-03-19.md`
- Read: `docs/tech-plans/remaining_status_recalibrated_2026-03-23.md`
- Read: `xngl-web/src/pages/*`
- Read: `xngl-service/**/src/main/java/**`

- [ ] **Step 1: Build a bid-to-current-capability mapping**

Expected: each bid feature gets a status label of `已满足` / `部分满足` / `未满足` / `需优化`.

- [ ] **Step 2: Write the internal total gap table**

Expected: one summary file that management can use for quick prioritization.

- [ ] **Step 3: Write the phased implementation suggestions**

Expected: a staged roadmap that groups gaps into near-term, mid-term, and enhancement phases.

### Task 7: Review, acceptance, and revision loop

**Files:**
- Modify: `docs/bid-solution/**/*`

- [ ] **Step 1: Run document-set validation checks**

Run:
- `rg -n "TODO|TBD|待补|占位|略" docs/bid-solution`
- `find docs/bid-solution -type f | sort`

Expected: no placeholder words remain; all planned files exist.

- [ ] **Step 2: Perform an independent Codex review pass**

Expected: a separate Codex worker reviews the generated documents for omissions, boundary ambiguity, mobile coverage gaps, integration incompleteness, and wording issues.

- [ ] **Step 3: Apply review feedback and finalize**

Expected: the document set is internally consistent, client-facing material and internal material stay separated, and major omissions are fixed before final handoff.
