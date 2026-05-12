import express from 'express'
import { authenticate } from '../middleware/auth.js'
import * as solicitudController from '../controllers/solicitudController.js'

const router = express.Router()

// Todas las rutas de solicitudes requieren estar logueado
router.use(authenticate)

router.get('/radar', solicitudController.getTodasSolicitudesActivas)
router.get('/mis-solicitudes', solicitudController.getMisSolicitudes)
router.get('/:id', solicitudController.getSolicitudById)
router.post('/', solicitudController.createSolicitud)

// --- Nuevas rutas para el sistema de ofertas ---

// El chofer responde a una solicitud
router.post('/:solicitud_id/responder', solicitudController.responderSolicitud)

// El pasajero ve las ofertas de su solicitud
router.get('/:solicitud_id/ofertas', solicitudController.getOfertasBySolicitud)

// El pasajero acepta una oferta específica
router.post('/ofertas/:respuesta_id/aceptar', solicitudController.aceptarRespuesta)

// El pasajero rechaza una oferta específica (con motivo opcional)
router.post('/ofertas/:respuesta_id/rechazar', solicitudController.rechazarRespuesta)


// El pasajero cancela su solicitud
router.post('/:id/cancelar', solicitudController.cancelarSolicitud)

export default router
