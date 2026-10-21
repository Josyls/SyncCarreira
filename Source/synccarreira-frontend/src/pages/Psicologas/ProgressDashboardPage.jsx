/**
 * @file ProgressDashboardPage.jsx
 * @description Painel da psicóloga/orientadora.
 *
 *  - RF-04: alertas de alunos que concluíram as trilhas com dúvida/indecisão
 *  - RF-05: progresso das turmas e dos alunos por etapa
 *  - RF-07: relatório da turma e relatórios individuais (.xlsx)
 *  - RF-08: próximas sessões e atalho para agendar (inclusive a partir de um alerta)
 *  - RF-09: feedback de fim de jornada e histórico de feedbacks enviados
 */

import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import {
  getPsychologistPanel, downloadClassReport, downloadStudentReport,
} from '../../services/psychologistPanelService.js'
import {
  getFeedbackStatus, getSentFeedbacks, sendJourneyFeedback, deleteFeedback,
} from '../../services/feedbackService.js'
import { getPsychologistAppointments } from '../../services/appointmentService.js'
import GuidanceAlerts from './Panel/GuidanceAlerts.jsx'
import ReportsPanel from './Panel/ReportsPanel.jsx'
import UpcomingSessions from './Panel/UpcomingSessions.jsx'
import SentFeedbacks from './Panel/SentFeedbacks.jsx'
import JourneyFeedbackModal from './Panel/JourneyFeedbackModal.jsx'
import '../Agendamentos/Agendamentos.css'
import './ProgressDashboardPage.css'

const JOURNEY_LABELS = {
  NAO_INICIADA: 'Não iniciada',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDA: 'Concluída',
}

const TRAIL_LABELS = {
  AUTOCONHECIMENTO: 'Autoconhecimento',
  INFLUENCIAS: 'Influências',
  PLANO_DE_FUTURO: 'Projeto de futuro',
  INFORMACAO: 'Informação',
}

function statusLabel(status) {
  return JOURNEY_LABELS[status] ?? 'Não iniciada'
}

function getTrailStatus(student, trail, index) {
  const hasIncompletePrevious = student.trails
    .slice(0, index)
    .some(previous => previous.status !== 'CONCLUIDA')
  if (hasIncompletePrevious) return { value: 'BLOQUEADA', label: 'Bloqueada' }
  return { value: trail.status, label: statusLabel(trail.status) }
}

function getSynthesisStatus(student) {
  if (student.finalSynthesisSubmitted) return { value: 'CONCLUIDA', label: 'Concluída' }
  const allQuestionTrailsDone = student.trails.length > 0
    && student.trails.every(trail => trail.status === 'CONCLUIDA')
  return allQuestionTrailsDone
    ? { value: 'EM_ANDAMENTO', label: 'Pendente' }
    : { value: 'BLOQUEADA', label: 'Bloqueada' }
}

function StatusPill({ status, label }) {
  return <span className={`pd-status pd-status--${status.toLowerCase()}`}>{label}</span>
}

function MetricCard({ label, value, note, tone }) {
  return (
    <article className={`pd-metric pd-metric--${tone}`}>
      <span className="pd-metric__label">{label}</span>
      <strong>{value}</strong>
      <span className="pd-metric__note">{note}</span>
    </article>
  )
}

/** Situação do feedback de jornada de um aluno (coluna "Feedback"). */
function FeedbackCell({ status, onSend }) {
  if (!status) return <span className="pd-table__sub">—</span>
  const sessionInfo = status.sessionFeedbacksSent > 0
    ? <span className="pd-table__sub">{status.sessionFeedbacksSent} de sessão</span>
    : null

  if (status.journeyFeedbackSent) {
    return <><span className="pd-feedback-ok">✓ Jornada</span>{sessionInfo}</>
  }
  if (!status.concludedJourneys) {
    return <><span className="pd-table__sub">Jornada não concluída</span>{sessionInfo}</>
  }
  return (
    <>
      <button type="button" className="pd-expand pd-expand--strong" onClick={onSend}>Enviar feedback</button>
      {sessionInfo}
    </>
  )
}

function StudentDetails({ student, feedbackStatus, onReport, onFeedback, onSchedule }) {
  const synthesisStatus = getSynthesisStatus(student)
  const canSendJourneyFeedback = feedbackStatus?.concludedJourneys > 0 && !feedbackStatus?.journeyFeedbackSent
  return (
    <div className="pd-details">
      <div className="pd-details__heading">
        <strong>Progresso por etapa</strong>
        <span>{student.concludedTrails} de {student.totalTrails} trilhas concluídas</span>
      </div>
      <div className="pd-stage-list">
        {student.trails.map((trail, index) => {
          const status = getTrailStatus(student, trail, index)
          return (
            <div className="pd-stage" key={trail.trailId}>
              <div className="pd-stage__top">
                <strong>{TRAIL_LABELS[trail.trailName] ?? trail.trailName}</strong>
                <StatusPill status={status.value} label={status.label} />
              </div>
              <div className="pd-progress-track" aria-label={`${trail.progressPercentage}% concluído`}>
                <span style={{ width: `${trail.progressPercentage}%` }} />
              </div>
              <span className="pd-stage__count">{trail.answeredQuestions} de {trail.totalQuestions} perguntas</span>
            </div>
          )
        })}
        <div className="pd-stage">
          <div className="pd-stage__top">
            <strong>Síntese final</strong>
            <StatusPill status={synthesisStatus.value} label={synthesisStatus.label} />
          </div>
          <p className="pd-stage__count">{student.finalSynthesisSubmitted ? 'Síntese enviada' : 'A síntese final ainda não foi enviada'}</p>
        </div>
      </div>
      <div className="pd-details__actions">
        <button type="button" className="ag-btn ag-btn--outline ag-btn--sm" onClick={onReport}>Baixar relatório individual</button>
        {canSendJourneyFeedback && (
          <button type="button" className="ag-btn ag-btn--outline ag-btn--sm" onClick={onFeedback}>Enviar feedback da jornada</button>
        )}
        <button type="button" className="ag-btn ag-btn--primary ag-btn--sm" onClick={onSchedule}>Agendar sessão</button>
      </div>
    </div>
  )
}

/** Linha da tabela de alunos (+ linha de detalhes quando expandida). */
function StudentRow({ student, expanded, feedbackStatus, reportBusy, onToggle, onReport, onFeedback, onSchedule }) {
  return (
    <Fragment>
      <tr>
        <td><strong>{student.studentName}</strong>{student.inDoubt && <span className="pd-table__flag">Em dúvida</span>}</td>
        <td>{student.schoolClassName || 'Sem turma'}</td>
        <td>{TRAIL_LABELS[student.currentTrail] ?? student.currentTrail ?? 'Jornada concluída'}</td>
        <td><strong>{student.progressPercentage}%</strong><span className="pd-table__sub">{student.answeredQuestions}/{student.totalQuestions} perguntas</span></td>
        <td><StatusPill status={student.journeyStatus} label={statusLabel(student.journeyStatus)} /></td>
        <td><FeedbackCell status={feedbackStatus} onSend={onFeedback} /></td>
        <td className="pd-table__actions">
          <button className="pd-expand" type="button" onClick={onReport} disabled={reportBusy} aria-label={`Baixar relatório de ${student.studentName}`}>
            {reportBusy ? 'Gerando…' : 'Relatório'}
          </button>
          <button className="pd-expand" type="button" aria-expanded={expanded} onClick={onToggle}>
            {expanded ? 'Fechar' : 'Detalhes'}
          </button>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td className="pd-table__details-cell" colSpan="7">
            <StudentDetails
              student={student}
              feedbackStatus={feedbackStatus}
              onReport={onReport}
              onFeedback={onFeedback}
              onSchedule={onSchedule}
            />
          </td>
        </tr>
      )}
    </Fragment>
  )
}

export default function ProgressDashboardPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [panel, setPanel] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [classFilter, setClassFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [expandedStudentId, setExpandedStudentId] = useState(null)

  // RF-09: situação e histórico dos feedbacks
  const [feedbackStatus, setFeedbackStatus] = useState([])
  const [sentFeedbacks, setSentFeedbacks] = useState([])
  const [feedbackError, setFeedbackError] = useState('')
  const [feedbackTarget, setFeedbackTarget] = useState(null) // aluno do modal de feedback de jornada

  // RF-08: sessões
  const [appointments, setAppointments] = useState([])
  const [loadingAppointments, setLoadingAppointments] = useState(true)
  const [appointmentsError, setAppointmentsError] = useState('')

  // RF-07: download individual em andamento
  const [reportBusyId, setReportBusyId] = useState(null)
  const [toast, setToast] = useState('')

  const loadPanel = useCallback(async () => {
    if (!user?.id) return
    setLoading(true)
    setError('')
    try {
      setPanel(await getPsychologistPanel(user.id))
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setLoading(false)
    }
  }, [user?.id])

  const loadFeedbacks = useCallback(async () => {
    if (!user?.id) return
    setFeedbackError('')
    try {
      const [status, sent] = await Promise.all([getFeedbackStatus(user.id), getSentFeedbacks()])
      setFeedbackStatus(status)
      setSentFeedbacks(sent)
    } catch (loadError) {
      setFeedbackError(loadError.message)
    }
  }, [user?.id])

  const loadAppointments = useCallback(async () => {
    if (!user?.id) return
    setLoadingAppointments(true)
    setAppointmentsError('')
    try {
      setAppointments(await getPsychologistAppointments(user.id))
    } catch (loadError) {
      setAppointmentsError(loadError.message)
    } finally {
      setLoadingAppointments(false)
    }
  }, [user?.id])

  const refreshAll = useCallback(() => {
    void loadPanel()
    void loadFeedbacks()
    void loadAppointments()
  }, [loadPanel, loadFeedbacks, loadAppointments])

  useEffect(() => { refreshAll() }, [refreshAll])

  useEffect(() => {
    if (!toast) return undefined
    const timer = setTimeout(() => setToast(''), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  const students = useMemo(() => panel?.students ?? [], [panel])
  const feedbackStatusById = useMemo(
    () => Object.fromEntries(feedbackStatus.map(item => [item.studentId, item])),
    [feedbackStatus]
  )
  const classes = useMemo(() => [...new Set(students.map(student => student.schoolClassName).filter(Boolean))]
    .sort((first, second) => first.localeCompare(second, 'pt-BR')), [students])
  const filteredStudents = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase('pt-BR')
    return students.filter(student => {
      const matchesName = !normalizedQuery || student.studentName?.toLocaleLowerCase('pt-BR').includes(normalizedQuery)
      const matchesClass = !classFilter || student.schoolClassName === classFilter
      const matchesStatus = !statusFilter || student.journeyStatus === statusFilter
      return matchesName && matchesClass && matchesStatus
    })
  }, [students, query, classFilter, statusFilter])

  const stages = useMemo(() => {
    if (!students.length) return []
    const questionStages = (students[0].trails ?? []).map(trail => ({
      id: trail.trailId,
      name: TRAIL_LABELS[trail.trailName] ?? trail.trailName,
      complete: students.filter(student => student.trails?.find(item => item.trailId === trail.trailId)?.status === 'CONCLUIDA').length,
    }))
    return [...questionStages, {
      id: 'final-synthesis',
      name: 'Síntese final',
      complete: students.filter(student => student.finalSynthesisSubmitted).length,
    }]
  }, [students])

  const attentionStudents = students.filter(student => student.inDoubt)

  // ── Ações ────────────────────────────────────────────────────
  function scheduleWith(student) {
    navigate(`/agendamentos?aluno=${student.studentId}`)
  }

  function showStudent(student) {
    setQuery('')
    setClassFilter('')
    setStatusFilter('')
    setExpandedStudentId(student.studentId)
    requestAnimationFrame(() => document.getElementById('pd-students-title')?.scrollIntoView({ behavior: 'smooth' }))
  }

  async function handleStudentReport(student) {
    setReportBusyId(student.studentId)
    try {
      const fileName = await downloadStudentReport(user.id, student.studentId, student.studentName)
      setToast(`Download iniciado: ${fileName}`)
    } catch (downloadError) {
      setToast(downloadError.message)
    } finally {
      setReportBusyId(null)
    }
  }

  async function handleSendJourneyFeedback(content) {
    const created = await sendJourneyFeedback({ studentId: feedbackTarget.studentId, content })
    setSentFeedbacks(list => [created, ...list])
    setFeedbackStatus(list => list.map(item => (
      item.studentId === feedbackTarget.studentId ? { ...item, journeyFeedbackSent: true } : item
    )))
    setFeedbackTarget(null)
    setToast('Feedback enviado. O aluno foi avisado por e-mail.')
  }

  async function handleDeleteFeedback(feedback) {
    await deleteFeedback(feedback.id)
    setSentFeedbacks(list => list.filter(item => item.id !== feedback.id))
    setFeedbackStatus(list => list.map(item => {
      if (item.studentId !== feedback.studentId) return item
      return feedback.type === 'JORNADA'
        ? { ...item, journeyFeedbackSent: false }
        : { ...item, sessionFeedbacksSent: Math.max(0, item.sessionFeedbacksSent - 1) }
    }))
    setToast('Feedback apagado.')
  }

  return (
    <div className="pd-page">
      <AppHeader />
      <main className="pd-main">
        <header className="pd-heading">
          <div>
            <span className="pd-eyebrow">ACOMPANHAMENTO DA JORNADA</span>
            <h1>Painel de progresso</h1>
            <p>Visão geral das trilhas, alertas, relatórios e feedbacks dos alunos da sua instituição.</p>
          </div>
          <button className="pd-refresh" type="button" onClick={refreshAll} disabled={loading}>
            {loading ? 'Atualizando…' : 'Atualizar painel'}
          </button>
        </header>

        {loading && <div className="pd-state" role="status">Carregando dados do painel…</div>}
        {!loading && error && (
          <div className="pd-state pd-state--error" role="alert">
            <strong>Não foi possível carregar o painel</strong>
            <span>{error}</span>
            <button type="button" onClick={loadPanel}>Tentar novamente</button>
          </div>
        )}
        {!loading && !error && panel && (
          <>
            <section className="pd-metrics" aria-label="Resumo da jornada">
              <MetricCard label="Alunos acompanhados" value={panel.totalStudents} note="na sua instituição" tone="purple" />
              <MetricCard label="Jornadas iniciadas" value={panel.inProgressStudents} note="com respostas registradas" tone="blue" />
              <MetricCard label="Jornadas concluídas" value={panel.concludedStudents} note="inclui a síntese final" tone="green" />
              <MetricCard label="Em dúvida" value={panel.inDoubtStudents} note="precisam de orientação" tone="amber" />
            </section>

            <GuidanceAlerts
              students={attentionStudents}
              feedbackStatusById={feedbackStatusById}
              onSchedule={scheduleWith}
              onFeedback={student => setFeedbackTarget(student)}
              onDetails={showStudent}
            />

            <div className="pd-grid">
              <UpcomingSessions
                appointments={appointments}
                loading={loadingAppointments}
                error={appointmentsError}
                onOpenSchedule={() => navigate('/agendamentos')}
                onNewSession={() => navigate('/agendamentos?nova=1')}
              />
              <ReportsPanel
                students={students}
                onDownload={(classId, className) => downloadClassReport(user.id, classId, className)}
              />
            </div>

            <section className="pd-panel" aria-labelledby="pd-stage-title">
              <div className="pd-section-heading">
                <div><h2 id="pd-stage-title">Conclusão por etapa</h2><p>Alunos que já concluíram cada etapa da jornada.</p></div>
              </div>
              {stages.length ? (
                <div className="pd-stage-chart">
                  {stages.map(stage => {
                    const percentage = panel.totalStudents ? Math.round(stage.complete * 100 / panel.totalStudents) : 0
                    return (
                      <div className="pd-chart-row" key={stage.id}>
                        <div className="pd-chart-row__label"><span>{stage.name}</span><strong>{stage.complete}/{panel.totalStudents}</strong></div>
                        <div className="pd-progress-track"><span style={{ width: `${percentage}%` }} /></div>
                      </div>
                    )
                  })}
                </div>
              ) : <p className="pd-empty-inline">Ainda não há alunos para exibir.</p>}
            </section>

            <section className="pd-panel pd-students" aria-labelledby="pd-students-title">
              <div className="pd-section-heading pd-students__heading">
                <div><h2 id="pd-students-title">Progresso dos alunos</h2><p>Andamento individual, feedback e relatório de cada aluno.</p></div>
                <span className="pd-result-count">{filteredStudents.length} de {students.length} alunos</span>
              </div>
              <div className="pd-filters">
                <label className="pd-search"><span className="pd-sr-only">Buscar aluno</span><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Buscar por nome…" /></label>
                <label><span className="pd-sr-only">Filtrar por turma</span><select value={classFilter} onChange={event => setClassFilter(event.target.value)}><option value="">Todas as turmas</option>{classes.map(name => <option key={name} value={name}>{name}</option>)}</select></label>
                <label><span className="pd-sr-only">Filtrar por status</span><select value={statusFilter} onChange={event => setStatusFilter(event.target.value)}><option value="">Todos os status</option><option value="NAO_INICIADA">Não iniciada</option><option value="EM_ANDAMENTO">Em andamento</option><option value="CONCLUIDA">Concluída</option></select></label>
              </div>

              {filteredStudents.length ? (
                <div className="pd-table-wrap">
                  <table className="pd-table">
                    <thead><tr><th>Aluno</th><th>Turma</th><th>Etapa atual</th><th>Progresso</th><th>Status</th><th>Feedback</th><th><span className="pd-sr-only">Ações</span></th></tr></thead>
                    <tbody>
                      {filteredStudents.map(student => (
                        <StudentRow
                          key={student.studentId}
                          student={student}
                          expanded={expandedStudentId === student.studentId}
                          feedbackStatus={feedbackStatusById[student.studentId]}
                          reportBusy={reportBusyId === student.studentId}
                          onToggle={() => setExpandedStudentId(current => (current === student.studentId ? null : student.studentId))}
                          onReport={() => handleStudentReport(student)}
                          onFeedback={() => setFeedbackTarget(student)}
                          onSchedule={() => scheduleWith(student)}
                        />
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : <p className="pd-empty-inline">Nenhum aluno corresponde aos filtros escolhidos.</p>}
            </section>

            <SentFeedbacks feedbacks={sentFeedbacks} error={feedbackError} onDelete={handleDeleteFeedback} />
          </>
        )}
      </main>

      {feedbackTarget && (
        <JourneyFeedbackModal
          student={feedbackTarget}
          journeyNumber={feedbackStatusById[feedbackTarget.studentId]?.lastConcludedJourneyNumber}
          onSend={handleSendJourneyFeedback}
          onClose={() => setFeedbackTarget(null)}
        />
      )}

      {toast && <div className="ag-toast" aria-live="polite">{toast}</div>}
    </div>
  )
}