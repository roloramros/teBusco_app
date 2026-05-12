// Respuesta exitosa
export const success = (res, data = {}, message = 'OK', statusCode = 200) => {
  return res.status(statusCode).json({
    ok:      true,
    message,
    data,
  })
}

// Respuesta de error
export const error = (res, message = 'Error interno', statusCode = 500, errors = null) => {
  const body = { ok: false, message }
  if (errors) body.errors = errors
  return res.status(statusCode).json(body)
}

// Atajos semánticos
export const created  = (res, data, message) => success(res, data, message, 201)
export const badRequest = (res, message, errors) => error(res, message, 400, errors)
export const unauthorized = (res, message = 'No autorizado') => error(res, message, 401)
export const forbidden    = (res, message = 'Acceso denegado') => error(res, message, 403)
export const notFound     = (res, message = 'Recurso no encontrado') => error(res, message, 404)
export const conflict     = (res, message = 'Conflicto con datos existentes') => error(res, message, 409)
