/**
 * @file JourneyFeedbackModal.jsx
 * @description RF-09 — Feedback textual da psicóloga ao aluno ao final da jornada.
 *
 * POST /feedbacks/journey. Sem journeyNumber, o backend usa a última jornada concluída.
 * Um feedback por jornada; o aluno é avisado por e-mail e lê na área dele.
 */

import { useState } from 'react'
import Modal from '../../Agendamentos/components/Modal.jsx'
import { FEEDBACK_MAX_LENGTH } from '../../../services/feedbackService.js'

/**
 * @param {Object}   props
 * @param {Object}   props.student       - { studentId, studentName, schoolClassName }
 * @param {number}   [props.journeyNumber] - última jornada concluída (só para exibir)
 * @param {Function} props.onSend        - async (content) => void
 * @param {Function} props.onClose
 */
export default function JourneyFeedbackModal({ student, journeyNumber, onSend, onClose }) {
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function handleSend() {
    if (!text.trim()) {
      setError('Escreva o feedback antes de enviar.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await onSend(text.trim())
    } catch (err) {
      setError(err.message || 'Não foi possível enviar o feedback.')
      setBusy(false)
    }
  }

  const journeyLabel = journeyNumber ? `Jornada nº ${journeyNumber}` : 'Última jornada concluída'

  return (
    <Modal
      title="Feedback de fim de jornada"
      subtitle={`${student.studentName} · ${student.schoolClassName || 'Sem turma'} · ${journeyLabel}`}
      onClose={onClose}
      busy={busy}
      footer={
        <>
          <button type="button" className="ag-btn ag-btn--ghost" onClick={onClose} disabled={busy}>Voltar</button>
          <button type="button" className="ag-btn ag-btn--primary" onClick={handleSend} disabled={busy}>
            {busy ? 'Enviando…' : 'Enviar feedback'}
          </button>
        </>
      }
    >
      <p className="ag-modal__text">
        O aluno é avisado por e-mail e lê o feedback na área de agendamentos. Depois de enviado,
        ele não pode ser editado, só apagado.
      </p>
      <div className="ag-field">
        <label className="ag-label" htmlFor="pd-journey-feedback">Feedback para o aluno</label>
        <textarea
          id="pd-journey-feedback"
          className="ag-input ag-textarea"
          rows={8}
          maxLength={FEEDBACK_MAX_LENGTH}
          placeholder="O que você observou na jornada, pontos fortes, áreas para explorar e próximos passos…"
          value={text}
          onChange={event => setText(event.target.value)}
        />
        <span className="ag-counter">{text.length}/{FEEDBACK_MAX_LENGTH}</span>
      </div>
      {error && <div className="ag-error" role="alert">{error}</div>}
    </Modal>
  )
}