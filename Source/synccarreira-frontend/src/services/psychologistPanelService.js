/**
 * @file psychologistPanelService.js
 * @description Painel da psicóloga (RF-04 / RF-05) e relatórios para download (RF-07).
 *
 * Endpoints:
 *  - GET /psychologist-panel/{psychologistId}                               → resumo + alunos
 *  - GET /psychologist-panel/{psychologistId}/reports/classes/{classId}     → relatório da turma (.xlsx)
 *  - GET /psychologist-panel/{psychologistId}/reports/students/{studentId}  → relatório individual (.xlsx)
 */

import api from './api'

// Relatórios podem demorar mais que o timeout padrão (10 s) para serem gerados
const REPORT_TIMEOUT_MS = 60000

export async function getPsychologistPanel(psychologistId) {
  try {
    const { data } = await api.get(`/psychologist-panel/${psychologistId}`)
    return data
  } catch (error) {
    const message = error.response?.data?.error || error.response?.data?.message
    throw new Error(message || 'Não foi possível carregar o painel de progresso.')
  }
}

/** Nome do arquivo vindo do Content-Disposition (quando o CORS expõe o header). */
function fileNameFromHeaders(headers, fallback) {
  const disposition = headers?.['content-disposition'] ?? ''
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8) return decodeURIComponent(utf8[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : fallback
}

/** Em downloads o corpo do erro também vem como Blob: lê o JSON para mostrar a mensagem do backend. */
async function blobErrorMessage(error, fallback) {
  const data = error.response?.data
  if (data instanceof Blob) {
    try {
      const body = JSON.parse(await data.text())
      return body.error || body.message || fallback
    } catch {
      return fallback
    }
  }
  return data?.error || data?.message || fallback
}

/** Dispara o download de um Blob no navegador. */
function saveBlob(blob, fileName) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Nome de arquivo seguro: "3º Ano A" → "3-ano-a" */
function slug(text) {
  return String(text ?? '')
    .normalize('NFD').replaceAll(/\p{Diacritic}/gu, '')
    .toLowerCase().replaceAll(/[^a-z0-9]+/g, '-').replaceAll(/(^-|-$)/g, '') || 'relatorio'
}

async function downloadReport(path, fallbackName, errorMessage) {
  try {
    const response = await api.get(path, { responseType: 'blob', timeout: REPORT_TIMEOUT_MS })
    const fileName = fileNameFromHeaders(response.headers, fallbackName)
    saveBlob(response.data, fileName)
    return fileName
  } catch (error) {
    throw new Error(await blobErrorMessage(error, errorMessage))
  }
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