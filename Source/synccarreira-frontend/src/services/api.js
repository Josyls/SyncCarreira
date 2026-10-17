/**
 * @file api.js
 * @description Instância central do Axios para comunicação com o backend.
 *
 * Todas as requisições HTTP da aplicação passam por esta instância.
 * Ela é responsável por:
 *  - Definir a URL base do backend (via variável de ambiente)
 *  - Anexar automaticamente o token JWT em cada requisição
 *  - Tratar erros globais de autenticação (token expirado, não autorizado)
 *
 * ─── Como usar ────────────────────────────────────────────────
 * Nunca importe o axios diretamente nas páginas ou services.
 * Sempre importe esta instância configurada:
 *
 * @example
 * import api from './api'
 * const data = await api.get('/usuarios')
 * await api.post('/auth/login', { email, senha })
 */

import axios from 'axios'

// ─── Configuração da URL base ─────────────────────────────────
//
// O endereço do backend é lido do arquivo .env na raiz do projeto.
// Crie o arquivo .env com o conteúdo:
//   VITE_API_URL=http://localhost:8080
//
// Em produção, substitua pelo endereço real do servidor.
// O fallback garante que a aplicação não quebre se o .env não existir.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',

  // Timeout de 10 segundos — evita que requisições fiquem penduradas
  timeout: 10000,

  headers: {
    'Content-Type': 'application/json',
  },
})

// ─── Interceptor de Requisição (Request) ──────────────────────
//
// Executado ANTES de cada requisição ser enviada.
// Lê o token do localStorage e injeta no header Authorization.
//
// Fluxo:
//   1. Usuário faz login → token salvo no localStorage
//   2. Qualquer requisição seguinte → este interceptor adiciona o header
//   3. Backend valida o token e retorna os dados
//
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')

    if (token) {
      // Padrão Bearer Token — confirmado no API_CONTRACT.md (Authorization: Bearer <token>)
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    // Erro ao montar a requisição (ex: problema de rede antes de enviar)
    return Promise.reject(error)
  }
)

// ─── Interceptor de Resposta (Response) ───────────────────────
//
// Executado APÓS cada resposta recebida do backend.
// Centraliza o tratamento de erros HTTP comuns.
//
api.interceptors.response.use(
  // Resposta com sucesso (2xx) — retorna normalmente
  (response) => response,

  // Resposta com erro (4xx, 5xx)
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      // Token expirado ou inválido — limpa a sessão e redireciona para login.
      // Usamos window.location (e não navigate()) de propósito: este arquivo fica
      // fora da árvore do React, e o recarregamento zera o estado do AuthContext.
      localStorage.removeItem('token')
      if (!error.config?.suppressAuthRedirect && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }

    if (status === 403) {
      // Usuário autenticado mas sem permissão para o recurso acessado
      console.warn('[API] Acesso negado — permissão insuficiente.')
    }

    if (status >= 500) {
      // Erro interno do servidor
      console.error('[API] Erro no servidor:', error.response?.data)
    }

    // Propaga o erro para o service/componente que fez a chamada
    return Promise.reject(error)
  }
)

export default api