import api from './api'

export async function getPsychologistPanel(psychologistId) {
  try {
    const { data } = await api.get(`/psychologist-panel/${psychologistId}`)
    return data
  } catch (error) {
    const message = error.response?.data?.error || error.response?.data?.message
    throw new Error(message || 'Não foi possível carregar o painel de progresso.')
  }
}
