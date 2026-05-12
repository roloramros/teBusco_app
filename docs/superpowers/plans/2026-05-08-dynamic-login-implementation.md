# Dynamic Login Background and Statistics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement an animated gradient background and real-time platform statistics (users, drivers, rides) for the Android login screen.

**Architecture:** 
- **Backend:** A new public REST endpoint in the Node.js API to fetch counts from the database.
- **Android:** A custom `ConstraintLayout` based UI with a programmatic gradient animator and floating `CardView` stat blocks with levitation animations.

**Tech Stack:** Node.js, Express, PostgreSQL, Android (Java), Retrofit.

---

### Task 1: Backend Statistics Endpoint

**Files:**
- Create: `tebusco-api/src/controllers/statsController.js`
- Modify: `tebusco-api/src/routes/geo.js`

- [x] **Step 1: Create stats controller**
- [x] **Step 2: Register the route in geo.js**
- [x] **Step 3: Verify the endpoint**
- [x] **Step 4: Commit**
`git add tebusco-api/src/controllers/statsController.js tebusco-api/src/routes/geo.js && git commit -m "backend: add public stats endpoint"`

---

### Task 2: Android API Data Model

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/data/model/StatsResponse.java`
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

- [x] **Step 1: Create StatsResponse model**
- [x] **Step 2: Add to ApiService**
- [x] **Step 3: Commit**
`git add app/src/main/java/com/codram.terecojo/data/model/StatsResponse.java app/src/main/java/com/codram/terecojo/data/remote/ApiService.java && git commit -m "android: add stats data model and api call"`

---

### Task 3: Android Animated Gradient Background

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/utils/GradientAnimator.java`
- Modify: `app/src/main/res/layout/activity_login.xml`

- [x] **Step 1: Create GradientAnimator utility**
- [x] **Step 2: Modify Login Layout**
- [x] **Step 3: Commit**
`git add app/src/main/java/com/codram/terecojo/utils/GradientAnimator.java app/src/main/res/layout/activity_login.xml && git commit -m "android: implement animated gradient background"`

---

### Task 4: Floating Stat Blocks UI

**Files:**
- Modify: `app/src/main/res/layout/activity_login.xml`
- Create: `app/src/main/res/layout/layout_stat_block.xml` (optional, for reuse)

- [x] **Step 1: Add Stat Blocks to Layout**
- [x] **Step 2: Implement Levitation Animation**
- [x] **Step 3: Commit**
`git add app/src/main/res/layout/activity_login.xml && git commit -m "android: add floating stat blocks to login screen"`

---

### Task 5: Data Integration & Counter Animation

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/LoginActivity.java`

- [x] **Step 1: Fetch Stats on onCreate**
- [x] **Step 2: Implement Counter Animation**
- [x] **Step 3: Verify & Commit**
`git commit -am "android: integrate stats data and counter animation"`
