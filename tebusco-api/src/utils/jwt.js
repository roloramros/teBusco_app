import jwt from 'jsonwebtoken'

const SECRET = process.env.JWT_SECRET
if (!SECRET) throw new Error('JWT_SECRET no definida. El servidor no puede arrancar sin ella.')
const EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d'

// Genera un token JWT con los datos del usuario
export const generateToken = (payload) => {
  return jwt.sign(payload, SECRET, { expiresIn: EXPIRES_IN })
}

// Verifica y decodifica un token
export const verifyToken = (token) => {
  return jwt.verify(token, SECRET)
}

// Extrae el token del header Authorization: Bearer <token>
export const extractToken = (req) => {
  const authHeader = req.headers['authorization']
  if (!authHeader || !authHeader.startsWith('Bearer ')) return null
  return authHeader.split(' ')[1]
}
