import { query } from '../config/database.js'
import * as res from '../utils/response.js'

// ─────────────────────────────────────────────────────────
// GET /api/geo/provincias
// Devuelve todas las provincias activas
// ─────────────────────────────────────────────────────────
export const getProvincias = async (req, reply) => {
  console.log('--- GET /api/geo/provincias ---')
  try {
    const { rows } = await query(
      `SELECT id, nombre, codigo
       FROM provincias
       WHERE activa = true
       ORDER BY nombre ASC`
    )
    console.log(`✅ Provincias cargadas: ${rows.length}`)
    return res.success(reply, rows)
  } catch (error) {
    console.error('❌ Error en getProvincias:', error.message)
    throw error
  }
}

// ─────────────────────────────────────────────────────────
// GET /api/geo/provincias/:id/municipios
// Devuelve los municipios de una provincia específica
// ─────────────────────────────────────────────────────────
export const getMunicipiosByProvincia = async (req, reply) => {
  const { id } = req.params
  const provinciaId = parseInt(id)

  if (isNaN(provinciaId)) {
    return res.badRequest(reply, 'ID de provincia inválido')
  }

  // Verificar que la provincia existe
  const { rows: prov } = await query(
    'SELECT id, nombre FROM provincias WHERE id = $1 AND activa = true',
    [provinciaId]
  )

  if (prov.length === 0) {
    return res.notFound(reply, 'Provincia no encontrada')
  }

  const { rows } = await query(
    `SELECT id, nombre, codigo
     FROM municipios
     WHERE provincia_id = $1 AND activo = true
     ORDER BY nombre ASC`,
    [provinciaId]
  )

  return res.success(reply, {
    provincia: prov[0],
    municipios: rows,
  })
}

// ─────────────────────────────────────────────────────────
// GET /api/geo/vehicle-types
// Devuelve los tipos de vehículos disponibles (del ENUM)
// ─────────────────────────────────────────────────────────
export const getVehicleTypes = async (req, reply) => {
  try {
    const { rows } = await query(
      "SELECT enumlabel FROM pg_enum JOIN pg_type ON pg_enum.enumtypid = pg_type.oid WHERE pg_type.typname = 'tipo_vehiculo'"
    )
    
    // Si no podemos leer el enum por alguna razón, devolvemos los valores por defecto
    const types = rows.length > 0 
      ? rows.map(r => r.enumlabel)
      : ['carro', 'camion', 'pizicorre', 'triciclo electrico', 'moto', 'bicitaxi'];

    return res.success(reply, types)
  } catch (error) {
    console.error('❌ Error en getVehicleTypes:', error.message)
    throw error
  }
}
