import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { setNewPassword } from '../../services/authService.js'
import Footer from '../../components/Footer/Footer.jsx'
import '../Login/LoginPage.css'

const INVALID_LINK_MESSAGE = 'Este link é inválido ou expirou. Contate o administrador.'

export default function NewPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')?.trim()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [completed, setCompleted] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    if (password.length < 8) {
      setError('A senha deve ter ao menos 8 caracteres.')
      return
    }
    if (password !== confirm) {
      setError('As senhas não coincidem.')
      return
    }

    setLoading(true)
    try {
      await setNewPassword(token, password)
      setCompleted(true)
    } catch (err) {
      if (err.response?.status === 404) {
        setError(INVALID_LINK_MESSAGE)
      } else {
        setError('Não foi possível definir sua senha agora. Tente novamente mais tarde.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="lp-root">
      <header className="lp-header">
        <span className="lp-header__brand">SyncCarreira</span>
      </header>

      <main className="lp-main">
        <div className="lp-card">
          <div className="lp-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22">
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>

          <h1 className="lp-card__title">{completed ? 'Senha definida' : 'Defina sua senha'}</h1>
          <p className="lp-card__sub">
            {completed
              ? 'Seu acesso está pronto. Entre com seu email e a nova senha.'
              : 'Crie uma senha para concluir seu primeiro acesso.'}
          </p>

          {!token && !completed && (
            <p className="lp-info" role="alert">{INVALID_LINK_MESSAGE}</p>
          )}

          {error && <div className="lp-error" role="alert">{error}</div>}

          {token && !completed && (
            <form onSubmit={handleSubmit} noValidate>
              <div className="lp-field">
                <label htmlFor="new-password">NOVA SENHA</label>
                <input
                  id="new-password"
                  type="password"
                  value={password}
                  onChange={event => setPassword(event.target.value)}
                  placeholder="Mínimo 8 caracteres"
                  autoComplete="new-password"
                  required
                />
              </div>

              <div className="lp-field">
                <label htmlFor="confirm-password">CONFIRMAR SENHA</label>
                <input
                  id="confirm-password"
                  type="password"
                  value={confirm}
                  onChange={event => setConfirm(event.target.value)}
                  placeholder="Repita sua senha"
                  autoComplete="new-password"
                  required
                />
              </div>

              <button type="submit" className="lp-btn" disabled={loading}>
                {loading ? <span className="lp-spinner" aria-label="Salvando senha" /> : 'Definir senha'}
              </button>
            </form>
          )}

          <p className="lp-footer-text">
            <Link to="/login" className="lp-link">Ir para o login</Link>
          </p>
        </div>
      </main>

      <Footer />
    </div>
  )
}