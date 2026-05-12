import { validationResult } from 'express-validator'
import { badRequest } from '../utils/response.js'

// Middleware: si hay errores de validación, devuelve 400 con el detalle
export const validar = (req, res, next) => {
  const errors = validationResult(req)
  if (!errors.isEmpty()) {
    const detalles = errors.array().map(e => ({
      campo:   e.path || e.param,
      mensaje: e.msg,
    }))
    return badRequest(res, 'Datos inválidos', detalles)
  }
  next()
}
