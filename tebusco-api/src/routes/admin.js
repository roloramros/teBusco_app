import { Router } from 'express'
import { authenticate, authorize } from '../middleware/auth.js'

const router = Router()

// Proteger todas las rutas para admins
router.use(authenticate, authorize('admin'))

// Placeholder for routes
// router.get('/stats', ...)

export default router
