import { Router } from 'express'
import { authenticate, authorize } from '../middleware/auth.js'
import * as adminController from '../controllers/adminController.js'

const router = Router()

// Proteger todas las rutas para admins
router.use(authenticate, authorize('admin'))

// Estadísticas para el dashboard
router.get('/stats', adminController.getStats)

export default router
