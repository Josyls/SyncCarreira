/**
 * @file JourneyHistory.jsx
 * @description RF-15 — Histórico de jornadas anteriores.
 *
 * Mostra a evolução do aluno por área ao longo das jornadas concluídas
 * (percentuais do resultado de cada jornada) e um cartão por jornada, com
 * áreas em destaque, interesses declarados na época, resultado e PDF.
 * Quando a jornada atual está concluída, permite iniciar uma nova.
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getMyJourneyHistory, getMyJourneyResult, downloadMyJourneyPdf, startNewJourney,
} from '../../../services/journeyHistoryService.js'

const AREAS = [
  { key: 'HUMANAS', label: 'Humanas', affinityField: 'humanities' },
  { key: 'EXATAS', label: 'Exatas', affinityField: 'exactSciences' },
  { key: 'BIOLOGICAS', label: 'Biológicas', affinityField: 'biologicalSciences' },
  { key: 'ARTES', label: 'Artes', affinityField: 'arts' },
]
const AREA_LABEL = Object.fromEntries(AREAS.map(area => [area.key, area.label]))

const formatNumber = value => new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(value ?? 0)
const formatDate = instant => (instant ? new Date(instant).toLocaleDateString('pt-BR') : '—')

/** Percentual da área no resultado da jornada (ou null se o resultado não carregou). */
function percentageOf(result, areaKey) {
  const score = result?.scores?.find(item => item.area === areaKey)
  return score ? score.percentage : null
}

/** Texto e classe da variação entre a primeira e a última jornada. */
function variationOf(first, last) {
  if (first === null || last === null) return { text: '—', tone: 'flat' }
  const delta = Math.round(last - first)
  if (delta > 0) return { text: `▲ ${delta} p.p.`, tone: 'up' }
  if (delta < 0) return { text: `▼ ${Math.abs(delta)} p.p.`, tone: 'down' }
  return { text: 'sem mudança', tone: 'flat' }
}

/** Tabela "Evolução por área": uma coluna por jornada, com barra e percentual. */
function EvolutionTable({ journeys, results }) {
  const ordered = [...journeys].sort((first, second) => first.journeyNumber - second.journeyNumber)
  const showVariation = ordered.length > 1

  return (
    <div className="sp-evolution">
      <div className="sp-evolution__wrap">
        <table className="sp-evolution__table">
          <caption className="sr-only">Percentual de afinidade por área em cada jornada concluída</caption>
          <thead>
            <tr>
              <th scope="col">Área</th>
              {ordered.map(journey => <th scope="col" key={journey.id}>Jornada {journey.journeyNumber}</th>)}
              {showVariation && <th scope="col">Evolução</th>}
            </tr>
          </thead>
          <tbody>
            {AREAS.map(area => {
              const values = ordered.map(journey => percentageOf(results[journey.id], area.key))
              const variation = variationOf(values[0], values.at(-1))
              return (
                <tr key={area.key}>
                  <th scope="row">{area.label}</th>
                  {ordered.map((journey, index) => (
                    <td key={journey.id}>
                      {values[index] === null ? <span className="sp-evolution__na">—</span> : (
                        <div className="sp-evolution__cell">
                          <span className="sp-evolution__bar"><span style={{ width: `${Math.min(100, values[index])}%` }} /></span>
                          <span className={journey.highestScoreAreas?.includes(area.key) ? 'sp-evolution__value is-top' : 'sp-evolution__value'}>
                            {formatNumber(values[index])}%
                          </span>
                        </div>
                      )}
                    </td>
                  ))}
                  {showVariation && <td className={`sp-evolution__var sp-evolution__var--${variation.tone}`}>{variation.text}</td>}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <p className="sp-evolution__note">Valores em destaque são as áreas mais fortes de cada jornada. O resultado não é um diagnóstico: é um retrato daquele momento.</p>
    </div>
  )
}

/** Cartão de uma jornada concluída. */
function JourneyCard({ journey, result, resultError }) {
  const [open, setOpen] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [message, setMessage] = useState('')

  async function handlePdf() {
    setDownloading(true)
    setMessage('')
    try {
      await downloadMyJourneyPdf(journey.id, journey.journeyNumber)
    } catch (error) {
      setMessage(error.message)
    } finally {
      setDownloading(false)
    }
  }

  const where = [journey.schoolClassName, journey.schoolYear, journey.institutionName].filter(Boolean).join(' · ')

  return (
    <article className="sp-journey">
      <header className="sp-journey__head">
        <span className="sp-journey__number" aria-hidden="true">{journey.journeyNumber}</span>
        <div>
          <h3>Jornada {journey.journeyNumber}</h3>
          <p>{formatDate(journey.startedAt)} a {formatDate(journey.concludedAt)}{where ? ` · ${where}` : ''}</p>
        </div>
        {journey.inDoubt && <span className="sp-journey__doubt">Sinalizou dúvida</span>}
      </header>

      <div className="sp-journey__facts">
        <div>
          <span className="sp-journey__label">Áreas em destaque</span>
          <div className="sp-chips">
            {(journey.highestScoreAreas ?? []).map(area => <span key={area} className="sp-chip">{AREA_LABEL[area] ?? area}</span>)}
            {!journey.highestScoreAreas?.length && <span className="sp-journey__muted">—</span>}
          </div>
        </div>
        <div>
          <span className="sp-journey__label">Seus interesses na época (0 a 5)</span>
          <div className="sp-chips">
            {AREAS.map(area => (
              <span key={area.key} className="sp-chip sp-chip--soft">{area.label} {journey.affinities?.[area.affinityField] ?? '—'}</span>
            ))}
          </div>
        </div>
      </div>

      <div className="sp-journey__actions">
        <button type="button" className="sp-link-btn" aria-expanded={open} onClick={() => setOpen(value => !value)}>
          {open ? 'Ocultar resultado' : 'Ver resultado'}
        </button>
        <button type="button" className="sp-outline-btn" onClick={handlePdf} disabled={downloading}>
          {downloading ? 'Gerando PDF…' : 'Baixar PDF'}
        </button>
      </div>
      {message && <p className="sp-message sp-message--error" role="alert">{message}</p>}

      {open && (
        <div className="sp-journey__result">
          {resultError && <p className="sp-journey__muted">{resultError}</p>}
          {!resultError && !result && <p className="sp-journey__muted">Carregando resultado…</p>}
          {result && (
            <>
              <ul className="sp-suggestions">
                {(result.suggestions ?? []).map(item => (
                  <li key={item.id}>
                    <strong>{item.name}</strong> <span className="sp-chip sp-chip--soft">{AREA_LABEL[item.area] ?? item.area}</span>
                    {item.description && <p>{item.description}</p>}
                    {item.careers?.length > 0 && <p className="sp-journey__muted">Carreiras: {item.careers.join(', ')}</p>}
                  </li>
                ))}
              </ul>
              {!result.suggestions?.length && <p className="sp-journey__muted">Sem sugestões registradas para esta jornada.</p>}
              {result.disclaimer && <p className="sp-evolution__note">{result.disclaimer}</p>}
            </>
          )}
        </div>
      )}
    </article>
  )
}

/** Situação da jornada atual + botão para começar uma nova (quando concluída). */
function CurrentJourneyBar({ status, onStarted }) {
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  if (!status) return null

  async function handleStart() {
    setBusy(true)
    setError('')
    try {
      onStarted(await startNewJourney())
      setConfirming(false)
    } catch (startError) {
      setError(startError.message)
    } finally {
      setBusy(false)
    }
  }

  if (!status.currentJourneyConcluded) {
    return (
      <div className="sp-current">
        <div>
          <strong>Jornada {status.currentJourneyNumber} em andamento</strong>
          <span>Iniciada em {formatDate(status.currentJourneyStartedAt)}. Quando você concluir, ela entra neste histórico.</span>
        </div>
        <Link className="sp-outline-btn" to="/home">Continuar trilhas</Link>
      </div>
    )
  }

  return (
    <div className="sp-current sp-current--done">
      <div>
        <strong>Jornada {status.currentJourneyNumber} concluída</strong>
        <span>Você pode refazer a jornada em outro momento para acompanhar como seus interesses mudaram.</span>
        {confirming && (
          <div className="sp-current__confirm">
            <span>Suas respostas e sínteses atuais serão apagadas para começar do zero. A jornada concluída continua guardada aqui. Continuar?</span>
            <div>
              <button type="button" className="sp-submit" onClick={handleStart} disabled={busy}>{busy ? 'Iniciando…' : 'Sim, iniciar nova jornada'}</button>
              <button type="button" className="sp-outline-btn" onClick={() => setConfirming(false)} disabled={busy}>Cancelar</button>
            </div>
          </div>
        )}
        {error && <p className="sp-message sp-message--error" role="alert">{error}</p>}
      </div>
      {!confirming && <button type="button" className="sp-outline-btn" onClick={() => setConfirming(true)}>Iniciar nova jornada</button>}
    </div>
  )
}

/**
 * @param {Object}   props
 * @param {Object}   props.status         - JourneyStatusDTO da jornada atual
 * @param {Function} props.onStatusChange - (JourneyStatusDTO) => void
 */
export default function JourneyHistory({ status, onStatusChange }) {
  const [journeys, setJourneys] = useState([])
  const [results, setResults] = useState({})
  const [resultErrors, setResultErrors] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const list = await getMyJourneyHistory()
      setJourneys(list)
      // Percentuais por área vêm do resultado de cada jornada
      const entries = await Promise.all(list.map(journey => getMyJourneyResult(journey.id)
        .then(result => ({ id: journey.id, result }))
        .catch(resultError => ({ id: journey.id, error: resultError.message }))))
      setResults(Object.fromEntries(entries.filter(item => item.result).map(item => [item.id, item.result])))
      setResultErrors(Object.fromEntries(entries.filter(item => item.error).map(item => [item.id, item.error])))
    } catch {
      setError('Não foi possível carregar o histórico das suas jornadas. Tente novamente.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  const newestFirst = useMemo(
    () => [...journeys].sort((first, second) => second.journeyNumber - first.journeyNumber),
    [journeys]
  )

  return (
    <div className="sp-history">
      <CurrentJourneyBar status={status} onStarted={newStatus => { onStatusChange(newStatus); void load() }} />

      {loading && <div className="sp-state" role="status">Carregando suas jornadas…</div>}
      {!loading && error && (
        <div className="sp-message sp-message--error" role="alert">
          {error} <button type="button" className="sp-link-btn" onClick={load}>Tentar de novo</button>
        </div>
      )}
      {!loading && !error && journeys.length === 0 && (
        <div className="sp-empty">Você ainda não concluiu nenhuma jornada. Quando concluir, ela aparece aqui e você poderá acompanhar sua evolução.</div>
      )}
      {!loading && !error && journeys.length > 0 && (
        <>
          <section aria-labelledby="sp-evolution-title">
            <h2 id="sp-evolution-title" className="sp-section-title">Sua evolução por área</h2>
            <EvolutionTable journeys={journeys} results={results} />
          </section>
          <section aria-labelledby="sp-journeys-title">
            <h2 id="sp-journeys-title" className="sp-section-title">Jornadas concluídas</h2>
            <div className="sp-journeys">
              {newestFirst.map(journey => (
                <JourneyCard key={journey.id} journey={journey} result={results[journey.id]} resultError={resultErrors[journey.id]} />
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}