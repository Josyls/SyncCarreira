import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { useAuth } from '../../context/AuthContext.jsx'
import { getPsychologistPanel } from '../../services/psychologistPanelService.js'
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

function StudentDetails({ student }) {
  const synthesisStatus = getSynthesisStatus(student)
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
    </div>
  )
}

export default function ProgressDashboardPage() {
  const { user } = useAuth()
  const [panel, setPanel] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [classFilter, setClassFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [expandedStudentId, setExpandedStudentId] = useState(null)

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

  useEffect(() => { void loadPanel() }, [loadPanel])

  const students = panel?.students ?? []
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

  return (
    <div className="pd-page">
      <AppHeader />
      <main className="pd-main">
        <header className="pd-heading">
          <div>
            <span className="pd-eyebrow">ACOMPANHAMENTO DA JORNADA</span>
            <h1>Painel de progresso</h1>
            <p>Visão geral das trilhas dos alunos da sua instituição.</p>
          </div>
          <button className="pd-refresh" type="button" onClick={loadPanel} disabled={loading}>
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
              <MetricCard label="Em dúvida" value={panel.inDoubtStudents} note="sinalizaram que precisam conversar" tone="amber" />
            </section>

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

            {attentionStudents.length > 0 && (
              <section className="pd-attention" aria-labelledby="pd-attention-title">
                <div className="pd-attention__icon" aria-hidden="true">!</div>
                <div className="pd-attention__body">
                  <h2 id="pd-attention-title">Alunos que sinalizaram dúvida</h2>
                  <p>Esses alunos indicaram que gostariam de conversar sobre a escolha profissional.</p>
                  <div className="pd-attention__list">
                    {attentionStudents.map(student => (
                      <button key={student.studentId} type="button" onClick={() => setExpandedStudentId(student.studentId)}>
                        {student.studentName}<span>{TRAIL_LABELS[student.currentTrail] ?? student.currentTrail ?? 'Jornada concluída'}</span>
                      </button>
                    ))}
                  </div>
                </div>
              </section>
            )}

            <section className="pd-panel pd-students" aria-labelledby="pd-students-title">
              <div className="pd-section-heading pd-students__heading">
                <div><h2 id="pd-students-title">Progresso dos alunos</h2><p>Consulte o andamento individual por trilha.</p></div>
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
                    <thead><tr><th>Aluno</th><th>Turma</th><th>Etapa atual</th><th>Progresso</th><th>Status</th><th><span className="pd-sr-only">Detalhes</span></th></tr></thead>
                    <tbody>
                      {filteredStudents.map(student => (
                        <Fragment key={student.studentId}>
                          <tr key={student.studentId}>
                            <td><strong>{student.studentName}</strong>{student.inDoubt && <span className="pd-table__flag">Em dúvida</span>}</td>
                            <td>{student.schoolClassName || 'Sem turma'}</td>
                            <td>{TRAIL_LABELS[student.currentTrail] ?? student.currentTrail ?? 'Jornada concluída'}</td>
                            <td><strong>{student.progressPercentage}%</strong><span className="pd-table__sub">{student.answeredQuestions}/{student.totalQuestions} perguntas</span></td>
                            <td><StatusPill status={student.journeyStatus} label={statusLabel(student.journeyStatus)} /></td>
                            <td><button className="pd-expand" type="button" aria-expanded={expandedStudentId === student.studentId} onClick={() => setExpandedStudentId(current => current === student.studentId ? null : student.studentId)}>{expandedStudentId === student.studentId ? 'Fechar' : 'Detalhes'}</button></td>
                          </tr>
                          {expandedStudentId === student.studentId && <tr key={`${student.studentId}-details`}><td className="pd-table__details-cell" colSpan="6"><StudentDetails student={student} /></td></tr>}
                        </Fragment>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : <p className="pd-empty-inline">Nenhum aluno corresponde aos filtros escolhidos.</p>}
            </section>
          </>
        )}
      </main>
    </div>
  )
}
