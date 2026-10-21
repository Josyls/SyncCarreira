/**
 * @file ReportsPanel.jsx
 * @description RF-07 — Relatório geral da turma para download (.xlsx).
 *
 * As turmas vêm dos próprios alunos do painel (schoolClassId/schoolClassName).
 * O relatório individual de cada aluno fica no botão "Relatório" da tabela.
 */

import { useMemo, useState } from 'react'

/**
 * @param {Object}   props
 * @param {Array}    props.students   - alunos do painel
 * @param {Function} props.onDownload - async (classId, className) => void
 */
export default function ReportsPanel({ students, onDownload }) {
  const classes = useMemo(() => {
    const map = new Map()
    students.forEach(student => {
      if (!student.schoolClassId) return
      const current = map.get(student.schoolClassId) ?? { id: student.schoolClassId, name: student.schoolClassName, total: 0 }
      current.total += 1
      map.set(student.schoolClassId, current)
    })
    return [...map.values()].sort((first, second) => (first.name ?? '').localeCompare(second.name ?? '', 'pt-BR'))
  }, [students])

  const [selectedId, setSelectedId] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(null) // { tone: 'ok'|'error', text }

  const selected = classes.find(item => String(item.id) === String(selectedId)) ?? classes[0]

  async function handleDownload() {
    if (!selected) return
    setBusy(true)
    setMessage(null)
    try {
      const fileName = await onDownload(selected.id, selected.name)
      setMessage({ tone: 'ok', text: `Download iniciado: ${fileName}` })
    } catch (error) {
      setMessage({ tone: 'error', text: error.message })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="pd-panel pd-reports" aria-labelledby="pd-reports-title">
      <div className="pd-section-heading">
        <div>
          <h2 id="pd-reports-title">Relatórios</h2>
          <p>Relatório geral da turma com a síntese e o status de cada aluno. Os individuais ficam na tabela de alunos.</p>
        </div>
      </div>

      {classes.length === 0 ? (
        <p className="pd-empty-inline">Nenhum aluno está vinculado a uma turma ainda.</p>
      ) : (
        <div className="pd-reports__row">
          <label className="pd-reports__select">
            <span className="pd-sr-only">Turma do relatório</span>
            <select value={selected?.id ?? ''} onChange={event => setSelectedId(event.target.value)}>
              {classes.map(item => (
                <option key={item.id} value={item.id}>
                  {item.name} · {item.total} aluno(s)
                </option>
              ))}
            </select>
          </label>
          <button type="button" className="ag-btn ag-btn--primary" onClick={handleDownload} disabled={busy}>
            {busy ? 'Gerando relatório…' : 'Baixar relatório da turma (.xlsx)'}
          </button>
        </div>
      )}

      {message && (
        <p className={`pd-reports__msg pd-reports__msg--${message.tone}`} role={message.tone === 'error' ? 'alert' : undefined}>
          {message.text}
        </p>
      )}
    </section>
  )
}