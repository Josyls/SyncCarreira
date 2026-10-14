/**
 * @file AgendamentosPage.jsx
 * @description Rota /agendamentos. Mostra a visão certa conforme o perfil:
 *  - Psicóloga/orientadora → RF-08 (criar, editar, cancelar sessões + feedback)
 *  - Aluno                 → RF-16 (ver agendamentos e feedbacks)
 */

import { useAuth } from '../../context/AuthContext.jsx'
import AppHeader from '../../components/AppHeader/AppHeader.jsx'
import { isPsychologist } from '../../utils/roles'
import PsicologaAgendamentos from './PsicologaAgendamentos.jsx'
import AlunoAgendamentos from './AlunoAgendamentos.jsx'
import './Agendamentos.css'

export default function AgendamentosPage() {
  const { user } = useAuth()
  const showPsychologistView = isPsychologist(user)

  return (
    <div className="ag-root">
      <AppHeader />
      <main className="ag-main">
        <div className={`ag-content${showPsychologistView ? ' ag-content--wide' : ''}`}>
          {showPsychologistView ? <PsicologaAgendamentos /> : <AlunoAgendamentos />}
        </div>
      </main>
    </div>
  )
}