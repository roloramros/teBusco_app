import { describe, it, expect, vi } from 'vitest'
import request from 'supertest'
import app from '../src/index.js'
import * as db from '../src/config/database.js'

vi.mock('../src/config/database.js', () => ({
  query: vi.fn()
}))

describe('GET /api/geo/stats', () => {
  it('should return public statistics', async () => {
    db.query.mockImplementation((sql) => {
      if (sql.includes('usuarios')) return Promise.resolve({ rows: [{ count: 10 }] })
      if (sql.includes('choferes')) return Promise.resolve({ rows: [{ count: 5 }] })
      if (sql.includes('completada')) return Promise.resolve({ rows: [{ count: 20 }] })
      if (sql.includes('activa')) return Promise.resolve({ rows: [{ count: 3 }] })
      return Promise.resolve({ rows: [] })
    })

    const response = await request(app).get('/api/geo/stats')
    
    expect(response.status).toBe(200)
    expect(response.body.ok).toBe(true)
    expect(response.body.data).toEqual({
      total_usuarios: 10,
      total_choferes: 5,
      viajes_completados: 20,
      viajes_activos: 3
    })
  })
})
