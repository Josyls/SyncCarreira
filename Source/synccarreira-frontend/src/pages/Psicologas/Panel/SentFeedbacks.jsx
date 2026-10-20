/**
 * @file SentFeedbacks.jsx
 * @description RF-09 — Histórico dos feedbacks enviados pela psicóloga (jornada e sessão).
 *
 * GET /feedbacks/sent. O backend não permite editar: a psicóloga pode apagar
 * (DELETE /feedbacks/{id}) e escrever de novo.
 */

import { useState } from 'react'
import { FEEDBACK_TYPE_LABEL } from '../../../services/feedbackService'

const INITIAL_VISIBLE = 5

function formatDate(instant) {
  return instant ? new Date(instant).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }) : ''
}

function contextOf(feedback) {
  if (feedback.type === 'JORNADA') return feedback.journeyNumber ? `Jornada nº ${feedback.journeyNumber}` : 'Jornada'
  return feedback.appointmentTitle || 'Sessão'
}

/**
 * @param {Object}   props
 * @param {Array}    props.feedbacks - FeedbackDTO[] (mais recentes primeiro)
 * @param {string}   [props.error]
 * @param {Function} props.onDelete  - async (feedback) => void
 */
export default function SentFeedbacks({ feedbacks, error, onDelete }) {
  const [showAll, setShowAll] = useState(false)
  const [confirmId, setConfirmId] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const [deleteError, setDeleteError] = useState('')

  const visible = showAll ? feedbacks : feedbacks.slice(0, INITIAL_VISIBLE)

  async function handleDelete(feedback) {
    setBusyId(feedback.id)
    setDeleteError('')
    try {
      await onDelete(feedback)
      setConfirmId(null)
    } catch (err) {
      setDeleteError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <section className="pd-panel pd-feedbacks" aria-labelledby="pd-feedbacks-title">
      <div className="pd-section-heading">
        <div>
          <h2 id="pd-feedbacks-title">Feedbacks enviados</h2>
          <p>Feedbacks de fim de jornada e de sessões. O aluno é avisado por e-mail a cada envio.</p>
        </div>
        <span className="pd-result-count">{feedbacks.length} enviado(s)</span>
      </div>

      {error && <p className="pd-reports__msg pd-reports__msg--error" role="alert">{error}</p>}
      {!error && feedbacks.length === 0 && (
        <p className="pd-empty-inline">Você ainda não enviou feedbacks. Use o botão “Enviar feedback” na tabela de alunos ou nas sessões realizadas.</p>
      )}

      {visible.length > 0 && (
        <ul className="pd-feedback-list">
          {visible.map(feedback => (
            <li key={feedback.id} className="pd-feedback">
              <div className="pd-feedback__top">
                <span className={`pd-feedback__type pd-feedback__type--${feedback.type?.toLowerCase()}`}>
                  {FEEDBACK_TYPE_LABEL[feedback.type] ?? feedback.type}
                </span>
                <strong>{feedback.studentName}</strong>
                <span className="pd-feedback__meta">{contextOf(feedback)} · {formatDate(feedback.createdAt)}</span>
                <span className={feedback.read ? 'pd-feedback__read' : 'pd-feedback__unread'}>
                  {feedback.read ? 'Lido' : 'Não lido'}
                </span>
              </div>
              <p className="pd-feedback__text">{feedback.content}</p>
              <div className="pd-feedback__actions">
                {confirmId === feedback.id ? (
                  <>
                    <span>Apagar este feedback?</span>
                    <button type="button" className="ag-btn ag-btn--danger ag-btn--sm" disabled={busyId === feedback.id} onClick={() => handleDelete(feedback)}>
                      {busyId === feedback.id ? 'Apagando…' : 'Sim, apagar'}
                    </button>
                    <button type="button" className="ag-btn ag-btn--ghost ag-btn--sm" onClick={() => setConfirmId(null)}>Não</button>
                  </>
                ) : (
                  <button type="button" className="ag-link" onClick={() => setConfirmId(feedback.id)}>Apagar</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {deleteError && <p className="pd-reports__msg pd-reports__msg--error" role="alert">{deleteError}</p>}

      {feedbacks.length > INITIAL_VISIBLE && (
        <button type="button" className="pd-show-more" onClick={() => setShowAll(value => !value)}>
          {showAll ? 'Mostrar menos' : `Ver todos (${feedbacks.length})`}
        </button>
      )}
    </section>
  )
}