import api from './api'

export async function getMyStudentProfile() {
  const { data } = await api.get('/students/me')
  return data
}

export async function saveMyStudentAffinities(affinities) {
  const { data } = await api.put('/students/me/affinities', affinities)
  return data
}
