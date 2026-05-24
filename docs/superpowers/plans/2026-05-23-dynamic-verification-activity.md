# Dynamic License Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a dynamic license verification screen in the Android app that reflects the real-time status of a driver's license.

**Architecture:** Backend endpoint in `authController.js` for drivers to fetch their license. Android `Licencia` model and updated `VerificationActivity` with dynamic UI.

**Tech Stack:** Node.js, Express, PostgreSQL (Backend), Java, Android XML, Retrofit (Android).

---

### Task 1: Backend - Add `getMiLicencia` Endpoint

**Files:**
- Modify: `tebusco-api/src/controllers/authController.js`
- Modify: `tebusco-api/src/routes/auth.js`

- [ ] **Step 1: Add `getMiLicencia` to `authController.js`**

Add at the end of the file:
```javascript
/**
 * GET /api/auth/mi-licencia
 * El chofer consulta el estado de su propia licencia
 */
export const getMiLicencia = async (req, res) => {
  try {
    const { id: usuarioId } = req.usuario

    // Obtener el chofer_id
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return response.forbidden(res, 'Solo los choferes pueden consultar su licencia')
    }

    const choferId = choferRows[0].id

    const { rows } = await query(`
      SELECT
        l.estado,
        l.trial_inicio,
        l.trial_fin,
        l.suscripcion_inicio,
        l.suscripcion_fin,
        l.ultimo_pago,
        l.monto_mensual,
        CASE
          WHEN l.estado = 'TRIAL_ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.trial_fin - NOW())::int)
          WHEN l.estado = 'ACTIVO'
            THEN GREATEST(0, EXTRACT(DAY FROM l.suscripcion_fin - NOW())::int)
          ELSE 0
        END AS dias_restantes
      FROM licencias_chofer l
      WHERE l.chofer_id = $1
    `, [choferId])

    if (rows.length === 0) {
      // El chofer existe pero aún no tiene licencia (caso legacy)
      return response.success(res, {
        estado: 'SIN_LICENCIA',
        dias_restantes: 0
      })
    }

    return response.success(res, rows[0])
  } catch (err) {
    console.error('❌ Error en /mi-licencia:', err.message)
    throw err
  }
}
```

- [ ] **Step 2: Add route to `auth.js`**

Add before `export default router`:
```javascript
router.get('/mi-licencia', authenticate, authController.getMiLicencia)
```

---

### Task 2: Android - Create `Licencia` Model

**Files:**
- Create: `app/src/main/java/com/codram/terecojo/data/model/Licencia.java`

- [ ] **Step 1: Create the `Licencia.java` file**

```java
package com.codram.terecojo.data.model;

import com.google.gson.annotations.SerializedName;

public class Licencia {

    @SerializedName("estado")
    private String estado;

    @SerializedName("dias_restantes")
    private int diasRestantes;

    @SerializedName("trial_inicio")
    private String trialInicio;

    @SerializedName("trial_fin")
    private String trialFin;

    @SerializedName("suscripcion_fin")
    private String suscripcionFin;

    @SerializedName("ultimo_pago")
    private String ultimoPago;

    @SerializedName("monto_mensual")
    private double montoMensual;

    public String getEstado() { return estado; }
    public int getDiasRestantes() { return diasRestantes; }
    public String getTrialInicio() { return trialInicio; }
    public String getTrialFin() { return trialFin; }
    public String getSuscripcionFin() { return suscripcionFin; }
    public String getUltimoPago() { return ultimoPago; }
    public double getMontoMensual() { return montoMensual; }

    // Helpers de lógica
    public boolean puedeOperar() {
        return "TRIAL_ACTIVO".equals(estado) || "ACTIVO".equals(estado);
    }

    public boolean esTrial() {
        return "TRIAL_ACTIVO".equals(estado) || "TRIAL_EXPIRADO".equals(estado);
    }

    public boolean necesitaPago() {
        return "TRIAL_EXPIRADO".equals(estado) || "SUSPENDIDO".equals(estado);
    }

    public boolean estaBloqueado() {
        return "BLOQUEADO".equals(estado);
    }
}
```

---

### Task 3: Android - Update `ApiService`

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/data/remote/ApiService.java`

- [ ] **Step 1: Add `getMiLicencia` call and import**

Add import:
```java
import com.codram.terecojo.data.model.Licencia;
```

Add call:
```java
    @GET("api/auth/mi-licencia")
    Call<ApiResponse<Licencia>> getMiLicencia();
```

---

### Task 4: Android - Create `bg_rounded_gray` Drawable

**Files:**
- Create: `app/src/main/res/drawable/bg_rounded_gray.xml`

- [ ] **Step 1: Create the drawable file**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#F0F0F0" />
    <corners android:radius="8dp" />
</shape>
```

---

### Task 5: Android - Replace `activity_verification.xml` Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_verification.xml`

- [ ] **Step 1: Replace the layout content with the provided XML**

(Provided in the prompt)

---

### Task 6: Android - Replace `VerificationActivity.java`

**Files:**
- Modify: `app/src/main/java/com/codram/terecojo/VerificationActivity.java`

- [ ] **Step 1: Replace the class content with the provided Java code**

(Provided in the prompt)

---
