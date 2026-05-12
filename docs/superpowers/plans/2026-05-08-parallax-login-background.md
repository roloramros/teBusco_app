# Parallax Statistics Background Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current static/card-based login background with a dynamic parallax effect featuring platform statistics moving vertically at different speeds.

**Architecture:** 
- **Layout:** Use a `FrameLayout` to layer a background container behind the login form.
- **Utility:** `ParallaxStatsManager` to dynamically create, animate, and update multiple `TextView` instances.
- **Integration:** `LoginActivity` fetches data and feeds it to the manager.

**Tech Stack:** Android (Java), ObjectAnimator, Retrofit.

---

### Task 1: Cleanup and Layout Preparation

**Files:**
- Modify: `app/src/main/res/layout/activity_login.xml`

- [x] **Step 1: Remove old stat blocks and prepare container**
- [x] **Step 2: Ensure ScrollView is transparent**
- [x] **Step 3: Commit**
`git add app/src/main/res/layout/activity_login.xml`
`git commit -m "ui: prepare login layout for parallax background"`

---

### Task 2: Implement ParallaxStatsManager Utility

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/utils/ParallaxStatsManager.java`

- [x] **Step 1: Create the manager class with initialization logic**
- [x] **Step 2: Commit**
`git add app/src/main/java/com/codram/terecojo/utils/ParallaxStatsManager.java`
`git commit -m "feat: implement ParallaxStatsManager for background animation"`

---

### Task 3: Integration in LoginActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [x] **Step 1: Replace old animation logic with ParallaxStatsManager**
- [x] **Step 2: Cleanup unused code**
- [x] **Step 3: Verify and Commit**
`git add app/src/main/java/com/codram/terecojo/LoginActivity.java`
`git commit -m "feat: integrate parallax statistics background in LoginActivity"`
