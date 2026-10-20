/**
 * @file PsicologaAgendamentos.jsx
 * @description RF-08 — Agendamento de sessões pela psicóloga/orientadora.
 *
 * Permite criar, editar e cancelar sessões individuais ou em grupo.
 * Quando a conta Google está conectada, cada ação é refletida no Google Agenda
 * da psicóloga (com link do Google Meet) e o aluno é avisado pelo próprio Google.
 *
 * Ordem das operações (para não deixar Google e sistema desencontrados):
 *   criar    → cria evento no Google → salva no backend (se falhar, remove o evento)
 *   editar   → atualiza evento no Google → atualiza no backend
 *   cancelar → remove evento no Google → marca como cancelada no backend
 */

import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../../context/AuthContext.jsx'
import SessionCard, { MeetIcon } from './components/SessionCard.jsx'
import SessionFormModal from './components/SessionFormModal.jsx'
import CancelModal from './components/CancelModal.jsx'
import FeedbackModal from './components/FeedbackModal.jsx'
import {
  getPsychologistAppointments, createAppointment, updateAppointment,
  cancelAppointment, displayStatus, USE_MOCK,
} from '../../services/appointmentService'
import { getSentFeedbacks, sendSessionFeedback, deleteFeedback } from '../../services/feedbackService'
import { getPsychologistPanel } from '../../services/psychologistPanelService'
import {
  isGoogleConfigured, isGoogleConnected, connectGoogle, disconnectGoogle,
  createMeetEvent, updateMeetEvent, cancelMeetEvent, preloadGoogle,
} from '../../services/googleCalendarService'
import { getAllStudents } from '../../services/studentService'

/**
 * Remove do Google um evento recém-criado quando o backend falhou (evita evento "órfão").
 * Fica fora do componente porque não depende de estado. Nunca rejeita: se nem o rollback
 * funcionar, o erro original (do backend) é o que importa mostrar.
 * @param {string} googleEventId
 * @returns {Promise<void>}
 */
function rollbackGoogleEvent(googleEventId) {
  return cancelMeetEvent(googleEventId).catch(() => undefined)
}

/**
 * Atalhos vindos do painel: ?aluno=ID abre "Nova sessão" com o aluno marcado,
 * ?nova=1 abre "Nova sessão" vazia.
 * @returns {Object|null} estado inicial do modal
 */
function readInitialModalFromUrl() {
  const params = new URLSearchParams(window.location.search)
  const studentId = Number(params.get('aluno'))
  const openNew = params.get('nova') === '1'
  if (!studentId && !openNew) return null
  return { type: 'form', preselectedStudentIds: studentId ? [studentId] : [] }
}

/** Remove os atalhos da URL, para não reabrir o formulário ao recarregar a página. */
function clearShortcutParams() {
  const params = new URLSearchParams(window.location.search)
  if (!params.has('aluno') && !params.has('nova')) return
  params.delete('aluno')
  params.delete('nova')
  const query = params.toString()
  window.history.replaceState(null, '', window.location.pathname + (query ? `?${query}` : ''))
}

/** Agrupa os feedbacks de sessão por agendamento: { [appointmentId]: { [studentId]: feedback } } */
function groupSessionFeedbacks(feedbacks) {
  const byAppointment = {}
  feedbacks
    .filter(feedback => feedback.type === 'SESSAO' && feedback.appointmentId)
    .forEach(feedback => {
      byAppointment[feedback.appointmentId] ??= {}
      byAppointment[feedback.appointmentId][feedback.studentId] = feedback
    })
  return byAppointment
}

const TABS = [
  { id: 'AGENDADA',  label: 'Próximas'   },
  { id: 'REALIZADA', label: 'Realizadas' },
  { id: 'CANCELADA', label: 'Canceladas' },
]

/** Botão de feedback de uma sessão realizada, com quantos participantes já receberam. */
function SessionFeedbackActions({ sent, total, onOpen }) {
  const done = sent === total
  let label = 'Enviar feedback'
  if (done) label = 'Ver feedbacks'
  else if (sent > 0) label = 'Continuar feedbacks'

  return (
    <>
      <button className={`ag-btn ag-btn--sm ${done ? 'ag-btn--outline' : 'ag-btn--primary'}`} onClick={onOpen}>
        {label}
      </button>
      <span className={done ? 'ag-card__ok' : 'ag-muted-sm'}>
        {done ? '✓ ' : ''}{sent} de {total} feedback(s) enviado(s)
      </span>
    </>
  )
}

export default function PsicologaAgendamentos() {
  const { user } = useAuth()
  const psychologist = useMemo(
    () => ({ id: user?.id, name: user?.nome, email: user?.email }),
    [user]
  )

  const [appointments, setAppointments] = useState([])
  const [loading, setLoading]           = useState(true)
  const [loadError, setLoadError]       = useState('')

  const [students, setStudents]               = useState([])
  const [loadingStudents, setLoadingStudents] = useState(true)
  const [doubtStudents, setDoubtStudents]     = useState([])
  const [loadingDoubts, setLoadingDoubts]     = useState(true)
  const [doubtError, setDoubtError]           = useState('')
  const [institutionStudentIds, setInstitutionStudentIds] = useState(null)

  const [tab, setTab]             = useState('AGENDADA')
  const [googleOn, setGoogleOn]   = useState(isGoogleConnected())
  const [googleMsg, setGoogleMsg] = useState('')
  const [toast, setToast]         = useState('')

  // Feedbacks individuais de sessão já enviados (RF-09)
  const [sentFeedbacks, setSentFeedbacks] = useState([])

  // Modais: { type: 'form'|'cancel'|'feedback', appointment?, preselectedStudentIds? }
  const [modal, setModal] = useState(readInitialModalFromUrl)
  useEffect(() => { clearShortcutParams() }, [])

  // ── Carregamento ────────────────────────────────────────────
  const load = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      setAppointments(await getPsychologistAppointments(psychologist.id))
    } catch (err) {
      setLoadError(err.message)
    } finally {
      setLoading(false)
    }
  }, [psychologist.id])

  useEffect(() => { load() }, [load])

  const loadSentFeedbacks = useCallback(async () => {
    try {
      setSentFeedbacks(await getSentFeedbacks())
    } catch {
      setSentFeedbacks([]) // a lista de sessões continua funcionando sem essa informação
    }
  }, [])

  useEffect(() => { void loadSentFeedbacks() }, [loadSentFeedbacks])

  useEffect(() => {
    preloadGoogle()
    getAllStudents()
      .then(setStudents)
      .catch(() => setStudents([]))
      .finally(() => setLoadingStudents(false))
  }, [])

  const loadDoubtStudents = useCallback(async () => {
    if (!psychologist.id) return
    setLoadingDoubts(true)
    setDoubtError('')
    setInstitutionStudentIds(null)
    try {
      const panel = await getPsychologistPanel(psychologist.id)
      setInstitutionStudentIds(new Set((panel.students ?? []).map(student => student.studentId)))
      const flagged = (panel.students ?? [])
        .filter(student => student.inDoubt)
        .sort((a, b) => new Date(a.doubtFlaggedAt) - new Date(b.doubtFlaggedAt))
      setDoubtStudents(flagged)
    } catch (err) {
      setDoubtError('Não foi possível confirmar quais alunos pertencem à sua instituição. Tente novamente.')
      setInstitutionStudentIds(new Set())
    } finally {
      setLoadingDoubts(false)
    }
  }, [psychologist.id])

  useEffect(() => { void loadDoubtStudents() }, [loadDoubtStudents])

  useEffect(() => {
    if (!toast) return
    const t = setTimeout(() => setToast(''), 4000)
    return () => clearTimeout(t)
  }, [toast])

  // ── Google ──────────────────────────────────────────────────
  async function ensureGoogle() {
    if (isGoogleConnected()) return
    await connectGoogle(psychologist.email)
    setGoogleOn(true)
  }

  async function handleConnectGoogle() {
    setGoogleMsg('')
    try {
      await ensureGoogle()
    } catch (err) {
      setGoogleMsg(err.message)
    }
  }

  function handleDisconnectGoogle() {
    disconnectGoogle()
    setGoogleOn(false)
  }

  // ── Ações ───────────────────────────────────────────────────
  function replaceInList(updated) {
    setAppointments(list =>
      list.map(a => (a.id === updated.id ? updated : a))
          .sort((x, y) => new Date(x.dateTime) - new Date(y.dateTime)))
  }

  /**
   * Cria ou atualiza o evento no Google Agenda (quando a opção está marcada).
   * @returns {{ google: Object, createdNow: boolean }} dados do Google + se o evento acabou de ser criado
   */
  async function syncSessionWithGoogle(form, editing) {
    const current = {
      googleEventId: editing?.googleEventId ?? null,
      meetLink:      editing?.meetLink ?? null,
      calendarLink:  editing?.calendarLink ?? null,
    }
    if (!form.syncGoogle) return { google: current, createdNow: false }

    await ensureGoogle()
    if (current.googleEventId) {
      const google = await updateMeetEvent(current.googleEventId, { ...form, meetLink: current.meetLink })
      return { google, createdNow: false }
    }
    return { google: await createMeetEvent(form), createdNow: true }
  }

  /** Salva no backend (ou no mock) e atualiza a lista na tela. */
  async function persistSession(editing, payload) {
    if (editing) {
      replaceInList(await updateAppointment(editing.id, payload))
      setToast(payload.syncGoogle ? 'Sessão atualizada e alunos avisados pelo Google Agenda.' : 'Sessão atualizada.')
      return
    }
    const created = await createAppointment(payload)
    setAppointments(list =>
      [...list, created].sort((x, y) => new Date(x.dateTime) - new Date(y.dateTime)))
    setTab('AGENDADA')
    setToast(payload.syncGoogle ? 'Sessão agendada! Convite com link do Meet enviado aos alunos.' : 'Sessão agendada.')
  }

  async function handleSaveSession(form) {
    const editing = modal?.appointment
    const { google, createdNow } = await syncSessionWithGoogle(form, editing)

    try {
      await persistSession(editing, { ...form, ...google, psychologist })
      setModal(null)
    } catch (err) {
      if (createdNow && google.googleEventId) {
        await rollbackGoogleEvent(google.googleEventId)
      }
      throw err
    }
  }

  async function handleCancelSession(reason) {
    const a = modal.appointment
    if (a.googleEventId && isGoogleConfigured()) {
      await ensureGoogle()
      await cancelMeetEvent(a.googleEventId)
    }
    replaceInList(await cancelAppointment(a.id, reason))
    setModal(null)
    setToast('Sessão cancelada. Os alunos foram avisados.')
  }

  async function handleSendSessionFeedback(studentId, content) {
    const created = await sendSessionFeedback({ appointmentId: modal.appointment.id, studentId, content })
    setSentFeedbacks(list => [created, ...list])
    setToast('Feedback enviado. O aluno foi avisado por e-mail.')
  }

  async function handleDeleteFeedback(feedbackId) {
    await deleteFeedback(feedbackId)
    setSentFeedbacks(list => list.filter(feedback => feedback.id !== feedbackId))
  }

  // ── Agrupamento por aba ─────────────────────────────────────
  const grouped = useMemo(() => {
    const g = { AGENDADA: [], REALIZADA: [], CANCELADA: [] }
    appointments.forEach(a => g[displayStatus(a)].push(a))
    g.REALIZADA.reverse() // mais recentes primeiro
    g.CANCELADA.reverse()
    return g
  }, [appointments])

  const feedbacksByAppointment = useMemo(() => groupSessionFeedbacks(sentFeedbacks), [sentFeedbacks])
  const sentCountOf = appointment => appointment.students
    .filter(student => feedbacksByAppointment[appointment.id]?.[student.id]).length
  const pendingFeedback = grouped.REALIZADA.filter(a => sentCountOf(a) < a.students.length).length
  const visible = grouped[tab]
  const doubtStudentIds = new Set(doubtStudents.map(student => student.studentId))
  const studentsWithDoubtStatus = students.filter(student => institutionStudentIds?.has(student.id)).map(student => ({
    ...student,
    inDoubt: doubtStudentIds.has(student.id),
  }))

  return (
    <>
      {/* Cabeçalho da página */}
      <div className="ag-page-head">
        <div>
          <h1 className="ag-h1">Agendamentos</h1>
          <p className="ag-sub">Crie e acompanhe sessões individuais ou em grupo com seus alunos.</p>
        </div>
        <button className="ag-btn ag-btn--primary" onClick={() => setModal({ type: 'form' })}>
          + Nova sessão
        </button>
      </div>

      {/* Status da integração Google */}
      <div className={`ag-google${googleOn ? ' ag-google--on' : ''}`}>
        <span className="ag-google__logo" aria-hidden="true">
          <svg viewBox="0 0 48 48" width="20" height="20">
            <path fill="#4285F4" d="M45.1 24.5c0-1.6-.1-3.1-.4-4.5H24v8.5h11.8c-.5 2.8-2.1 5.1-4.4 6.7v5.6h7.1c4.2-3.8 6.6-9.5 6.6-16.3z"/>
            <path fill="#34A853" d="M24 46c5.9 0 10.9-2 14.5-5.3l-7.1-5.6c-2 1.3-4.5 2.1-7.4 2.1-5.7 0-10.5-3.8-12.2-9H4.5v5.7C8.1 41 15.5 46 24 46z"/>
            <path fill="#FBBC05" d="M11.8 28.2c-.4-1.3-.7-2.7-.7-4.2s.3-2.9.7-4.2v-5.7H4.5C3 17.1 2 20.4 2 24s1 6.9 2.5 9.9l7.3-5.7z"/>
            <path fill="#EA4335" d="M24 10.8c3.2 0 6.1 1.1 8.4 3.3l6.3-6.3C34.9 4.2 29.9 2 24 2 15.5 2 8.1 7 4.5 14.1l7.3 5.7c1.7-5.2 6.5-9 12.2-9z"/>
          </svg>
        </span>
        <div className="ag-google__text">
          {!isGoogleConfigured() && (
            <><strong>Google Agenda não configurado.</strong> As sessões serão salvas sem link do Meet. Defina <code>VITE_GOOGLE_CLIENT_ID</code> no .env.</>
          )}
          {isGoogleConfigured() && !googleOn && (
            <><strong>Conecte seu Google Agenda</strong> para gerar o link do Meet e enviar o convite aos alunos automaticamente.</>
          )}
          {isGoogleConfigured() && googleOn && (
            <><strong>Google Agenda conectado.</strong> Novas sessões geram link do Meet e convite por e-mail.</>
          )}
          {googleMsg && <span className="ag-google__err">{googleMsg}</span>}
        </div>
        {isGoogleConfigured() && (
          googleOn
            ? <button className="ag-btn ag-btn--ghost ag-btn--sm" onClick={handleDisconnectGoogle}>Desconectar</button>
            : <button className="ag-btn ag-btn--outline ag-btn--sm" onClick={handleConnectGoogle}>Conectar Google</button>
        )}
      </div>

      {USE_MOCK && (
        <div className="ag-note">
          Modo demonstração: os agendamentos estão sendo salvos só neste navegador
          (<code>VITE_APPOINTMENTS_MOCK=true</code>). Remova essa variável do .env para usar o backend.
        </div>
      )}

      {/* Abas */}
      <div className="ag-tabs" role="tablist">
        {TABS.map(t => (
          <button
            key={t.id}
            role="tab"
            aria-selected={tab === t.id}
            className={`ag-tab${tab === t.id ? ' ag-tab--on' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
            <span className="ag-tab__count">{grouped[t.id].length}</span>
            {t.id === 'REALIZADA' && pendingFeedback > 0 && (
              <span className="ag-tab__dot" title={`${pendingFeedback} sem feedback`} />
            )}
          </button>
        ))}
      </div>

      {/* Lista */}
      <section className="ag-list" aria-live="polite">
        {loading && <div className="ag-state"><div className="ag-spinner" />Carregando sessões…</div>}

        {!loading && loadError && (
          <div className="ag-state ag-state--error">
            {loadError}
            <button className="ag-btn ag-btn--ghost ag-btn--sm" onClick={load}>Tentar de novo</button>
          </div>
        )}

        {!loading && !loadError && visible.length === 0 && (
          <div className="ag-empty">
            {tab === 'AGENDADA' && <>Nenhuma sessão futura. <button className="ag-link" onClick={() => setModal({ type: 'form' })}>Agende a primeira</button>.</>}
            {tab === 'REALIZADA' && 'As sessões que já aconteceram aparecem aqui para você registrar o feedback.'}
            {tab === 'CANCELADA' && 'Nenhuma sessão cancelada.'}
          </div>
        )}

        {!loading && !loadError && visible.map(a => (
          <SessionCard key={a.id} appointment={a} viewer="psicologa">
            {tab === 'AGENDADA' && (
              <>
                {a.meetLink && (
                  <a className="ag-btn ag-btn--meet ag-btn--sm" href={a.meetLink} target="_blank" rel="noreferrer">
                    <MeetIcon /> Abrir Meet
                  </a>
                )}
                <button className="ag-btn ag-btn--outline ag-btn--sm"
                        onClick={() => setModal({ type: 'form', appointment: a })}>Editar</button>
                <button className="ag-btn ag-btn--ghost-danger ag-btn--sm"
                        onClick={() => setModal({ type: 'cancel', appointment: a })}>Cancelar</button>
              </>
            )}
            {tab === 'REALIZADA' && (
              <SessionFeedbackActions
                sent={sentCountOf(a)}
                total={a.students.length}
                onOpen={() => setModal({ type: 'feedback', appointment: a })}
              />
            )}
          </SessionCard>
        ))}
      </section>

      {/* Modais */}
      {modal?.type === 'form' && (
        <SessionFormModal
          initial={modal.appointment}
          students={studentsWithDoubtStatus}
          loadingStudents={loadingStudents || loadingDoubts}
          doubtStudentsLoading={loadingDoubts}
          doubtStudentsError={doubtError}
          doubtStudentCount={doubtStudents.length}
          onRetryDoubtStudents={loadDoubtStudents}
          preselectedStudentIds={modal.preselectedStudentIds}
          onSubmit={handleSaveSession}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'cancel' && (
        <CancelModal
          appointment={modal.appointment}
          onConfirm={handleCancelSession}
          onClose={() => setModal(null)}
        />
      )}
      {modal?.type === 'feedback' && (
        <FeedbackModal
          appointment={modal.appointment}
          sentByStudent={feedbacksByAppointment[modal.appointment.id] ?? {}}
          onSend={handleSendSessionFeedback}
          onDelete={handleDeleteFeedback}
          onClose={() => setModal(null)}
        />
      )}

      {toast && <div className="ag-toast" aria-live="polite">{toast}</div>}
    </>
  )
}