import api from './api'

export async function getMyJourneyHistory() {
  const { data } = await api.get('/journeys/me')
  return Array.isArray(data) ? data : []
}

export async function getMyCurrentJourneyStatus() {
  const { data } = await api.get('/journeys/me/current')
  return data
}
