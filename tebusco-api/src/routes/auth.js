import { Router } from 'express'
import { body } from 'express-validator'
import { 
  registro, 
  login, 
  logout, 
  me, 
  updateFcmToken,
  getMisSesiones,
  revocarSesion,
  revocarTodasLasSesiones 
} from '../controllers/authController.js'
import { authenticate } from '../middleware/auth.js'
import { validar } from '../middleware/validar.js'

const router = Router()

// ── Reglas de validación ──────────────────────────────────

const reglasRegistro = [
  body('nombre')
    .trim()
    .notEmpty().withMessage('El nombre es obligatorio')
    .isLength({ min: 2, max: 100 }).withMessage('El nombre debe tener entre 2 y 100 caracteres'),

  body('username')
    .trim()
    .notEmpty().withMessage('El nombre de usuario es obligatorio')
    .matches(/^[a-zA-Z0-9._]+$/).withMessage('El nombre de usuario solo debe contener letras, números, puntos o guiones bajos')
    .isLength({ min: 3, max: 50 }).withMessage('El username debe tener entre 3 y 50 caracteres'),

  body('password')
    .isLength({ min: 6 }).withMessage('La contraseña debe tener al menos 6 caracteres'),

  body('tipo')
    .optional()
    .isIn(['pasajero', 'chofer']).withMessage('Tipo debe ser pasajero o chofer'),

  body('email')
    .optional({ nullable: true, checkFalsy: true })
    .isEmail().withMessage('Email inválido')
    .normalizeEmail(),

  body('telefono')
    .optional({ nullable: true, checkFalsy: true })
    .isMobilePhone('any').withMessage('Número de teléfono inválido'),

  body('provincia_id')
    .optional({ nullable: true })
    .isInt({ min: 1, max: 16 }).withMessage('provincia_id inválido'),

  body('municipio_id')
    .optional({ nullable: true })
    .isInt({ min: 1 }).withMessage('municipio_id inválido'),

  // Al menos uno: email o teléfono
  body().custom((_, { req }) => {
    if (!req.body.email && !req.body.telefono) {
      throw new Error('Debes proporcionar al menos un email o un número de teléfono')
    }
    return true
  }),
]

const reglasLogin = [
  body('identificador')
    .trim()
    .notEmpty().withMessage('Email o teléfono es obligatorio'),

  body('password')
    .notEmpty().withMessage('La contraseña es obligatoria'),
]

// ── Rutas públicas ────────────────────────────────────────

// POST /api/auth/registro
router.post('/registro', reglasRegistro, validar, registro)

// POST /api/auth/login
router.post('/login', reglasLogin, validar, login)

// ── Rutas protegidas ──────────────────────────────────────

// POST /api/auth/logout
router.post('/logout', authenticate, logout)

// GET /api/auth/me  — perfil del usuario autenticado
router.get('/me', authenticate, me)

// POST /api/auth/update-fcm-token
router.post('/update-fcm-token', authenticate, updateFcmToken)

// NUEVO — Gestión de sesiones
router.get('/sesiones', authenticate, getMisSesiones)
router.delete('/sesiones/:id', authenticate, revocarSesion)
router.delete('/sesiones', authenticate, revocarTodasLasSesiones)

export default router
