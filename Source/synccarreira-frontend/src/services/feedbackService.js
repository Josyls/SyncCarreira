/**
 * @file feedbackService.js
 * @description Feedbacks da psicóloga para o aluno (RF-09) e área de feedbacks do aluno (RF-16).
 *
 * Endpoints (FeedbackController):
 *  - POST   /feedbacks/journey             → feedback de fim de jornada { studentId, journeyNumber?, content }
 *  - POST   /feedbacks/session             → feedback individual de sessão { appointmentId, studentId, content }
 *  - GET    /feedbacks/sent                → feedbacks enviados pela psicóloga logada
 *  - DELETE /feedbacks/{id}                → apaga um feedback (não existe edição)
 *  - GET    /feedbacks/panel/{psychologistId} → situação de feedback de cada aluno da instituição
 *  - GET    /feedbacks/me                  → feedbacks recebidos pelo aluno logado
 *  - PATCH  /feedbacks/{id}/read           → aluno marca como lido
 *
 * Regras do backend: um feedback por jornada e um por aluno em cada sessão;
 * feedback de sessão só depois do horário de término; o aluno recebe um e-mail avisando.
 */

import api from './api'

export const FEEDBACK_MAX_LENGTH = 5000

export const FEEDBACK_TYPE_LABEL = {
  JORNADA: 'Fim de jornada',
  SESSAO: 'Sessão de orientação',
}

function toError(error, fallback) {
  const data = error.response?.data
  const validation = data?.errors?.map(item => item.message).join(' ')
  return new Error(validation || data?.error || data?.message || fallback)
}

// ─── Psicóloga ────────────────────────────────────────────────

/** Feedback de fim de jornada. Sem journeyNumber, vale para a última jornada concluída. */
export async function sendJourneyFeedback({ studentId, journeyNumber, content }) {
  try {
    const { data } = await api.post('/feedbacks/journey', { studentId, journeyNumber, content })
    return data
  } catch (error) {
    throw toError(error, 'Não foi possível enviar o feedback da jornada.')
  }
}

/** Feedback individual de um aluno sobre uma sessão realizada. */
export async function sendSessionFeedback({ appointmentId, studentId, content }) {
  try {
    const { data } = await api.post('/feedbacks/session', { appointmentId, studentId, content })
    return data
  } catch (error) {
    throw toError(error, 'Não foi possível enviar o feedback da sessão.')
  }
}

export async function getSentFeedbacks() {
  try {
    const { data } = await api.get('/feedbacks/sent')
    return data ?? []
  } catch (error) {
    throw toError(error, 'Não foi possível carregar os feedbacks enviados.')
  }
}

export async function deleteFeedback(id) {
  try {
    await api.delete(`/feedbacks/${id}`)
  } catch (error) {
    throw toError(error, 'Não foi possível apagar o feedback.')
  }
}

/** Situação de feedback por aluno (jornadas concluídas, feedback de jornada enviado, nº de feedbacks de sessão). */
export async function getFeedbackStatus(psychologistId) {
  try {
    const { data } = await api.get(`/feedbacks/panel/${psychologistId}`)
    return data ?? []
  } catch (error) {
    throw toError(error, 'Não foi possível carregar a situação dos feedbacks.')
  }
}

// ─── Aluno ────────────────────────────────────────────────────

export async function getMyFeedbacks() {
  try {
    const { data } = await api.get('/feedbacks/me')
    return data ?? []
  } catch (error) {
    throw toError(error, 'Não foi possível carregar seus feedbacks.')
  }
}

export async function markFeedbackAsRead(id) {
  try {
    const { data } = await api.patch(`/feedbacks/${id}/read`)
    return data
  } catch (error) {
    throw toError(error, 'Não foi possível marcar o feedback como lido.')
  }
}