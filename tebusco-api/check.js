import 'dotenv/config'
import pg from 'pg'

const { Pool } = pg
const pool = new Pool({
  host:     process.env.DB_HOST     || 'localhost',
  port:     parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME     || 'tebusco_db',
  user:     process.env.DB_USER     || 'tebusco_user',
  password: process.env.DB_PASSWORD || '',
})

async function check() {
  try {
    const res = await pool.query("SELECT column_name FROM information_schema.columns WHERE table_name = 'choferes'")
    console.log("Columns of choferes:")
    console.log(res.rows.map(r => r.column_name))
    
    const res2 = await pool.query("SELECT * FROM choferes LIMIT 1")
    console.log("Sample data:")
    console.log(res2.rows)
  } catch(e) {
    console.error(e)
  }
  process.exit(0)
}
check()