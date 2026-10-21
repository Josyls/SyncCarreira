/**
 * @file journeyHistoryService.js
 * @description Jornadas do aluno
 *
 * Endpoints (JourneyHistoryController / JourneyResultController):
 *  - GET  /journeys/me                      → jornadas concluídas (JourneyRecordDTO[])
 *  - GET  /journeys/me/current              → situação da jornada atual (JourneyStatusDTO)
 *  - POST /journeys/me/new                  → inicia nova jornada (apaga respostas e sínteses atuais)
 *  - GET  /journeys/me/{journeyId}/pdf      → PDF de uma jornada concluída
 *  - GET  /journeys/me/{journeyId}/result   → resultado de uma jornada concluída (JourneyResultDTO)
 */

import api from './api'
import { downloadFile } from '../utils/download.js'

function toError(error, fallback) {
  const data = error.response?.data
  return new Error(data?.error || data?.message || fallback)
}

export async function getMyJourneyHistory() {
  const { data } = await api.get('/journeys/me')
  return Array.isArray(data) ? data : []
}

export async function getMyCurrentJourneyStatus() {
  const { data } = await api.get('/journeys/me/current')
  return data
}

/** Resultado (scores em %, áreas em destaque e sugestões) de uma jornada concluída. */
export async function getMyJourneyResult(journeyId) {
  try {
    const { data } = await api.get(`/journeys/me/${journeyId}/result`)
    return data
  } catch (error) {
    throw toError(error, 'Não foi possível carregar o resultado desta jornada.')
  }
}

/** Baixa o PDF de uma jornada concluída. */
export function downloadMyJourneyPdf(journeyId, journeyNumber) {
  return downloadFile(api, `/journeys/me/${journeyId}/pdf`, {
    fallbackName: `minha-jornada-${journeyNumber ?? journeyId}.pdf`,
    errorMessage: 'Não foi possível gerar o PDF desta jornada.',
  })
}

/**
 * Inicia uma nova jornada. A atual continua guardada no histórico,
 * mas as respostas e sínteses em andamento são apagadas.
 */
export async function startNewJourney() {
  try {
    const { data } = await api.post('/journeys/me/new')
    return data
  } catch (error) {
    throw toError(error, 'Não foi possível iniciar uma nova jornada.')
  }
}