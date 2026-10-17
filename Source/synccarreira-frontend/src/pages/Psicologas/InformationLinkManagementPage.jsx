import { useCallback, useEffect, useState } from 'react'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import {
  createInformationLink,
  deleteInformationLink,
  getManagedInformationLinks,
} from '../../services/informationLinkService.js'
import './InformationLinkManagementPage.css'

const KNOWLEDGE_AREAS = [
  { value: 'HUMANAS', label: 'Ciências Humanas' },
  { value: 'BIOLOGICAS', label: 'Ciências Biológicas' },
  { value: 'EXATAS', label: 'Ciências Exatas' },
  { value: 'ARTES', label: 'Artes' },
]

const UNIVERSITY_ACCESS = 'UNIVERSITY_ACCESS'
const EMPTY_FORM = { topic: '', url: '', category: '' }

function categoryLabel(knowledgeArea) {
  if (!knowledgeArea) return 'Acesso à universidade'
  return KNOWLEDGE_AREAS.find(area => area.value === knowledgeArea)?.label ?? knowledgeArea
}

function requestError(error, fallback) {
  if (error.response?.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.'
  if (error.response?.status === 403) return 'Seu perfil não tem permissão para gerenciar os links.'
  if (error.response?.status === 422) {
    return error.response.data?.message || 'Confira o tópico e informe uma URL válida iniciada por http:// ou https://.'
  }
  return error.response?.data?.message || fallback
}

export default function InformationLinkManagementPage() {
  const [links, setLinks] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const loadLinks = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setLinks(await getManagedInformationLinks())
    } catch (loadError) {
      console.error('Erro ao carregar links de informação:', loadError)
      setError(requestError(loadError, 'Não foi possível carregar os links. Tente novamente.'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void loadLinks() }, [loadLinks])

  function updateForm(field, value) {
    setForm(current => ({ ...current, [field]: value }))
    setError('')
    setSuccess('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError('')
    setSuccess('')

    const url = form.url.trim()
    try {
      const parsedUrl = new URL(url)
      if (!['http:', 'https:'].includes(parsedUrl.protocol)) throw new Error('URL inválida')

      const created = await createInformationLink({
        topic: form.topic.trim(),
        url,
        knowledgeArea: form.category === UNIVERSITY_ACCESS ? null : form.category,
      })
      setLinks(current => [...current, created].sort((a, b) => a.topic.localeCompare(b.topic, 'pt-BR')))
      setForm(EMPTY_FORM)
      setSuccess('Link cadastrado e disponível na trilha Informação.')
    } catch (submitError) {
      console.error('Erro ao cadastrar link de informação:', submitError)
      setError(submitError.response
        ? requestError(submitError, 'Não foi possível cadastrar o link.')
        : 'Informe uma URL válida iniciada por http:// ou https://.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(link) {
    if (!window.confirm(`Excluir o link “${link.topic}”?`)) return
    setDeletingId(link.id)
    setError('')
    setSuccess('')
    try {
      await deleteInformationLink(link.id)
      setLinks(current => current.filter(item => item.id !== link.id))
      setSuccess('Link removido da trilha Informação.')
    } catch (deleteError) {
      console.error('Erro ao excluir link de informação:', deleteError)
      setError(requestError(deleteError, 'Não foi possível excluir o link.'))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="information-links-page">
      <AppHeader />
      <main className="information-links-main">
        <header className="information-links-heading">
          <div>
            <p className="information-links-eyebrow">Trilha de Informação</p>
            <h1>Curadoria de links</h1>
            <p className="information-links-description">
              Cadastre fontes confiáveis para apoiar os alunos em pesquisas sobre carreiras e acesso à universidade.
            </p>
          </div>
          <span className="information-links-count">{links.length} {links.length === 1 ? 'link' : 'links'}</span>
        </header>

        {error && <p className="information-links-alert information-links-alert--error" role="alert">{error}</p>}
        {success && <p className="information-links-alert information-links-alert--success" role="status">{success}</p>}

        <section className="information-links-panel" aria-labelledby="information-links-form-title">
          <div className="information-links-panel__heading">
            <span className="information-links-panel__icon" aria-hidden="true">+</span>
            <div>
              <h2 id="information-links-form-title">Adicionar fonte</h2>
              <p>Os links são materiais complementares e não são obrigatórios para concluir a trilha.</p>
            </div>
          </div>
          <form className="information-links-form" onSubmit={handleSubmit}>
            <label>
              Tópico ou nome da fonte
              <input
                value={form.topic}
                onChange={event => updateForm('topic', event.target.value)}
                maxLength={255}
                placeholder="Ex.: Cursos de Psicologia"
                required
              />
            </label>
            <label>
              Link
              <input
                type="url"
                value={form.url}
                onChange={event => updateForm('url', event.target.value)}
                maxLength={500}
                placeholder="https://exemplo.org.br"
                required
              />
            </label>
            <label>
              Exibir como
              <select
                value={form.category}
                onChange={event => updateForm('category', event.target.value)}
                required
              >
                <option value="" disabled>Selecione uma categoria</option>
                <optgroup label="Links de carreira">
                  {KNOWLEDGE_AREAS.map(area => <option key={area.value} value={area.value}>{area.label}</option>)}
                </optgroup>
                <optgroup label="Acesso à universidade">
                  <option value={UNIVERSITY_ACCESS}>ENEM, ProUni, cotas e ingresso</option>
                </optgroup>
              </select>
            </label>
            <button className="information-links-primary" type="submit" disabled={saving}>
              {saving ? 'Salvando…' : 'Cadastrar link'}
            </button>
          </form>
        </section>

        <section className="information-links-list" aria-labelledby="information-links-list-title">
          <div className="information-links-list__heading">
            <div>
              <h2 id="information-links-list-title">Fontes cadastradas</h2>
              <p>A psicóloga pode remover links desatualizados. Para alterar um link, exclua-o e cadastre a versão atualizada.</p>
            </div>
            <button className="information-links-refresh" type="button" onClick={loadLinks} disabled={loading}>
              Atualizar lista
            </button>
          </div>

          {loading ? (
            <div className="information-links-empty">Carregando fontes…</div>
          ) : links.length === 0 ? (
            <div className="information-links-empty">Nenhum link cadastrado. Adicione uma fonte para começar a curadoria.</div>
          ) : (
            <div className="information-links-table-wrap">
              <table className="information-links-table">
                <thead><tr><th>Tópico</th><th>Categoria</th><th>Endereço</th><th><span className="sr-only">Ações</span></th></tr></thead>
                <tbody>
                  {links.map(link => (
                    <tr key={link.id}>
                      <td><strong>{link.topic}</strong></td>
                      <td><span className="information-links-category">{categoryLabel(link.knowledgeArea)}</span></td>
                      <td><a href={link.url} target="_blank" rel="noreferrer">Abrir fonte ↗</a></td>
                      <td className="information-links-actions">
                        <button type="button" onClick={() => handleDelete(link)} disabled={deletingId === link.id}>
                          {deletingId === link.id ? 'Removendo…' : 'Remover'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  )
}
