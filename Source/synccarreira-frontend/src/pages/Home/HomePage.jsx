import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext.jsx'
import { getTrails, canAccessTrail, getQuestionsByTrail, getAnswers } from '../../services/trailService'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { isAdministrator, isPsychologist } from '../../utils/roles'
import './HomePage.css'

// ─── Card de trilha com progresso ─────────────────────────────

function TrailCard({ trail, studentId, onEnter, refreshKey }) {
  const [progress, setProgress] = useState(null)
  const [access, setAccess] = useState('checking')
  const isInformationTrail = trail?.name
      ?.normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toUpperCase() === 'INFORMACAO'

  // refreshKey muda quando o usuário volta para a Home — força recarregar o progresso
  useEffect(() => {
    if (!trail?.id || !studentId) return

    setProgress(null) // reseta para mostrar "Carregando..."
    setAccess('checking')
    let current = true

    async function loadProgress() {
      try {
        const allowed = await canAccessTrail(trail.id)
        if (!current) return
        if (!allowed) {
          setAccess('locked')
          setProgress({ pct: 0, answered: 0, total: 0 })
          return
        }
        setAccess('allowed')

        const questions = await getQuestionsByTrail(trail.id)
        if (!current) return
        const total = questions.length
        if (total === 0) { setProgress({ pct: 0, answered: 0, total: 0 }); return }

        let answers = []
        try {
          answers = await getAnswers(studentId, trail.id)
        } catch {
          // aluno sem respostas ainda — progresso 0
        }

        const answeredSet = new Set()
        for (const ans of answers) {
          const optId = ans.questionOptionDTO?.id
          if (!optId) continue
          const q = questions.find(q => q.options?.some(o => o.id === optId))
          if (q) answeredSet.add(q.id)
        }

        const answered = answeredSet.size
        setProgress({ pct: Math.round((answered / total) * 100), answered, total })

      } catch {
        if (!current) return
        setAccess('error')
        setProgress(null)
      }
    }

    loadProgress()
    return () => { current = false }
  }, [trail?.id, studentId, refreshKey]) // refreshKey garante reload ao voltar

  const isLocked = access === 'locked'
  const accessFailed = access === 'error'
  const isComplete = progress?.pct === 100
  const isStarted  = progress?.answered > 0

  let buttonLabel = isInformationTrail ? 'Acessar' : 'Iniciar'
  if (isLocked)       buttonLabel = 'Bloqueada'
  else if (accessFailed) buttonLabel = 'Indisponível'
  else if (isComplete)     buttonLabel = 'Revisar'
  else if (isStarted) buttonLabel = 'Continuar'

  return (
    <div className={`hp-trail-card${isComplete ? ' hp-trail-card--done' : ''}${isLocked || accessFailed ? ' hp-trail-card--locked' : ''}`}>
      <div className="hp-trail-card__icon" aria-hidden="true">
        {isLocked || accessFailed ? (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3" strokeLinecap="round"/></svg>
        ) : isComplete
          ? <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" width="22" height="22"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" strokeLinecap="round"/><polyline points="22 4 12 14.01 9 11.01" strokeLinecap="round" strokeLinejoin="round"/></svg>
          : <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="22" height="22"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2z" strokeLinecap="round"/><path d="M12 8v4l3 3" strokeLinecap="round" strokeLinejoin="round"/></svg>
        }
      </div>

      <div className="hp-trail-card__info">
        <span className="hp-trail-card__name">{trail.name}</span>

        {access === 'checking' || progress === null ? (
          <span className="hp-trail-card__loading">{isInformationTrail ? 'Verificando acesso…' : 'Carregando progresso…'}</span>
        ) : isLocked ? (
          <span className="hp-trail-card__lock-message">Conclua a trilha anterior, incluindo a síntese, para liberar esta etapa.</span>
        ) : accessFailed ? (
          <span className="hp-trail-card__lock-message">Não foi possível verificar o acesso. Atualize a página e tente novamente.</span>
        ) : isInformationTrail ? (
          <span className="hp-trail-card__final-step">Etapa final · síntese da jornada</span>
        ) : (
          <>
            <div className="hp-trail-card__bar">
              <div
                className="hp-trail-card__bar-fill"
                style={{ width: `${progress.pct}%` }}
              />
            </div>
            <span className="hp-trail-card__pct">
              {progress.answered} / {progress.total} perguntas · {progress.pct}%
            </span>
          </>
        )}
      </div>

      <button
        className={`hp-trail-card__btn${isComplete ? ' hp-trail-card__btn--done' : ''}`}
        onClick={() => onEnter(trail.id)}
        disabled={access !== 'allowed'}
        aria-disabled={access !== 'allowed'}
      >
        {buttonLabel}
      </button>
    </div>
  )
}

// ─── Componente principal ─────────────────────────────────────

export default function HomePage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  // Psicóloga não responde trilhas: a Home dela leva para os agendamentos
  const psicologa = isPsychologist(user)
  const administradora = isAdministrator(user)
  const aluno = user?.roles?.some(role => role.authority === 'ROLE_USER') || user?.perfil === 'ROLE_USER'

  const [trails, setTrails]         = useState([])
  const [loadingTrails, setLoadingTrails] = useState(true)
  const [trailsError, setTrailsError] = useState('')
  // refreshKey muda ao ganhar foco — força TrailCards a recarregar progresso
  const [refreshKey, setRefreshKey] = useState(() => Date.now())

  // Recarrega progresso quando o usuário volta para a aba ou janela
  useEffect(() => {
    function handleFocus() { setRefreshKey(Date.now()) }
    window.addEventListener('focus', handleFocus)
    return () => window.removeEventListener('focus', handleFocus)
  }, [])

  // Carrega lista de trilhas uma vez
  useEffect(() => {
    if (psicologa || !aluno) {
      setLoadingTrails(false)
      return
    }
    async function load() {
      try {
        const data = await getTrails()
        setTrails(data)
      } catch {
        setTrailsError('Não foi possível carregar as trilhas.')
      } finally {
        setLoadingTrails(false)
      }
    }
    load()
  }, [psicologa, aluno])

  // Ao entrar numa trilha, atualiza o refreshKey ao voltar via navigate
  function handleEnterTrail(id) {
    navigate(`/trail/${id}`)
  }

  return (
    <div className="hp-root">

      {/* ── Cabeçalho (com link para Agendamentos) ── */}
      <AppHeader />

      {/* ── Conteúdo ── */}
      <main className="hp-main">
        <div className="hp-content">

          {/* Card de boas-vindas */}
          <div className="hp-card">
            <div className="hp-avatar" aria-hidden="true">
              {user?.nome?.charAt(0).toUpperCase() ?? '?'}
            </div>
            <h1 className="hp-title">
              Bem-vindo, <span>{user?.nome ?? 'Usuário'}</span>!
            </h1>
            <span className="hp-role">{psicologa ? 'Psicóloga / Orientadora' : 'Aluno'}</span>
            <p className="hp-sub">
              {psicologa
                ? 'Acompanhe seus alunos e organize as sessões de orientação.'
                : 'Continue sua jornada de autoconhecimento.'}
            </p>
            {psicologa && (
              <button
                className="hp-btn-cadastro"
                onClick={() => navigate('/agendamentos')}
              >
                Ver agendamentos
              </button>
            )}
            {administradora && (
              <button
                className="hp-btn-cadastro"
                onClick={() => navigate('/gestao')}
              >
                Gestão de usuários
              </button>
            )}
          </div>

          {/* Seção de trilhas (só para alunos) */}
          {aluno && (
          <section className="hp-trails">
            <h2 className="hp-trails__title">Suas trilhas</h2>

            {loadingTrails && (
              <div className="hp-trails__loading">
                <div className="hp-trails__spinner" />
                Carregando trilhas…
              </div>
            )}

            {!loadingTrails && trailsError && (
              <div className="hp-trails__error">{trailsError}</div>
            )}

            {!loadingTrails && !trailsError && trails.length === 0 && (
              <div className="hp-trails__empty">
                Nenhuma trilha disponível no momento.
              </div>
            )}

            {!loadingTrails && trails.map(trail => (
              <TrailCard
                key={`${trail.id}-${refreshKey}`}
                trail={trail}
                studentId={user?.id}
                onEnter={handleEnterTrail}
                refreshKey={refreshKey}
              />
            ))}
          </section>
          )}

        </div>
      </main>
    </div>
  )
}
