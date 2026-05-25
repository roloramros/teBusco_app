import { Router } from 'express'
import { authenticate, authorize } from '../middleware/auth.js'      
import * as adminController from '../controllers/adminController.js' 
import * as licenciaController from '../controllers/licenciaController.js' 

const router = Router()

// Proteger todas las rutas para admins
router.use(authenticate, authorize('admin'))

// Estad�sticas para el dashboard
router.get('/stats', adminController.getStats)

// Gesti�n de Chofer
router.get('/choferes', adminController.getChoferes)
router.get('/choferes/:id', adminController.getChoferById)
router.post('/choferes/:id/aprobar', adminController.aprobarChofer)  
router.post('/choferes/:id/rechazar', adminController.rechazarChofer)

// Gestión de Usuarios
router.get('/usuarios', adminController.getUsuarios)
router.patch('/usuarios/:id/toggle-activo', adminController.toggleUsuarioActivo)
router.delete('/usuarios/:id', adminController.deleteUsuario)

// Monitoreo de Solicitudes
router.get('/solicitudes', adminController.getSolicitudes)

// Notificaciones
router.post('/notificaciones/broadcast', adminController.broadcastNotification)

// Gestión de Licencias
router.get('/licencias',                              licenciaController.getLicencias)
router.get('/licencias/stats',                        licenciaController.getLicenciasStats)
router.get('/licencias/:chofer_id',                   licenciaController.getLicenciaByChofer)
router.post('/licencias/actualizar-cuota-masiva',    licenciaController.actualizarCuotaMasiva)
router.post('/licencias/:chofer_id/registrar-pago',   licenciaController.registrarPago)
router.post('/licencias/:chofer_id/cambiar-estado',   licenciaController.cambiarEstado)

export default router

