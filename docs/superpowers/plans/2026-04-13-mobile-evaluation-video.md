# 小程序正式评审视频 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐移动端延期申报与亮点展示能力，并生成一支动态操作的小程序正式评审视频。

**Architecture:** 以 `MiniProgramDemo.tsx` 作为移动端动态演示页，补齐延期申报、留痕列表和增值亮点；复用现有 PC 看板与报表页面；再使用与 Web 正式评审相同的“百炼配音 + 字幕 + Playwright 录屏 + ffmpeg 合成”流程生成成片。

**Tech Stack:** React 19、TypeScript、Ant Design、Playwright、Node.js、ffmpeg、Bailian TTS

---

### Task 1: 补齐移动端延期申报与历史留痕

**Files:**
- Modify: `xngl-web/src/utils/miniApi.ts`
- Modify: `xngl-web/src/pages/MiniProgramDemo.tsx`

- [ ] Step 1: 添加延期申报接口封装与数据类型
- [ ] Step 2: 在移动端演示页补充延期申报表单
- [ ] Step 3: 在历史留痕区补充延期申报列表
- [ ] Step 4: 将“消纳记录”文案调整为更适合评审的“消纳清单”
- [ ] Step 5: 对改动文件做定向类型校验

### Task 2: 编写移动端正式讲解词与配音文本

**Files:**
- Create: `docs/bid-solution/demo-video/05_桐庐华数Mini正式评审讲解词.md`
- Create: `docs/bid-solution/demo-video/06_桐庐华数Mini正式配音文本.txt`

- [ ] Step 1: 按评分项优先顺序编写讲解词
- [ ] Step 2: 在讲解词末尾补充亮点能力说明
- [ ] Step 3: 输出适配百炼 TTS 的分段配音文本

### Task 3: 实现移动端正式评审录屏脚本

**Files:**
- Create: `docs/bid-solution/demo-video/record_mobile_scored_demo.mjs`
- Modify: `docs/bid-solution/demo-video/build_bailian_audio_assets.mjs`（如需要复用）

- [ ] Step 1: 设计移动端动态录屏顺序
- [ ] Step 2: 使用 Playwright 驱动移动端演示页完成动态操作
- [ ] Step 3: 切换到 PC 看板与报表页完成联动展示
- [ ] Step 4: 输出原始录屏文件

### Task 4: 实现移动端正式评审总装脚本

**Files:**
- Create: `docs/bid-solution/demo-video/build_mobile_scored_demo.sh`

- [ ] Step 1: 生成百炼配音分段
- [ ] Step 2: 生成整段配音和字幕时间轴
- [ ] Step 3: 调用录屏脚本输出原始视频
- [ ] Step 4: 合成带字幕成片

### Task 5: 验收与交付

**Files:**
- Output: `docs/bid-solution/demo-video/output/桐庐华数-渣土平台小程序端正式评审演示.mp4`

- [ ] Step 1: 用 `ffprobe` 校验成片时长与音视频流
- [ ] Step 2: 抽帧确认字幕存在且关键页面已覆盖
- [ ] Step 3: 校验评分项与亮点项均被展示
