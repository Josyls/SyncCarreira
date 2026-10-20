/**
 * @file GuidanceAlerts.jsx
 * @description RF-04 — Alerta de necessidade de orientação.
 *
 * Lista, no painel da psicóloga, os alunos que concluíram as trilhas e sinalizaram
 * dúvida/indecisão (campo `inDoubt` do GET /psychologist-panel). Os mais antigos
 * aparecem primeiro, porque estão esperando há mais tempo.
 */

const DAY_MS = 24 * 60 * 60 * 1000

function waitingTime(flaggedAt) {
  if (!flaggedAt) return ''
  const days = Math.floor((Date.now() - new Date(flaggedAt).getTime()) / DAY_MS)
  if (days <= 0) return 'hoje'
  if (days === 1) return 'há 1 dia'
  return `há ${days} dias`
}

function formatDate(flaggedAt) {
  return flaggedAt ? new Date(flaggedAt).toLocaleDateString('pt-BR') : 'data não informada'
}

/**
 * @param {Object}   props
 * @param {Array}    props.students          - alunos com inDoubt = true
 * @param {Object}   props.feedbackStatusById - { [studentId]: StudentFeedbackStatusDTO }
 * @param {Function} props.onSchedule        - (student) => void
 * @param {Function} props.onFeedback        - (student) => void
 * @param {Function} props.onDetails         - (student) => void
 */
export default function GuidanceAlerts({ students, feedbackStatusById, onSchedule, onFeedback, onDetails }) {
  const sorted = [...students].sort(
    (first, second) => new Date(first.doubtFlaggedAt ?? 0) - new Date(second.doubtFlaggedAt ?? 0)
  )

  if (sorted.length === 0) {
    return (
      <section className="pd-alerts pd-alerts--empty" aria-labelledby="pd-alerts-title">
        <div className="pd-alerts__icon pd-alerts__icon--ok" aria-hidden="true">✓</div>
        <div>
          <h2 id="pd-alerts-title">Alertas de orientação</h2>
          <p>Nenhum aluno concluiu as trilhas sinalizando dúvida ou indecisão até agora.</p>
        </div>
      </section>
    )
  }

  return (
    <section className="pd-alerts" aria-labelledby="pd-alerts-title">
      <div className="pd-alerts__header">
        <div className="pd-alerts__icon" aria-hidden="true">!</div>
        <div>
          <h2 id="pd-alerts-title">
            Alertas de orientação <span className="pd-alerts__count">{sorted.length}</span>
          </h2>
          <p>Alunos que concluíram as trilhas e sinalizaram dúvida ou indecisão sobre a escolha profissional.</p>
        </div>
      </div>

      <ul className="pd-alerts__list">
        {sorted.map(student => {
          const feedback = feedbackStatusById[student.studentId]
          return (
            <li key={student.studentId} className="pd-alert">
              <div className="pd-alert__info">
                <strong>{student.studentName}</strong>
                <span>{student.schoolClassName || 'Sem turma'}</span>
                <span className="pd-alert__since">
                  Sinalizou em {formatDate(student.doubtFlaggedAt)} · {waitingTime(student.doubtFlaggedAt)}
                </span>
              </div>
              <div className="pd-alert__actions">
                <button type="button" className="ag-btn ag-btn--primary ag-btn--sm" onClick={() => onSchedule(student)}>
                  Agendar sessão
                </button>
                {feedback?.journeyFeedbackSent
                  ? <span className="pd-alert__done">✓ Feedback enviado</span>
                  : (
                    <button
                      type="button"
                      className="ag-btn ag-btn--outline ag-btn--sm"
                      disabled={!feedback?.concludedJourneys}
                      title={feedback?.concludedJourneys ? undefined : 'Disponível quando a jornada for concluída'}
                      onClick={() => onFeedback(student)}
                    >
                      Enviar feedback
                    </button>
                  )}
                <button type="button" className="ag-btn ag-btn--ghost ag-btn--sm" onClick={() => onDetails(student)}>
                  Ver progresso
                </button>
              </div>
            </li>
          )
        })}
      </ul>
    </section>
  )
}