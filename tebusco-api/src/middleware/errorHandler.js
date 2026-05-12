// Middleware global de manejo de errores
// Debe ser el último middleware registrado en Express
export const errorHandler = (err, req, res, next) => {
  console.error('❌ Error no controlado:', err.message)

  // Error de violación de constraint en PostgreSQL (ej: email duplicado)
  if (err.code === '23505') {
    return res.status(409).json({
      ok: false,
      message: 'Ya existe un registro con esos datos',
    })
  }

  // Error de clave foránea
  if (err.code === '23503') {
    return res.status(400).json({
      ok: false,
      message: 'Referencia a un recurso que no existe',
    })
  }

  // Error genérico
  const statusCode = err.statusCode || 500
  const message = process.env.NODE_ENV === 'production'
    ? 'Error interno del servidor'
    : err.message

  res.status(statusCode).json({ ok: false, message })
}

// Middleware para rutas no encontradas (404)
export const notFoundHandler = (req, res) => {
  res.status(404).json({
    ok: false,
    message: `Ruta no encontrada: ${req.method} ${req.originalUrl}`,
  })
}
