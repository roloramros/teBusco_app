import express from 'express'
import { authenticate, authorize } from '../middleware/auth.js'
import * as vehicleController from '../controllers/vehicleController.js'
import { uploadVehiclePhoto } from '../utils/upload.js'

const router = express.Router()

// Todas las rutas de vehículos requieren autenticación y rol de chofer
router.use(authenticate)
router.use(authorize('chofer', 'driver')) // Soportamos ambos nombres de rol por si acaso

router.get('/', vehicleController.getVehicles)
router.post('/', uploadVehiclePhoto.single('foto'), vehicleController.addVehicle)
router.put('/:id', uploadVehiclePhoto.single('foto'), vehicleController.updateVehicle)
router.delete('/:id', vehicleController.deleteVehicle)

export default router
