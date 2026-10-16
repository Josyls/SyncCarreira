import api from './api'

/** Lista links para a tela de curadoria da psicóloga. */
export async function getManagedInformationLinks() {
  const { data } = await api.get('/information-links')
  return Array.isArray(data) ? data : []
}

/** Busca os links recomendados ao aluno na trilha de Informação. */
export async function getStudentInformationTrail() {
  const { data } = await api.get('/information-links/me')
  return data
}

/** Cadastra um link de carreira ou de acesso à universidade. */
export async function createInformationLink(link) {
  const { data } = await api.post('/information-links', link)
  return data
}

/** Exclui um link da curadoria. */
export async function deleteInformationLink(id) {
  await api.delete(`/information-links/${id}`)
}
