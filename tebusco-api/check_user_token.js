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

console.log('Configuración del pool:', {
  host: process.env.DB_HOST,
  port: process.env.DB_PORT,
  database: process.env.DB_NAME,
  user: process.env.DB_USER
});

async function checkUser() {
  console.log('Iniciando consulta...');
  try {
    const res = await pool.query("SELECT id, nombre, fcm_token FROM usuarios WHERE id = '2a86a30c-ece5-4770-980e-e55eb36ffb0a';")
    if (res.rows.length === 0) {
      console.log('Usuario no encontrado en la tabla usuarios.');
    } else {
      console.log('Usuario encontrado:', res.rows[0])
    }
  } catch (err) {
    console.error('Error en la query:', err);
  } finally {
    console.log('Cerrando pool.');
    await pool.end()
  }
}

checkUser()
