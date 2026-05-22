import 'dotenv/config'
import pg from 'pg'

const { Pool } = pg
const pool = new Pool({
  host:     process.env.DB_HOST,
  port:     process.env.DB_PORT,
  database: process.env.DB_NAME,
  user:     process.env.DB_USER,
  password: process.env.DB_PASSWORD,
})

async function checkColumns() {
  try {
    const res = await pool.query(`
      SELECT table_name, column_name 
      FROM information_schema.columns 
      WHERE table_schema = 'public' 
      AND table_name IN ('usuarios', 'choferes', 'solicitudes') 
      ORDER BY table_name, ordinal_position
    `)
    res.rows.forEach(row => console.log(`${row.table_name}.${row.column_name}`))
  } catch (err) {
    console.error('Error:', err.message)
  } finally {
    await pool.end()
  }
}

checkColumns()
