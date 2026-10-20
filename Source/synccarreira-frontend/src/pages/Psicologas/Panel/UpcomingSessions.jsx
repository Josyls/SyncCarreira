/**
 * @file UpcomingSessions.jsx
 * @description RF-08 no painel — próximas sessões agendadas, com atalho para a agenda.
 */

import { displayStatus, formatSessionDate, TYPES } from '../../../services/appointmentService'

const MAX_ITEMS = 3

/**
 * @param {Object}   props
 * @param {Array}    props.appointments - AppointmentDTO normalizados
 * @param {boolean}  props.loading
 * @param {string}   [props.error]
 * @param {Function} props.onOpenSchedule - () => void
 * @param {Function} props.onNewSession   - () => void
 */
export default function UpcomingSessions({ appointments, loading, error, onOpenSchedule, onNewSession }) {
  const upcoming = appointments.filter(item => displayStatus(item) === 'AGENDADA')
  const realized = appointments.filter(item => displayStatus(item) === 'REALIZADA').length

  return (
    <section className="pd-panel pd-sessions" aria-labelledby="pd-sessions-title">
      <div className="pd-section-heading">
        <div>
          <h2 id="pd-sessions-title">Próximas sessões</h2>
          <p>{upcoming.length} agendada(s) · {realized} realizada(s)</p>
        </div>
        <div className="pd-sessions__actions">
          <button type="button" className="ag-btn ag-btn--outline ag-btn--sm" onClick={onOpenSchedule}>Ver agenda</button>
          <button type="button" className="ag-btn ag-btn--primary ag-btn--sm" onClick={onNewSession}>+ Nova sessão</button>
        </div>
      </div>

      {loading && <p className="pd-empty-inline">Carregando sessões…</p>}
      {!loading && error && <p className="pd-reports__msg pd-reports__msg--error">{error}</p>}
      {!loading && !error && upcoming.length === 0 && (
        <p className="pd-empty-inline">Nenhuma sessão agendada.</p>
      )}
      {!loading && !error && upcoming.length > 0 && (
        <ul className="pd-session-list">
          {upcoming.slice(0, MAX_ITEMS).map(item => (
            <li key={item.id} className="pd-session">
              <div>
                <strong>{item.title}</strong>
                <span>{formatSessionDate(item)}</span>
              </div>
              <span className="pd-session__who">
                {item.scheduleType === TYPES.GRUPO ? `Grupo · ${item.students.length}` : item.students[0]?.name}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}