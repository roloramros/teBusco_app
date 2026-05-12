import { query } from '../config/database.js'
import { success, created, notFound, badRequest, forbidden } from '../utils/response.js'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Función auxiliar para borrar una foto del servidor
 */
const deleteFile = (fotoUrl) => {
  if (!fotoUrl) return
  try {
    const fileName = fotoUrl.split('/').pop()
    const filePath = path.join(__dirname, '../public/uploads/vehicles', fileName)
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath)
    }
  } catch (err) {
    console.error('Error al borrar archivo:', err)
  }
}

/**
 * Obtener todos los vehículos del chofer autenticado
 */
export const getVehicles = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario

    // Primero obtener el ID del chofer asociado al usuario
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return forbidden(res, 'No eres un chofer registrado')
    }

    const choferId = choferRows[0].id

    const { rows } = await query(
      'SELECT * FROM vehiculos WHERE chofer_id = $1 AND activo = true ORDER BY marca ASC',
      [choferId]
    )

    return success(res, rows)
  } catch (err) {
    next(err)
  }
}

/**
 * Añadir un nuevo vehículo
 */
export const addVehicle = async (req, res, next) => {
  try {
    const { id: usuarioId } = req.usuario
    const { marca, placa, tipo, capacidad_pasajeros } = req.body
    
    // Si hay un archivo, generar la URL
    let foto_url = req.body.foto_url || null
    if (req.file) {
      foto_url = `${req.protocol}://${req.get('host')}/uploads/vehicles/${req.file.filename}`
    }

    // Validaciones básicas
    if (!marca || !placa || !tipo) {
      return badRequest(res, 'Marca, placa y tipo son obligatorios')
    }

    // Obtener ID del chofer
    const { rows: choferRows } = await query(
      'SELECT id FROM choferes WHERE usuario_id = $1',
      [usuarioId]
    )

    if (choferRows.length === 0) {
      return forbidden(res, 'No eres un chofer registrado')
    }

    const choferId = choferRows[0].id

    // Insertar vehículo
    const { rows } = await query(
      `INSERT INTO vehiculos (chofer_id, marca, placa, tipo, capacidad_pasajeros, foto_url)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [choferId, marca, placa, tipo, capacidad_pasajeros, foto_url]
    )

    return created(res, rows[0], 'Vehículo añadido correctamente')
  } catch (err) {
    // Manejar error de placa duplicada
    if (err.code === '23505') {
      return badRequest(res, 'La placa ya está registrada en otro vehículo')
    }
    next(err)
  }
}

/**
 * Actualizar un vehículo existente
 */
export const updateVehicle = async (req, res, next) => {
  try {
    const { id: vehicleId } = req.params
    const { id: usuarioId } = req.usuario
    const { marca, placa, tipo, capacidad_pasajeros, activo } = req.body

    // Verificar que el vehículo pertenece al chofer y obtener foto actual
    const { rows: checkRows } = await query(
      `SELECT v.* FROM vehiculos v
       JOIN choferes c ON v.chofer_id = c.id
       WHERE v.id = $1 AND c.usuario_id = $2`,
      [vehicleId, usuarioId]
    )

    if (checkRows.length === 0) {
      if (req.file) deleteFile(`${req.protocol}://${req.get('host')}/uploads/vehicles/${req.file.filename}`)
      return notFound(res, 'Vehículo no encontrado o no te pertenece')
    }

    const currentVehicle = checkRows[0]

    // Si hay un nuevo archivo, generar la nueva URL y borrar la vieja
    let foto_url = currentVehicle.foto_url
    if (req.file) {
      deleteFile(currentVehicle.foto_url)
      foto_url = `${req.protocol}://${req.get('host')}/uploads/vehicles/${req.file.filename}`
    }

    // Actualizar campos (solo los proporcionados)
    const { rows } = await query(
      `UPDATE vehiculos
       SET marca = COALESCE($1, marca),
           placa = COALESCE($2, placa),
           tipo = COALESCE($3, tipo),
           capacidad_pasajeros = COALESCE($4, capacidad_pasajeros),
           foto_url = COALESCE($5, foto_url),
           activo = COALESCE($6, activo)
       WHERE id = $7
       RETURNING *`,
      [marca, placa, tipo, capacidad_pasajeros, foto_url, activo, vehicleId]
    )

    return success(res, rows[0], 'Vehículo actualizado correctamente')
  } catch (err) {
    if (err.code === '23505') {
      return badRequest(res, 'La placa ya está registrada en otro vehículo')
    }
    next(err)
  }
}

/**
 * Eliminar un vehículo (lógico o físico)
 */
export const deleteVehicle = async (req, res, next) => {
  try {
    const { id: vehicleId } = req.params
    const { id: usuarioId } = req.usuario

    // Obtener info del vehículo para borrar la foto
    const { rows: checkRows } = await query(
      `SELECT v.foto_url FROM vehiculos v
       JOIN choferes c ON v.chofer_id = c.id
       WHERE v.id = $1 AND c.usuario_id = $2`,
      [vehicleId, usuarioId]
    )

    if (checkRows.length === 0) {
      return notFound(res, 'Vehículo no encontrado o no te pertenece')
    }

    const { foto_url } = checkRows[0]

    const { rowCount } = await query(
      `DELETE FROM vehiculos
       WHERE id = $1 AND chofer_id = (SELECT id FROM choferes WHERE usuario_id = $2)`,
      [vehicleId, usuarioId]
    )

    if (rowCount > 0) {
      deleteFile(foto_url)
    }

    return success(res, null, 'Vehículo eliminado correctamente')
  } catch (err) {
    next(err)
  }
}
