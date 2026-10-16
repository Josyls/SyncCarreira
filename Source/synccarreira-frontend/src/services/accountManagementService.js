import api from './api'
import { getAllStudents } from './studentService'

const ENDPOINTS = {
  aluno: '/students',
  psicologa: '/psychologists',
}

function normalizeAccount(profile, account) {
  return { ...account, profile }
}

export async function listManagedAccounts(profile) {
  if (profile === 'aluno') {
    const students = await getAllStudents({ suppressAuthRedirect: true })
    return students.map(student => normalizeAccount('aluno', student))
  }

  const { data } = await api.get(ENDPOINTS[profile], { suppressAuthRedirect: true })
  return data.map(psychologist => normalizeAccount('psicologa', psychologist))
}

export async function listActiveInstitutions() {
  const { data } = await api.get('/institutions', { suppressAuthRedirect: true })
  return data.filter(institution => institution.active)
}

export async function listManagedInstitutions() {
  const { data } = await api.get('/institutions', { suppressAuthRedirect: true })
  return data
}

export async function saveManagedInstitution(institution, id) {
  const payload = {
    legalName: institution.legalName.trim(),
    tradeName: institution.tradeName.trim(),
    cnpj: institution.cnpj.trim(),
    type: institution.type,
    ...(id ? { active: institution.active } : {}),
  }
  const response = id
    ? await api.put(`/institutions/${id}`, payload, { suppressAuthRedirect: true })
    : await api.post('/institutions', payload, { suppressAuthRedirect: true })
  return response.data
}

export async function saveManagedAccount(profile, account, id) {
  const payload = profile === 'aluno'
    ? {
        name: account.name.trim(),
        email: account.email.trim(),
        schollarYear: account.schollarYear,
        schoolType: account.schoolType,
      }
    : {
        name: account.name.trim(),
        email: account.email.trim(),
        crp: account.crp.trim(),
        contractExpirationDate: account.contractExpirationDate,
        institutionId: Number(account.institutionId),
      }

  const endpoint = ENDPOINTS[profile]
  const response = id
    ? await api.put(`${endpoint}/${id}`, payload, { suppressAuthRedirect: true })
    : await api.post(endpoint, payload, { suppressAuthRedirect: true })

  return normalizeAccount(profile, response.data)
}

export function deleteManagedAccount(profile, id) {
  return api.delete(`${ENDPOINTS[profile]}/${id}`, { suppressAuthRedirect: true })
}

export async function listManagedClasses(institutionId) {
  const { data } = await api.get('/classes', {
    params: institutionId ? { institutionId } : undefined,
    suppressAuthRedirect: true,
  })
  return data
}

export async function createManagedClass(classData) {
  const { data } = await api.post('/classes', {
    name: classData.name.trim(),
    schoolYear: Number(classData.schoolYear),
    institutionId: Number(classData.institutionId),
  }, { suppressAuthRedirect: true })
  return data
}

export function assignStudentToClass(studentId, classId) {
  return api.patch('/students/class', null, {
    params: { studentId, classId },
    suppressAuthRedirect: true,
  })
}
