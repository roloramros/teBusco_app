import 'dotenv/config'
import express from 'express'
import helmet from 'helmet'
import cors from 'cors'
import rateLimit from 'express-rate-limit'
import path from 'path'
import { fileURLToPath } from 'url'

import authRoutes from './routes/auth.js'
import geoRoutes  from './routes/geo.js'
import vehicleRoutes from './routes/vehicle.js'
import solicitudRoutes from './routes/solicitud.js'
import notificationRoutes from './routes/notification.js'
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js'

const app  = express()
const PORT = process.env.PORT || 3000

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

// Confiar en el proxy inverso (Nginx, Cloudflare, etc.) para express-rate-limit
app.set('trust proxy', 1)

// ══════════════════════════════════════════════════════════
// SEGURIDAD Y MIDDLEWARES GLOBALES
// ══════════════════════════════════════════════════════════

// Helmet: cabeceras HTTP de seguridad
app.use(helmet())

// Servir archivos estáticos (para las fotos de los vehículos)
app.use('/uploads', express.static(path.join(__dirname, 'public/uploads')))

// CORS: ajusta el origin a tu dominio en producción
app.use(cors({
  origin: process.env.NODE_ENV === 'production'
    ? process.env.FRONTEND_URL || '*'
    : '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization'],
}))

// Rate limiting global: máx 100 requests / 15 min por IP
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000'),
  max:      parseInt(process.env.RATE_LIMIT_MAX       || '100'),
  standardHeaders: true,
  legacyHeaders:   false,
  message: { ok: false, message: 'Demasiadas peticiones. Intenta más tarde.' },
})
app.use(limiter)

// Rate limiting más estricto para auth: máx 10 intentos / 15 min
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 10,
  message: { ok: false, message: 'Demasiados intentos de autenticación. Espera 15 minutos.' },
})

// Parser JSON
app.use(express.json({ limit: '1mb' }))
app.use(express.urlencoded({ extended: true }))

// ══════════════════════════════════════════════════════════
// RUTAS
// ══════════════════════════════════════════════════════════

// Health check — útil para monitorear el servidor desde el VPS
app.get('/health', (req, res) => {
  res.json({
    ok:      true,
    app:     'Te Busco API',
    version: '1.0.0',
    uptime:  process.uptime(),
    timestamp: new Date().toISOString(),
  })
})

// Rutas de autenticación
// Aplicamos el authLimiter (estricto) solo a login y registro para proteger contra fuerza bruta
app.post('/api/auth/login', authLimiter)
app.post('/api/auth/registro', authLimiter)

// Luego cargamos todas las rutas de auth (logout, me, etc. usarán el limiter global)
app.use('/api/auth', authRoutes)

// Rutas geográficas — públicas, sin auth, para poblar spinners
app.use('/api/geo', geoRoutes)

// Rutas de vehículos — privadas para choferes
app.use('/api/vehicles', vehicleRoutes)

// Rutas de solicitudes — privadas
app.use('/api/solicitudes', solicitudRoutes)

// Rutas de notificaciones — privadas
app.use('/api/notificaciones', notificationRoutes)

// ══════════════════════════════════════════════════════════
// MANEJO DE ERRORES (siempre al final)
// ══════════════════════════════════════════════════════════
app.use(notFoundHandler)
app.use(errorHandler)

console.log('🚀 Iniciando proceso de arranque...')

// ══════════════════════════════════════════════════════════
// INICIAR SERVIDOR
// ══════════════════════════════════════════════════════════
const startServer = async () => {
  try {
    console.log('📡 Paso 1: Intentando levantar servidor Express...')
    app.listen(PORT, () => {
      console.log(`\n🚖  Te Busco API`)
      console.log(`🌐  Corriendo en http://localhost:${PORT}`)
      console.log(`━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n`)
    }).on('error', (err) => {
      if (err.code === 'EADDRINUSE') {
        console.error(`❌ El puerto ${PORT} ya está en uso`)
      } else {
        console.error('❌ Error al iniciar servidor:', err.message)
      }
    })
  } catch (err) {
    console.error('❌ Error crítico en startup:', err.message)
    setTimeout(() => process.exit(1), 5000)
  }
}

console.log('⏳ Llamando a startServer()...')
startServer()

export default app
