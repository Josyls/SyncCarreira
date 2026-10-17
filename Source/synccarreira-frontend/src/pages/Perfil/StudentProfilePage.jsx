import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { getMyStudentProfile, saveMyStudentAffinities } from '../../services/studentProfileService.js'
import './StudentProfilePage.css'

const AREAS = [
  {
    key: 'humanities',
    title: 'Humanas',
    prompt: 'Quanto você se interessa por sociedade, história, comunicação e cultura?',
  },
  {
    key: 'exactSciences',
    title: 'Exatas',
    prompt: 'Quanto você se interessa por matemática, lógica, tecnologia e resolução de problemas?',
  },
  {
    key: 'biologicalSciences',
    title: 'Biológicas',
    prompt: 'Quanto você se interessa por saúde, natureza e pelo funcionamento dos seres vivos?',
  },
  {
    key: 'arts',
    title: 'Artes',
    prompt: 'Quanto você se interessa por criação, música, imagem, design e expressão artística?',
  },
]

const EMPTY_AFFINITIES = {
  humanities: null,
  exactSciences: null,
  biologicalSciences: null,
  arts: null,
}

function errorMessage(error) {
  if (error.response?.status === 422) {
    return 'Revise suas respostas: cada área precisa receber uma nota de 0 a 5.'
  }
  if (!error.response) return 'Não foi possível conectar ao serviço. Confira se o backend está disponível e tente novamente.'
  return 'Não foi possível salvar seu perfil agora. Tente novamente.'
}

export default function StudentProfilePage() {
  const navigate = useNavigate()
  const [profile, setProfile] = useState(null)
  const [affinities, setAffinities] = useState(EMPTY_AFFINITIES)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let active = true
    getMyStudentProfile()
      .then(data => {
        if (!active) return
        setProfile(data)
        setAffinities({ ...EMPTY_AFFINITIES, ...data.affinities })
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar seu perfil. Atualize a página ou tente novamente.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [])

  const complete = AREAS.every(({ key }) => Number.isInteger(affinities[key])
    && affinities[key] >= 0 && affinities[key] <= 5)

  function selectAffinity(key, value) {
    setAffinities(current => ({ ...current, [key]: value }))
    setError('')
    setSuccess('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!complete || saving) return

    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const updated = await saveMyStudentAffinities(affinities)
      setProfile(updated)
      setAffinities({ ...EMPTY_AFFINITIES, ...updated.affinities })

      if (!profile?.profileCompleted) {
        navigate('/home', { replace: true, state: { profilePersonalized: true } })
        return
      }
      setSuccess('Seus interesses foram atualizados. Você pode alterá-los novamente quando quiser.')
    } catch (saveError) {
      setError(errorMessage(saveError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="sp-page">
      <AppHeader />
      <main className="sp-main">
        <Link className="sp-back" to="/home">← Voltar para o início</Link>

        <section className="sp-card">
          <span className="sp-eyebrow">PERSONALIZAÇÃO DA JORNADA</span>
          <h1>{profile?.profileCompleted ? 'Seus interesses' : 'Vamos personalizar sua jornada'}</h1>
          <p className="sp-intro">
            Suas respostas ajudam a contextualizar o resultado da jornada. Você pode voltar aqui e alterá-las sempre que quiser.
          </p>

          {loading ? (
            <div className="sp-state" role="status">Carregando seu perfil…</div>
          ) : (
            <form onSubmit={handleSubmit}>
              <fieldset className="sp-scale-legend" disabled>
                <legend>Escolha uma nota para cada área</legend>
                <div className="sp-scale-ends"><span>0 · Não me interessa</span><span>5 · Tenho muito interesse</span></div>
              </fieldset>

              <div className="sp-areas">
                {AREAS.map(({ key, title, prompt }, index) => (
                  <fieldset className="sp-area" key={key}>
                    <legend><span>{String(index + 1).padStart(2, '0')}</span>{title}</legend>
                    <p>{prompt}</p>
                    <div className="sp-rating" role="group" aria-label={`Interesse em ${title}`}>
                      {[0, 1, 2, 3, 4, 5].map(value => (
                        <button
                          className={affinities[key] === value ? 'is-selected' : ''}
                          key={value}
                          type="button"
                          aria-pressed={affinities[key] === value}
                          aria-label={`${value} de 5`}
                          onClick={() => selectAffinity(key, value)}
                        >
                          {value}
                        </button>
                      ))}
                    </div>
                  </fieldset>
                ))}
              </div>

              {error && <p className="sp-message sp-message--error" role="alert">{error}</p>}
              {success && <p className="sp-message sp-message--success" role="status">{success}</p>}

              <div className="sp-form-footer">
                <p>Essas respostas não definem sua carreira. Elas são apenas um ponto de partida e podem mudar com o tempo.</p>
                <button className="sp-submit" type="submit" disabled={!complete || saving}>
                  {saving ? 'Salvando…' : profile?.profileCompleted ? 'Salvar alterações' : 'Salvar e começar a jornada'}
                </button>
              </div>
            </form>
          )}
        </section>
      </main>
    </div>
  )
}
