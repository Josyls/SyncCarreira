import { useEffect, useState } from 'react'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { getAnswers, getMySyntheses, getQuestionsByTrail, getTrails } from '../../services/trailService.js'
import { getMyCurrentJourneyStatus, getMyJourneyHistory } from '../../services/journeyHistoryService.js'
import './JourneyPanoramaPage.css'

const TRAIL_NAMES = {
  AUTOCONHECIMENTO: 'Autoconhecimento',
  INFLUENCIAS: 'Influências',
  INFORMACAO: 'Informação',
  PLANO_DE_FUTURO: 'Projeto de futuro',
}

const AREA_NAMES = {
  HUMANAS: 'Ciências Humanas',
  BIOLOGICAS: 'Ciências Biológicas',
  EXATAS: 'Ciências Exatas',
  ARTES: 'Artes',
}

function dateLabel(value) {
  if (!value) return 'Data não informada'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 'Data não informada' : date.toLocaleDateString('pt-BR', { dateStyle: 'long' })
}

function trailLabel(trail) {
  return TRAIL_NAMES[trail.name] ?? TRAIL_NAMES[trail.trailName] ?? trail.name ?? trail.trailName ?? 'Etapa da jornada'
}

function getAnsweredQuestions(questions, answers) {
  const answerOptionIds = new Set((answers ?? []).map(answer => String(answer.questionOptionDTO?.id)))
  return (questions ?? []).flatMap((question, index) => {
    const selectedOptions = (question.options ?? []).filter(option => answerOptionIds.has(String(option.id)))
    if (!selectedOptions.length) return []
    return [{
      id: question.id ?? index,
      prompt: question.content,
      responses: selectedOptions.map(option => option.optionText),
    }]
  })
}

function isTrailComplete(trail) {
  if (!trail.synthesis) return false
  if (trail.name === 'INFORMACAO') return true
  return trail.totalQuestions > 0 && trail.answers.length >= trail.totalQuestions
}

function TrailMemory({ trail, index, isLast }) {
  const answered = trail.answers.length > 0
  const hasActivity = answered || Boolean(trail.synthesis)
  const complete = isTrailComplete(trail)
  const state = complete ? 'complete' : hasActivity ? 'progress' : 'waiting'
  const status = complete ? 'Concluída' : answered ? 'Em andamento' : 'Ainda não iniciada'

  return (
    <article className={`jp-step jp-step--${state}${isLast ? ' jp-step--last' : ''}`}>
      <div className="jp-step__marker" aria-hidden="true">
        <span>{complete ? '✓' : String(index + 1).padStart(2, '0')}</span>
      </div>
      <div className="jp-step__content">
        <header className="jp-step__heading">
          <div>
            <span className="jp-step__eyebrow">ETAPA {String(index + 1).padStart(2, '0')}</span>
            <h2>{trailLabel(trail)}</h2>
          </div>
          <span className={`jp-status jp-status--${state}`}>{status}</span>
        </header>

        <p className="jp-step__caption">
          {trail.answers.length} de {trail.totalQuestions} perguntas com resposta
        </p>

        {answered && (
          <div className="jp-memories">
            <h3>O que você respondeu</h3>
            {trail.answers.map((answer, answerIndex) => (
              <div className="jp-memory" key={answer.id}>
                <span>Pergunta {answerIndex + 1}</span>
                <p>{answer.prompt}</p>
                <strong>{answer.responses.join(' · ')}</strong>
              </div>
            ))}
          </div>
        )}

        {trail.synthesis && (
          <div className="jp-synthesis">
            <span>{trail.name === 'INFORMACAO' ? 'SÍNTESE DA JORNADA' : 'SUA SÍNTESE'}</span>
            <p>{trail.synthesis.content}</p>
            {trail.synthesis.createdAt && <time>{dateLabel(trail.synthesis.createdAt)}</time>}
          </div>
        )}

        {!hasActivity && (
          <p className="jp-step__empty">Esta etapa ainda não faz parte do caminho percorrido nesta jornada.</p>
        )}
      </div>
    </article>
  )
}

function PastJourney({ journey }) {
  const areas = journey.highestScoreAreas ?? []
  const scores = [
    { label: 'Ciências Humanas', value: journey.humanitiesScore },
    { label: 'Ciências Exatas', value: journey.exactSciencesScore },
    { label: 'Ciências Biológicas', value: journey.biologicalSciencesScore },
    { label: 'Artes', value: journey.artsScore },
  ]
    .filter(item => item.value !== null && item.value !== undefined)

  return (
    <article className="jp-past-card">
      <div className="jp-past-card__number">
        <span>JORNADA</span>
        <strong>{String(journey.journeyNumber).padStart(2, '0')}</strong>
      </div>
      <div className="jp-past-card__body">
        <h3>Um caminho que você já percorreu</h3>
        <p className="jp-past-card__date">Concluída em {dateLabel(journey.concludedAt)}</p>
        {(journey.schoolClassName || journey.institutionName) && (
          <p className="jp-past-card__context">{[journey.schoolClassName, journey.institutionName].filter(Boolean).join(' · ')}</p>
        )}
        {areas.length > 0 && (
          <div className="jp-area-chips" aria-label="Áreas que mais apareceram no resultado">
            {areas.map(area => <span key={area}>{AREA_NAMES[area] ?? area}</span>)}
          </div>
        )}
        {scores.length > 0 && (
          <div className="jp-score-list">
            {scores.map(score => (
              <div key={score.label}>
                <span>{score.label}</span><strong>{Math.round(Number(score.value) * 10) / 10}%</strong>
              </div>
            ))}
          </div>
        )}
      </div>
    </article>
  )
}

export default function JourneyPanoramaPage() {
  const { user } = useAuth()
  const [currentJourney, setCurrentJourney] = useState(null)
  const [pastJourneys, setPastJourneys] = useState([])
  const [historyError, setHistoryError] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    async function load() {
      setLoading(true)
      setError('')
      setHistoryError('')
      try {
        const [trailsData, syntheses, status] = await Promise.all([
          getTrails(),
          getMySyntheses(),
          getMyCurrentJourneyStatus(),
        ])
        const orderedTrails = [...(trailsData ?? [])].sort((a, b) => (a.sequentialOrder ?? 0) - (b.sequentialOrder ?? 0))
        const detailedTrails = await Promise.all(orderedTrails.map(async trail => {
          const [questions, answers] = await Promise.all([
            getQuestionsByTrail(trail.id),
            getAnswers(user?.id, trail.id),
          ])
          const synthesis = syntheses.find(item => Number(item.trailId ?? item.trail?.id) === Number(trail.id))
          return {
            ...trail,
            totalQuestions: (questions ?? []).length,
            answers: getAnsweredQuestions(questions, answers),
            synthesis: synthesis ?? null,
          }
        }))

        if (active) {
          setCurrentJourney({ ...status, trails: detailedTrails })
          try {
            setPastJourneys(await getMyJourneyHistory())
          } catch {
            setHistoryError('Não foi possível carregar o resumo das jornadas anteriores.')
          }
        }
      } catch (loadError) {
        if (active) setError(loadError.response?.data?.message || 'Não foi possível montar o panorama da sua jornada agora.')
      } finally {
        if (active) setLoading(false)
      }
    }

    void load()
    return () => { active = false }
  }, [user?.id])

  const completedStages = currentJourney?.trails.filter(isTrailComplete).length ?? 0
  const totalStages = currentJourney?.trails.length ?? 0

  return (
    <div className="jp-page">
      <AppHeader />
      <main className="jp-main">
        <header className="jp-hero">
          <div className="jp-hero__mark" aria-hidden="true">✦</div>
          <span className="jp-eyebrow">UM OLHAR SOBRE O SEU CAMINHO</span>
          <h1>Sua jornada, do seu jeito</h1>
          <p>Um registro das ideias, escolhas e descobertas que você reuniu ao longo do percurso.</p>
          {currentJourney && <span className="jp-hero__meta">Jornada {currentJourney.currentJourneyNumber} · {completedStages} de {totalStages} etapas concluídas</span>}
        </header>

        {loading && <div className="jp-state" role="status">Reunindo os registros da sua jornada…</div>}
        {!loading && error && <div className="jp-state jp-state--error" role="alert">{error}</div>}

        {!loading && !error && currentJourney && (
          <>
            <section className="jp-current" aria-labelledby="jp-current-title">
              <div className="jp-section-heading">
                <span className="jp-section-heading__number">01</span>
                <div><h2 id="jp-current-title">O caminho que você está percorrendo</h2><p>Suas respostas e reflexões reunidas em cada etapa.</p></div>
              </div>
              <div className="jp-roadmap">
                {currentJourney.trails.map((trail, index) => (
                  <TrailMemory key={trail.id} trail={trail} index={index} isLast={index === currentJourney.trails.length - 1} />
                ))}
              </div>
            </section>

            <section className="jp-past" aria-labelledby="jp-past-title">
              <div className="jp-section-heading">
                <span className="jp-section-heading__number jp-section-heading__number--warm">02</span>
                <div><h2 id="jp-past-title">Outros caminhos que você percorreu</h2><p>Uma lembrança de cada jornada concluída.</p></div>
              </div>
              {historyError && <p className="jp-history-error" role="status">{historyError}</p>}
              {!historyError && pastJourneys.length === 0 && (
                <div className="jp-past-empty">Quando você concluir outras jornadas, elas ficarão guardadas aqui.</div>
              )}
              {pastJourneys.map(journey => <PastJourney key={journey.id} journey={journey} />)}
            </section>
          </>
        )}
      </main>
    </div>
  )
}
