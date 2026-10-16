import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { getCurrentJourneyResult } from '../../services/journeyResultService.js'
import './JourneyResultPage.css'

const AREA_LABELS = {
  HUMANAS: 'Ciências Humanas',
  BIOLOGICAS: 'Ciências Biológicas',
  EXATAS: 'Ciências Exatas',
  ARTES: 'Artes',
}

function percentage(value) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(value ?? 0)
}

function formattedDate(value) {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('pt-BR', { dateStyle: 'long' })
}

function resultError(error) {
  if (error.response?.status === 403) {
    return 'Este resultado só pode ser consultado pelo aluno responsável pela jornada.'
  }
  if (error.response?.status === 422) {
    return 'O leque de sugestões estará disponível depois que você concluir todas as trilhas e salvar as sínteses.'
  }
  return error.response?.data?.message || 'Não foi possível carregar seu resultado agora. Tente novamente.'
}

export default function JourneyResultPage() {
  const navigate = useNavigate()
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let current = true
    getCurrentJourneyResult()
      .then(data => { if (current) setResult(data) })
      .catch(requestError => {
        console.error('Erro ao carregar o resultado da jornada:', requestError)
        if (current) setError(resultError(requestError))
      })
      .finally(() => { if (current) setLoading(false) })

    return () => { current = false }
  }, [])

  const scores = result?.scores ?? []
  const suggestions = result?.suggestions ?? []

  return (
    <div className="journey-result-page">
      <AppHeader />
      <main className="journey-result-main">
        <button className="journey-result-back" type="button" onClick={() => navigate('/home')}>
          <span aria-hidden="true">←</span> Voltar para minhas trilhas
        </button>

        {loading ? (
          <div className="journey-result-state" role="status">Preparando seu resultado…</div>
        ) : error ? (
          <section className="journey-result-state journey-result-state--error" role="alert">
            <span className="journey-result-state__icon" aria-hidden="true">i</span>
            <h1>Seu resultado ainda não está disponível</h1>
            <p>{error}</p>
            <button type="button" onClick={() => navigate('/home')}>Voltar para as trilhas</button>
          </section>
        ) : (
          <>
            <header className="journey-result-hero">
              <span className="journey-result-hero__eyebrow">Jornada {result.journeyNumber}</span>
              <h1>Seu leque de possibilidades</h1>
              <p>Use este resultado para explorar caminhos que podem combinar com você neste momento.</p>
              {formattedDate(result.concludedAt) && (
                <span className="journey-result-hero__date">Concluída em {formattedDate(result.concludedAt)}</span>
              )}
            </header>

            <aside className="journey-result-disclaimer" aria-label="Aviso sobre o resultado">
              <span className="journey-result-disclaimer__mark" aria-hidden="true">i</span>
              <div>
                <strong>Um ponto de partida, não um diagnóstico</strong>
                <p>{result.disclaimer}</p>
              </div>
            </aside>

            <section className="journey-result-section" aria-labelledby="journey-result-areas-title">
              <div className="journey-result-section__heading">
                <span>01</span>
                <div>
                  <h2 id="journey-result-areas-title">Áreas que apareceram nas suas respostas</h2>
                  <p>Os percentuais são comparados ao máximo possível de cada área.</p>
                </div>
              </div>
              <div className="journey-result-scores">
                {scores.map(score => (
                  <article className={`journey-result-score${score.highlighted ? ' journey-result-score--highlighted' : ''}`} key={score.area}>
                    <div className="journey-result-score__topline">
                      <div>
                        <h3>{score.areaName || AREA_LABELS[score.area] || score.area}</h3>
                        {score.highlighted && <span className="journey-result-score__badge">Em destaque</span>}
                      </div>
                      <strong>{percentage(score.percentage)}%</strong>
                    </div>
                    <div className="journey-result-score__track" aria-hidden="true">
                      <span style={{ width: `${Math.min(100, Math.max(0, score.percentage ?? 0))}%` }} />
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <section className="journey-result-section" aria-labelledby="journey-result-careers-title">
              <div className="journey-result-section__heading">
                <span>02</span>
                <div>
                  <h2 id="journey-result-careers-title">Áreas e carreiras para explorar</h2>
                  <p>São possibilidades para pesquisar, conversar e conhecer melhor — não uma escolha pronta.</p>
                </div>
              </div>

              {suggestions.length > 0 ? (
                <div className="journey-result-suggestions">
                  {suggestions.map(suggestion => (
                    <article className="journey-result-suggestion" key={suggestion.id}>
                      <span className="journey-result-suggestion__area">
                        {AREA_LABELS[suggestion.area] || suggestion.area}
                      </span>
                      <h3>{suggestion.name}</h3>
                      <p>{suggestion.description}</p>
                      {suggestion.careers?.length > 0 && (
                        <div className="journey-result-careers">
                          <h4>Algumas carreiras</h4>
                          <ul>
                            {suggestion.careers.map(career => <li key={career}>{career}</li>)}
                          </ul>
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              ) : (
                <p className="journey-result-empty">
                  Suas respostas ainda não destacam uma área específica. Isso não limita suas possibilidades: explore diferentes caminhos e converse com a psicóloga da sua escola.
                </p>
              )}
            </section>

            <footer className="journey-result-footer">
              <p>Seus interesses podem mudar. Conhecer cursos e conversar com profissionais pode ajudar nos próximos passos.</p>
              <button type="button" onClick={() => navigate('/home')}>Continuar explorando</button>
            </footer>
          </>
        )}
      </main>
    </div>
  )
}
