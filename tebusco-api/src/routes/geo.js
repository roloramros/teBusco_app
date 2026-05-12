import { Router } from 'express'
import { getProvincias, getMunicipiosByProvincia, getVehicleTypes } from '../controllers/geoController.js'
import { getPublicStats } from '../controllers/statsController.js'

const router = Router()

// GET /api/geo/stats
// Estadísticas públicas de la plataforma
router.get('/stats', getPublicStats)

// GET /api/geo/provincias
// Poblar el primer spinner (provincias)
router.get('/provincias', getProvincias)

// GET /api/geo/provincias/:id/municipios
// Poblar el segundo spinner cuando el usuario selecciona una provincia
router.get('/provincias/:id/municipios', getMunicipiosByProvincia)

// GET /api/geo/vehicle-types
// Obtener tipos de vehículos para el spinner
router.get('/vehicle-types', getVehicleTypes)

export default router
