import api from './api'

/** Busca o leque de áreas e carreiras da jornada atual concluída pelo aluno. */
export async function getCurrentJourneyResult() {
  const { data } = await api.get('/journey/result')
  return data
}
