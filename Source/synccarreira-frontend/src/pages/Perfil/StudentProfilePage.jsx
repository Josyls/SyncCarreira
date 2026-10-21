/**
 * @file StudentProfilePage.jsx
 * @description Perfil do aluno
 */

import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { getMyStudentProfile } from '../../services/studentProfileService.js'
import { getMyCurrentJourneyStatus } from '../../services/journeyHistoryService.js'
import InterestsForm from './components/InterestsForm.jsx'
import DoubtSignal from './components/DoubtSignal.jsx'
import JourneyHistory from './components/JourneyHistory.jsx'
import AlunoAgendamentos from '../Agendamentos/AlunoAgendamentos.jsx'
import '../Agendamentos/Agendamentos.css'
import './StudentProfilePage.css'

const TABS = [
  { id: 'interesses', label: 'Interesses' },
  { id: 'jornadas', label: 'Minhas jornadas' },
  { id: 'agendamentos', label: 'Agendamentos e feedbacks' },
]

/** Primeiro acesso: só o formulário de interesses, como antes. */
function OnboardingCard({ profile, onSaved }) {
  return (
    <section className="sp-card">
      <span className="sp-eyebrow">PERSONALIZAÇÃO DA JORNADA</span>
      <h1>Vamos personalizar sua jornada</h1>
      <p className="sp-intro">
        Suas respostas ajudam a contextualizar o resultado da jornada. Você pode voltar aqui e alterá-las sempre que quiser.
      </p>
      <InterestsForm profile={profile} onSaved={onSaved} />
    </section>
  )
}

export default function StudentProfilePage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const [profile, setProfile] = useState(null)
  const [journeyStatus, setJourneyStatus] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const requestedTab = searchParams.get('aba')
  const activeTab = TABS.some(tab => tab.id === requestedTab) ? requestedTab : 'interesses'

  useEffect(() => {
    let active = true
    getMyStudentProfile()
      .then(async data => {
        if (!active) return
        setProfile(data)
        if (data.profileCompleted) {
          const status = await getMyCurrentJourneyStatus().catch(() => null)
          if (active) setJourneyStatus(status)
        }
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar seu perfil. Atualize a página ou tente novamente.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [])

  function openTab(tabId) {
    setSearchParams(tabId === 'interesses' ? {} : { aba: tabId })
  }

  function handleTabKeyDown(event, index) {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return
    const step = event.key === 'ArrowRight' ? 1 : -1
    const next = TABS[(index + step + TABS.length) % TABS.length]
    openTab(next.id)
    document.getElementById(`sp-tab-${next.id}`)?.focus()
  }

  const firstName = (user?.nome ?? profile?.name ?? '').split(' ')[0]

  return (
    <div className="sp-page">
      <AppHeader />
      <main className="sp-main">
        <Link className="sp-back" to="/home">← Voltar para o início</Link>

        {loading && <section className="sp-card"><div className="sp-state" role="status">Carregando seu perfil…</div></section>}
        {!loading && error && <section className="sp-card"><p className="sp-message sp-message--error" role="alert">{error}</p></section>}

        {!loading && !error && profile && !profile.profileCompleted && (
          <OnboardingCard profile={profile} onSaved={setProfile} />
        )}

        {!loading && !error && profile?.profileCompleted && (
          <>
            <header className="sp-profile-head">
              <span className="sp-eyebrow">MEU PERFIL</span>
              <h1>{firstName ? `Olá, ${firstName}` : 'Meu perfil'}</h1>
              <p className="sp-intro">Seus interesses, suas jornadas e as conversas com a sua orientadora em um só lugar.</p>
            </header>

            {/* key: ao iniciar uma nova jornada, o card recarrega a situação da dúvida */}
            <DoubtSignal
              key={journeyStatus?.currentJourneyNumber ?? 'sem-jornada'}
              journeyConcluded={Boolean(journeyStatus?.currentJourneyConcluded)}
              onOpenSchedule={() => openTab('agendamentos')}
            />

            <div className="sp-tabs" role="tablist" aria-label="Seções do perfil">
              {TABS.map((tab, index) => (
                <button
                  key={tab.id}
                  id={`sp-tab-${tab.id}`}
                  type="button"
                  role="tab"
                  aria-selected={activeTab === tab.id}
                  aria-controls={`sp-panel-${tab.id}`}
                  tabIndex={activeTab === tab.id ? 0 : -1}
                  className={`sp-tab${activeTab === tab.id ? ' is-active' : ''}`}
                  onClick={() => openTab(tab.id)}
                  onKeyDown={event => handleTabKeyDown(event, index)}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <section
              className="sp-card sp-tab-panel"
              id={`sp-panel-${activeTab}`}
              role="tabpanel"
              aria-labelledby={`sp-tab-${activeTab}`}
            >
              {activeTab === 'interesses' && (
                <>
                  <h2 className="sp-section-title">Seus interesses</h2>
                  <p className="sp-intro">Suas respostas ajudam a contextualizar o resultado da jornada. Você pode alterá-las sempre que quiser.</p>
                  <InterestsForm profile={profile} onSaved={setProfile} />
                </>
              )}
              {activeTab === 'jornadas' && (
                <JourneyHistory status={journeyStatus} onStatusChange={setJourneyStatus} />
              )}
              {activeTab === 'agendamentos' && (
                <div className="sp-embedded-schedule">
                  <AlunoAgendamentos embedded />
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  )
}