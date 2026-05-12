import pg from 'pg'
import dotenv from 'dotenv'

dotenv.config()

const { Pool } = pg

const pool = new Pool({
  host:     process.env.DB_HOST     || 'localhost',
  port:     parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME     || 'tebusco_db',
  user:     process.env.DB_USER     || 'tebusco_user',
  password: process.env.DB_PASSWORD || '',
  // Configuración recomendada para VPS con recursos limitados
  max:              10,    // máximo 10 conexiones simultáneas
  idleTimeoutMillis: 30000, // cerrar conexiones inactivas a los 30s
  connectionTimeoutMillis: 5000, // timeout si no hay conexión en 5s
})

// Verificar conexión al iniciar
// pool.on('connect', () => {
//   console.log('✅ Conectado a PostgreSQL')
// })

pool.on('error', (err) => {
  console.error('❌ Error inesperado en el pool de PostgreSQL:', err.message)
})

// Helper: ejecutar query con manejo de errores centralizado
export const query = async (text, params) => {
  const start = Date.now()
  try {
    const result = await pool.query(text, params)
    const duration = Date.now() - start
    if (process.env.NODE_ENV === 'development') {
      console.log(`🔍 Query (${duration}ms):`, text.substring(0, 80))
    }
    return result
  } catch (err) {
    console.error('❌ Error en query:', err.message)
    throw err
  }
}

// Helper: obtener una conexión del pool (para transacciones)
export const getClient = () => pool.connect()

export default pool
