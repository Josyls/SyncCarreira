/**
 * @file studentService.js
 * @description Consulta e manutenção de alunos.
 */

import api from './api'

export function getStudents() {
  return api.get('/students')
}

export function getStudentById(id) {
  return api.get(`/students/${id}`)
}

export function updateStudent(id, data) {
  return api.put(`/students/${id}`, data)
}

/**
 * Busca todos os alunos cadastrados (percorre todas as páginas).
 * @returns {Promise<Array<{id:number, name:string, email:string, className?:string, institutionName?:string}>>}
 */
export const getAllStudents = async ({ suppressAuthRedirect = false } = {}) => {
  const size = 100
  let page = 0
  let all = []
  let last = false

  while (!last) {
    const { data } = await api.get('/students', {
      params: { page, size, sort: 'name,asc' },
      suppressAuthRedirect,
    })
    // O backend retorna Page<...>; se um dia virar lista simples, também funciona
    const content = Array.isArray(data) ? data : (data.content ?? [])
    all = all.concat(content)
    last = Array.isArray(data) ? true : (data.last ?? true)
    page += 1
    if (page > 50) break // proteção contra loop infinito
  }

  return all
}