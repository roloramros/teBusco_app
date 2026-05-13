import { Router } from 'express'
import { authenticate, authorize } from '../middleware/auth.js'
import * as adminController from '../controllers/adminController.js'

const router = Router()

// Proteger todas las rutas para admins
router.use(authenticate, authorize('admin'))

// Estadísticas para el dashboard
router.get('/stats', adminController.getStats)

// Gestión de Chofer
router.get('/choferes', adminController.getChoferes)
router.get('/choferes/:id', adminController.getChoferById)
router.post('/choferes/:id/aprobar', adminController.aprobarChofer)
router.post('/choferes/:id/rechazar', adminController.rechazarChofer)

export default router
