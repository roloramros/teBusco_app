# Driver Verification Restriction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restrict unverified drivers from sending offers by applying the `requireVerificado` middleware to the relevant route.

**Architecture:** We will modify the solicitation routes to include the `requireVerificado` middleware for the `responderSolicitud` endpoint. This leverages existing authentication and verification logic.

**Tech Stack:** Node.js, Express 5, ES Modules.

---

### Task 1: Update Solicitation Routes

**Files:**
- Modify: `tebusco-api/src/routes/solicitud.js`

- [ ] **Step 1: Import `requireVerificado` and apply it to the response route**

In `tebusco-api/src/routes/solicitud.js`, update the imports and the route definition for responding to a solicitation.

```javascript
import express from 'express'
import { authenticate, requireVerificado } from '../middleware/auth.js' // Modified: added requireVerificado
import * as solicitudController from '../controllers/solicitudController.js'

const router = express.Router()

// ... rest of the file ...

// El chofer responde a una solicitud
// Modified: added requireVerificado middleware
router.post('/:solicitud_id/responder', requireVerificado, solicitudController.responderSolicitud)

// ... rest of the file ...
```

- [ ] **Step 2: Verify syntax**

Run a syntax check on the modified file.

Run: `node --check tebusco-api/src/routes/solicitud.js`
Expected: No output (success)

- [ ] **Step 3: Commit the change**

```bash
git add tebusco-api/src/routes/solicitud.js
git commit -m "feat(solicitud): restrict sending offers to verified drivers only"
```
