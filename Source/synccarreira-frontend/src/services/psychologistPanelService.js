/**
 * @file psychologistPanelService.js
 * @description Painel da psicóloga
 * Endpoints:
 *  - GET /psychologist-panel/{psychologistId}                               → resumo + alunos
 *  - GET /psychologist-panel/{psychologistId}/reports/classes/{classId}     → relatório da turma (.xlsx)
 *  - GET /psychologist-panel/{psychologistId}/reports/students/{studentId}  → relatório individual (.xlsx)
 */

import api from './api'
import { downloadFile, slug } from '../utils/download.js'

export async function getPsychologistPanel(psychologistId) {
  try {
    const { data } = await api.get(`/psychologist-panel/${psychologistId}`)
    return data
  } catch (error) {
    const message = error.response?.data?.error || error.response?.data?.message
    throw new Error(message || 'Não foi possível carregar o painel de progresso.')
  }
}

function downloadReport(path, fallbackName, errorMessage) {
  return downloadFile(api, path, { fallbackName, errorMessage })
}

/** RF-07: relatório geral da turma, com síntese e status de cada aluno. */
export function downloadClassReport(psychologistId, schoolClassId, className) {
  return downloadReport(
    `/psychologist-panel/${psychologistId}/reports/classes/${schoolClassId}`,
    `relatorio-turma-${slug(className)}.xlsx`,
    'Não foi possível gerar o relatório da turma.'
  )
}

/** RF-07: relatório individual do aluno. */
export function downloadStudentReport(psychologistId, studentId, studentName) {
  return downloadReport(
    `/psychologist-panel/${psychologistId}/reports/students/${studentId}`,
    `relatorio-aluno-${slug(studentName)}.xlsx`,
    'Não foi possível gerar o relatório do aluno.'
  )
}