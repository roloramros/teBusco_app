# Disable Offer Button for Unverified Drivers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve UX by disabling the "OFERTAR" button for unverified drivers in the Android application.

**Architecture:** Update the user data model to include verification status, modify the `RideRequestAdapter` to accept and use this status to disable the button, and update the relevant activities to pass the status and prevent dialog opening for unverified users.

**Tech Stack:** Android (Java), Retrofit, GSON.

---

### Task 1: Update AuthResponse.User Model

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/data/model/AuthResponse.java`

- [ ] **Step 1: Add verificado field to User class**

```java
public static class User {
    // ... existing fields ...
    private boolean verificado; // Added

    // ... existing getters ...
    public boolean isVerificado() { return verificado; } // Added
}
```

- [ ] **Step 2: Commit change**

```bash
git add app/src/main/java/com/codram/terecojo/data/model/AuthResponse.java
git commit -m "feat(model): add verificado field to User model"
```

---

### Task 2: Update RideRequestAdapter

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java`

- [ ] **Step 1: Add isUserVerified field and update constructor**

```java
public class RideRequestAdapter extends RecyclerView.Adapter<RideRequestAdapter.ViewHolder> {
    private List<RideRequest> requests;
    private OnRideActionListener listener;
    private boolean isUserVerified; // Added

    public RideRequestAdapter(List<RideRequest> requests, boolean isUserVerified, OnRideActionListener listener) { // Updated
        this.requests = requests;
        this.isUserVerified = isUserVerified; // Added
        this.listener = listener;
    }
    // ...
}
```

- [ ] **Step 2: Update onBindViewHolder to disable button if not verified**

```java
        if (request.isHaRespondido()) {
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setAlpha(0.5f);
            setTextOnButton(holder.btnAccept, "OFERTADO");
        } else if (!isUserVerified) { // Added block
            holder.btnAccept.setEnabled(false);
            holder.btnAccept.setAlpha(0.5f);
            setTextOnButton(holder.btnAccept, "PENDIENTE");
        } else {
            holder.btnAccept.setEnabled(true);
            holder.btnAccept.setAlpha(1.0f);
            setTextOnButton(holder.btnAccept, "OFERTAR");
        }
```

*Note: Helper method `setTextOnButton` should be added or logic inlined as per original code style.*

- [ ] **Step 3: Commit change**

```bash
git add app/src/main/java/com/codram/terecojo/ui/adapter/RideRequestAdapter.java
git commit -m "feat(ui): disable offer button in adapter for unverified users"
```

---

### Task 3: Update DriverProfileActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/DriverProfileActivity.java`

- [ ] **Step 1: Pass verification status to adapter and check in onAccept**

```java
    private void setupRequestsList() {
        AuthResponse.User user = SessionManager.getInstance(this).getUser(); // Added
        boolean verified = user != null && user.isVerificado(); // Added
        adapter = new RideRequestAdapter(requests, verified, this); // Updated
        profileBinding.rvRequests.setLayoutManager(new LinearLayoutManager(this));
        profileBinding.rvRequests.setAdapter(adapter);
    }

    @Override
    public void onAccept(RideRequest request) {
        AuthResponse.User user = SessionManager.getInstance(this).getUser(); // Added
        if (user != null && !user.isVerificado()) { // Added check
            Toast.makeText(this, "Tu cuenta está pendiente de verificación", Toast.LENGTH_SHORT).show();
            return;
        }
        showMakeOfferDialog(request);
    }
```

- [ ] **Step 2: Commit change**

```bash
git add app/src/main/java/com/codram/terecojo/DriverProfileActivity.java
git commit -m "feat(ui): update DriverProfileActivity to handle verification status"
```

---

### Task 4: Update DriverActivity (Radar & Map)

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/DriverActivity.java`

- [ ] **Step 1: Update adapter initialization in setupRequestsList**

```java
    private void setupRequestsList() {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        boolean verified = user != null && user.isVerificado();
        adapter = new RideRequestAdapter(radarRequests, verified, this);
        // ...
    }
```

- [ ] **Step 2: Check verification in onAccept**

```java
    @Override
    public void onAccept(RideRequest request) {
        AuthResponse.User user = SessionManager.getInstance(this).getUser();
        if (user != null && !user.isVerificado()) {
            Toast.makeText(this, "Tu cuenta está pendiente de verificación", Toast.LENGTH_SHORT).show();
            return;
        }
        showMakeOfferDialog(request);
    }
```

- [ ] **Step 3: Update Map Marker Click logic**

Search for `displayMarkers` or `OnMarkerClickListener` and ensure that if an "Offer" button is shown in an info window, it's also restricted.

- [ ] **Step 4: Commit change**

```bash
git add app/src/main/java/com/codram/terecojo/DriverActivity.java
git commit -m "feat(ui): update DriverActivity to handle verification status"
```
