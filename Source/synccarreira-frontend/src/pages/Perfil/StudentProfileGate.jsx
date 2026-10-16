import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { getMyStudentProfile } from '../../services/studentProfileService.js'
import './StudentProfilePage.css'

function hasStudentRole(user) {
  const studentRoles = ['ROLE_USER', 'ROLE_STUDENT', 'ROLE_ALUNO']
  return user?.roles?.some(role => studentRoles.includes(role.authority))
    || studentRoles.includes(user?.perfil)
}

export default function StudentProfileGate({ children }) {
  const { user } = useAuth()
  const location = useLocation()
  const [status, setStatus] = useState('loading')
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    if (!hasStudentRole(user)) {
      setStatus('ready')
      return
    }

    let active = true
    getMyStudentProfile()
      .then(profile => {
        if (active) setStatus(profile.profileCompleted ? 'ready' : 'incomplete')
      })
      .catch(() => {
        if (active) setStatus('error')
      })

    return () => { active = false }
  }, [user, retry])

  if (!hasStudentRole(user)) return children

  if (status === 'loading') {
    return <main className="sp-gate-state" role="status">Verificando seu perfil…</main>
  }

  if (status === 'incomplete') {
    return <Navigate to="/perfil" replace state={{ from: location.pathname }} />
  }

  if (status === 'error') {
    return (
      <main className="sp-gate-state" role="alert">
        <h1>Não foi possível verificar seu perfil</h1>
        <p>Precisamos confirmar seus interesses antes de abrir as trilhas.</p>
        <div className="sp-gate-actions">
          <button type="button" onClick={() => { setStatus('loading'); setRetry(value => value + 1) }}>
            Tentar novamente
          </button>
          <Link to="/perfil">Abrir meu perfil</Link>
        </div>
      </main>
    )
  }

  return children
}
