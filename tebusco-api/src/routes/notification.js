import { Router } from 'express'
import { 
  getMyNotifications, 
  markAsRead, 
  markAllAsRead, 
  deleteNotification, 
  deleteAllNotifications 
} from '../controllers/notificationController.js'
import { authenticate } from '../middleware/auth.js'

const router = Router()

// Todas las rutas de notificaciones requieren autenticación
router.use(authenticate)

router.get('/', getMyNotifications)
router.patch('/:id/read', markAsRead)
router.post('/read-all', markAllAsRead)
router.delete('/:id', deleteNotification)
router.delete('/', deleteAllNotifications)

export default router
