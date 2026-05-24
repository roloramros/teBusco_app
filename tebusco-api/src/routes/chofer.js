import { Router } from 'express'
import { authenticate, requireVerificado } from '../middleware/auth.js'
import * as choferController from '../controllers/choferController.js'

const router = Router()

router.use(authenticate)

// El chofer activa o desactiva su visibilidad en el mapa
router.put('/visibilidad', requireVerificado, choferController.toggleVisibilidad)

// El ForegroundService actualiza la posición del chofer cada 60s
router.put('/ubicacion', requireVerificado, choferController.actualizarUbicacion)

// El pasajero obtiene choferes visibles cercanos
router.get('/disponibles', choferController.getChoferesDisponibles)

export default router
