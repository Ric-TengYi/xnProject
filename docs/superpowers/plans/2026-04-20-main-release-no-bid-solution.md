# Main Release Without Bid Solution Assets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a local integration branch from `main`, merge the active development branches, remove `docs/bid-solution` content from the release branch, and push the cleaned integration branch.

**Architecture:** Start from local `main` so the unpublished local `main` commit is preserved, then merge the three unmerged feature branches into a dedicated release branch. After the merge result is stable, delete `docs/bid-solution` from the integration branch and add an ignore rule so large bid assets do not get pushed again from this branch lineage.

**Tech Stack:** Git, shell, repository `.gitignore`

---

### Task 1: Prepare the Repository State

**Files:**
- Modify: `.gitignore`
- Reference: `docs/bid-solution/`

- [ ] **Step 1: Verify the working tree is clean**

Run: `git status -sb`
Expected: current branch shows no modified or untracked files before branch switching.

- [ ] **Step 2: Verify the local branches that are unmerged into `origin/main`**

Run: `git branch -r --no-merged origin/main`
Expected: list includes `origin/feature/cmp-50-alert-collaboration-ui`, `origin/feature/contract-settlement-v2`, and `origin/feature/mini-driver-e2e`.

- [ ] **Step 3: Add a guardrail to ignore bid-solution assets on the integration branch**

Update `.gitignore` to include:

```gitignore
docs/bid-solution/
```

- [ ] **Step 4: Re-check repository status after the ignore-rule change**

Run: `git status --short`
Expected: only `.gitignore` shows as modified at this point.

### Task 2: Create the Integration Branch and Merge Development Branches

**Files:**
- Modify: `.gitignore`
- Remove: `docs/bid-solution/**`

- [ ] **Step 1: Create a dedicated integration branch from local `main`**

Run: `git switch main && git switch -c release/main-no-bid-solution-2026-04-20`
Expected: new local branch is created from the current local `main`.

- [ ] **Step 2: Merge the first feature branch**

Run: `git merge --no-ff feature/mini-driver-e2e`
Expected: branch merges cleanly or stops on conflicts for manual resolution.

- [ ] **Step 3: Merge the second feature branch**

Run: `git merge --no-ff feature/contract-settlement-v2`
Expected: branch merges cleanly or stops on conflicts for manual resolution.

- [ ] **Step 4: Merge the third feature branch**

Run: `git merge --no-ff feature/cmp-50-alert-collaboration-ui`
Expected: branch merges cleanly or stops on conflicts for manual resolution.

- [ ] **Step 5: Remove bid solution assets from the integrated branch**

Run: `rm -rf docs/bid-solution`
Expected: the full folder content is deleted from the integration branch tree.

- [ ] **Step 6: Commit the cleanup and merge result**

Run:

```bash
git add .gitignore
git add -A docs/bid-solution
git commit -m "chore: merge release branches without bid solution assets"
```

Expected: one commit records the asset removal and ignore rule on top of the merged branch result.

### Task 3: Verify and Push the Clean Integration Branch

**Files:**
- Verify: `.gitignore`
- Verify removed: `docs/bid-solution/**`

- [ ] **Step 1: Verify the branch no longer tracks bid-solution files**

Run: `git ls-tree -r --name-only HEAD | rg '^docs/bid-solution/'`
Expected: no output.

- [ ] **Step 2: Verify key repository checks before push**

Run: `mvn test -pl xngl-service-manager`
Expected: backend tests pass.

Run: `npm run build`
Expected: if this fails, record the exact TypeScript errors and do not claim the branch is release-ready.

- [ ] **Step 3: Push the integration branch**

Run: `git push -u origin release/main-no-bid-solution-2026-04-20`
Expected: remote branch is created successfully.

- [ ] **Step 4: Verify the branch tracks the new remote**

Run: `git status -sb`
Expected: current branch tracks `origin/release/main-no-bid-solution-2026-04-20` with no pending changes.
