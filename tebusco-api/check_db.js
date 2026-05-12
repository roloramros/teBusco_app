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

async function checkTables() {
  try {
    const res = await pool.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public'
      ORDER BY table_name;
    `)
    console.log('Tablas encontradas:')
    res.rows.forEach(row => console.log(`- ${row.table_name}`))
    
    const types = await pool.query(`
      SELECT n.nspname as schema, t.typname as type 
      FROM pg_type t 
      LEFT JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace 
      WHERE (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) 
      AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid)
      AND n.nspname = 'public';
    `)
    console.log('\nTipos ENUM encontrados:')
    types.rows.forEach(row => console.log(`- ${row.type}`))
    
  } catch (err) {
    console.error('Error:', err.message)
  } finally {
    await pool.end()
  }
}

checkTables()
