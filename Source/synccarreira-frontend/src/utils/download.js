/**
 * @file download.js
 * @description Helpers para baixar arquivos gerados pelo backend (relatórios .xlsx, PDFs).
 */

/** Nome do arquivo vindo do Content-Disposition (quando o CORS expõe o header). */
export function fileNameFromHeaders(headers, fallback) {
  const disposition = headers?.['content-disposition'] ?? ''
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8) return decodeURIComponent(utf8[1])
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1] : fallback
}

/** Em downloads o corpo do erro também vem como Blob: lê o JSON para mostrar a mensagem do backend. */
export async function blobErrorMessage(error, fallback) {
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
export function saveBlob(blob, fileName) {
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
export function slug(text) {
  return String(text ?? '')
    .normalize('NFD').replaceAll(/\p{Diacritic}/gu, '')
    .toLowerCase().replaceAll(/[^a-z0-9]+/g, '-').replaceAll(/(^-|-$)/g, '') || 'arquivo'
}

/**
 * GET que devolve arquivo: baixa e salva com o nome do backend (ou o fallback).
 * @param {import('axios').AxiosInstance} api
 * @returns {Promise<string>} nome do arquivo salvo
 */
export async function downloadFile(api, path, { fallbackName, errorMessage, timeout = 60000 }) {
  try {
    const response = await api.get(path, { responseType: 'blob', timeout })
    const fileName = fileNameFromHeaders(response.headers, fallbackName)
    saveBlob(response.data, fileName)
    return fileName
  } catch (error) {
    throw new Error(await blobErrorMessage(error, errorMessage))
  }
}