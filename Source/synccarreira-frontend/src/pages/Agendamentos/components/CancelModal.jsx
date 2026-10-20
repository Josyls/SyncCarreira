/**
 * @file CancelModal.jsx
 * @description Confirmação de cancelamento de sessão.
 */

import { useState } from 'react'
import Modal from './Modal.jsx'
import { formatSessionDate } from '../../../services/appointmentService'

export default function CancelModal({ appointment, onConfirm, onClose }) {
  const [reason, setReason] = useState('')
  const [busy, setBusy]     = useState(false)
  const [error, setError]   = useState('')

  async function handleConfirm() {
    // O backend exige o motivo (AppointmentCancelDTO.cancelReason é @NotBlank)
    if (!reason.trim()) {
      setError('Informe o motivo do cancelamento. Ele aparece para o aluno.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await onConfirm(reason.trim())
    } catch (err) {
      setError(err.message || 'Não foi possível cancelar a sessão.')
      setBusy(false)
    }
  }

  return (
    <Modal
      title="Cancelar sessão?"
      subtitle={formatSessionDate(appointment)}
      onClose={onClose}
      busy={busy}
      footer={
        <>
          <button type="button" className="ag-btn ag-btn--ghost" onClick={onClose} disabled={busy}>
            Manter sessão
          </button>
          <button type="button" className="ag-btn ag-btn--danger" onClick={handleConfirm} disabled={busy}>
            {busy ? 'Cancelando…' : 'Cancelar sessão'}
          </button>
        </>
      }
    >
      <p className="ag-modal__text">
        {appointment.googleEventId
          ? 'O evento será removido do Google Agenda e os alunos receberão um aviso de cancelamento por e-mail.'
          : 'A sessão ficará marcada como cancelada na área do aluno.'}
      </p>
      <div className="ag-field">
        <label className="ag-label" htmlFor="ag-cancel-reason">Motivo <em>(visível para o aluno)</em></label>
        <textarea
          id="ag-cancel-reason"
          className="ag-input ag-textarea"
          rows={3}
          maxLength={300}
          required
          placeholder="Ex.: Precisei remarcar por um compromisso na escola. Vou enviar um novo horário."
          value={reason}
          onChange={e => setReason(e.target.value)}
        />
      </div>
      {error && <div className="ag-error" role="alert">{error}</div>}
    </Modal>
  )
}