/**
 * @file AuthContext.jsx
 * @description Contexto global de autenticação do SyncCarreira.
 *
 * Ajustado para o contrato real do backend:
 *  - Restaura os dados do usuário autenticado via GET /users/me
 *  - Normaliza campos: backend usa `name`, frontend usa `nome`
 *  - authService.me() agora chama GET /users/{id}
 */

import { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react'
import * as authService from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser]             = useState(null)
  const [loading, setLoading]       = useState(false)
  const [loadingInit, setLoadingInit] = useState(true)

  // ── Restauração de sessão ──────────────────────────────────
  useEffect(() => {
    const token = localStorage.getItem('token')

    if (!token) {
      setLoadingInit(false)
      return
    }

    authService.me()
        .then((usuario) => setUser(usuario))
        .catch(() => {
          localStorage.removeItem('token')
        })
        .finally(() => setLoadingInit(false))
  }, [])

  // ── Login ──────────────────────────────────────────────────
  const login = useCallback(async (email, password) => {
    setLoading(true)
    try {
      const data = await authService.login(email, password)

      // Normaliza o objeto de usuário (backend usa `name`, app usa `nome`)
      const usuario = data.usuario || data.user || data
      if (usuario?.name && !usuario?.nome) {
        usuario.nome = usuario.name
      }

      setUser(usuario)
      return { success: true }

    } catch (err) {
      return { success: false, message: err.message }
    } finally {
      setLoading(false)
    }
  }, [])

  // ── Logout ─────────────────────────────────────────────────
  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  // useMemo evita criar um novo objeto a cada render (e re-render desnecessário dos consumidores)
  const value = useMemo(
    () => ({ user, loading, login, logout }),
    [user, loading, login, logout]
  )

  if (loadingInit) return null

  return (
      <AuthContext.Provider value={value}>
        {children}
      </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de <AuthProvider>')
  return ctx
}