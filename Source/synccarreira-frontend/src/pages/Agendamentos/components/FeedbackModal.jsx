/**
 * @file FeedbackModal.jsx
 * @description RF-09 — Feedback individual da psicóloga para cada aluno após uma sessão.
 *
 * Usa POST /feedbacks/session (um feedback por aluno em cada sessão). O backend não
 * permite editar: para corrigir, a psicóloga apaga e escreve de novo. Em sessões em
 * grupo, cada participante recebe o seu próprio feedback.
 */

import { useState } from 'react'
import Modal from './Modal.jsx'
import { formatSessionDate } from '../../../services/appointmentService'
import { FEEDBACK_MAX_LENGTH } from '../../../services/feedbackService'

function formatSentAt(instant) {
  if (!instant) return ''
  return new Date(instant).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}

/** Feedback já enviado para um participante. */
function SentFeedback({ feedback, busy, onDelete }) {
  return (
    <div className="ag-fb-sent">
      <p className="ag-fb-sent__text">{feedback.content}</p>
      <div className="ag-fb-sent__meta">
        <span>Enviado em {formatSentAt(feedback.createdAt)}</span>
        <span className={feedback.read ? 'ag-fb-read' : 'ag-fb-unread'}>
          {feedback.read ? 'Lido pelo aluno' : 'Ainda não lido'}
        </span>
        <button type="button" className="ag-link" disabled={busy} onClick={onDelete}>
          Apagar para reescrever
        </button>
      </div>
    </div>
  )
}

/** Campo para escrever o feedback de um participante. */
function FeedbackEditor({ student, busy, onSend }) {
  const [text, setText] = useState('')
  const fieldId = `ag-feedback-${student.id}`

  return (
    <div className="ag-fb-editor">
      <label className="sr-only" htmlFor={fieldId}>Feedback para {student.name}</label>
      <textarea
        id={fieldId}
        className="ag-input ag-textarea"
        rows={4}
        maxLength={FEEDBACK_MAX_LENGTH}
        placeholder="Pontos conversados, combinados e próximos passos…"
        value={text}
        onChange={event => setText(event.target.value)}
      />
      <div className="ag-fb-editor__foot">
        <span className="ag-counter">{text.length}/{FEEDBACK_MAX_LENGTH}</span>
        <button
          type="button"
          className="ag-btn ag-btn--primary ag-btn--sm"
          disabled={busy || !text.trim()}
          onClick={() => onSend(text.trim())}
        >
          {busy ? 'Enviando…' : 'Enviar feedback'}
        </button>
      </div>
    </div>
  )
}

/**
 * @param {Object}   props
 * @param {Object}   props.appointment
 * @param {Object}   props.sentByStudent - { [studentId]: FeedbackDTO } já enviados para esta sessão
 * @param {Function} props.onSend        - async (studentId, content) => void
 * @param {Function} props.onDelete      - async (feedbackId) => void
 * @param {Function} props.onClose
 */
export default function FeedbackModal({ appointment, sentByStudent, onSend, onDelete, onClose }) {
  const [busyStudentId, setBusyStudentId] = useState(null)
  const [error, setError] = useState('')

  async function run(studentId, action) {
    setBusyStudentId(studentId)
    setError('')
    try {
      await action()
    } catch (err) {
      setError(err.message || 'Não foi possível concluir a ação.')
    } finally {
      setBusyStudentId(null)
    }
  }

  const sentCount = appointment.students.filter(student => sentByStudent[student.id]).length

  return (
    <Modal
      title="Feedback da sessão"
      subtitle={`${formatSessionDate(appointment)} · ${sentCount} de ${appointment.students.length} enviado(s)`}
      onClose={onClose}
      busy={busyStudentId !== null}
      wide
      footer={
        <button type="button" className="ag-btn ag-btn--ghost" onClick={onClose} disabled={busyStudentId !== null}>
          Fechar
        </button>
      }
    >
      <p className="ag-modal__text">
        Cada aluno recebe um feedback individual e é avisado por e-mail. Depois de enviado,
        o feedback não pode ser editado, só apagado.
      </p>

      <ul className="ag-fb-list">
        {appointment.students.map(student => {
          const sent = sentByStudent[student.id]
          const busy = busyStudentId === student.id
          return (
            <li key={student.id} className="ag-fb-item">
              <div className="ag-fb-item__head">
                <span className="ag-picker__avatar" aria-hidden="true">{student.name?.charAt(0).toUpperCase() ?? '?'}</span>
                <strong>{student.name}</strong>
                {sent && <span className="ag-card__ok">✓ Enviado</span>}
              </div>
              {sent
                ? <SentFeedback feedback={sent} busy={busy} onDelete={() => run(student.id, () => onDelete(sent.id))} />
                : <FeedbackEditor student={student} busy={busy} onSend={content => run(student.id, () => onSend(student.id, content))} />}
            </li>
          )
        })}
      </ul>

      {error && <div className="ag-error" role="alert">{error}</div>}
    </Modal>
  )
}