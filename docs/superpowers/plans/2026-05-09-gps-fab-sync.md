# GPS FAB Synchronization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sync the GPS FAB (`fabMyLocation`) position with the bottom sheet in `MainActivity` so it stays just above it and moves as the panel expands.

**Architecture:** Use `CoordinatorLayout` anchoring for `fabMyLocation` to automatically follow the `bottomSheet` and adjust window insets handling to prevent positioning conflicts.

**Tech Stack:** Android, XML (CoordinatorLayout), Java (ViewCompat, WindowInsets).

---

### Task 1: Update XML Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Anchor `fabMyLocation` to the bottom sheet**
Modify `app/src/main/res/layout/activity_main.xml` to remove `layout_gravity` and `layout_marginBottom` from `fabMyLocation`, and add `app:layout_anchor="@id/bottomSheet"` and `app:layout_anchorGravity="top|end"`.

```xml
        <!-- FAB de Mi Ubicación -->
        <com.google.android.material.floatingactionbutton.FloatingActionButton
            android:id="@+id/fabMyLocation"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_margin="16dp"
            app:layout_anchor="@id/bottomSheet"
            app:layout_anchorGravity="top|end"
            android:contentDescription="Mi ubicación"
            android:src="@drawable/ic_my_location"
            app:backgroundTint="@color/white"
            app:tint="@color/primary_blue"
            android:elevation="20dp" />
```

### Task 2: Adjust Window Insets in MainActivity

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/MainActivity.java`

- [ ] **Step 1: Override Inset Listener for `fabMyLocation`**
In `MainActivity.java`, update `setupWindowInsets()` to override the listener set by `BaseActivity`. This prevents the manual `bottomMargin` from interfering with the anchor and ensures it only respects right margins for the system navigation bar.

```java
    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnMenu, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int baseMargin = (int) (16 * getResources().getDisplayMetrics().density);
            params.topMargin = baseMargin + systemBars.top;
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabMyLocation, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
            // Reseteamos bottomMargin ya que ahora se posiciona respecto al ancla
            params.bottomMargin = (int) (16 * getResources().getDisplayMetrics().density); 
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.fabConfirmLocation, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int baseMargin = (int) (172 * getResources().getDisplayMetrics().density);
            params.bottomMargin = baseMargin + systemBars.bottom;
            params.rightMargin = (int) (16 * getResources().getDisplayMetrics().density) + systemBars.right;
            v.setLayoutParams(params);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheet, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
    }
```
