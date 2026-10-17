import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getStudentById, updateStudent } from "../../services/studentService";

export default function StudentFormPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [form, setForm] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        getStudentById(id)
            .then((res) => setForm(res.data))
            .catch((err) => {
                console.error("Erro ao carregar aluno:", err);
                setError("Não foi possível carregar os dados do aluno.");
            })
            .finally(() => setLoading(false));
    }, [id]);

    function handleChange(e) {
        const { name, value } = e.target;
        setForm((prev) => ({ ...prev, [name]: value }));
    }

    function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        setError(null);

        updateStudent(id, form)
            .then(() => navigate(`/alunos/${id}`))
            .catch((err) => {
                console.error("Erro ao salvar aluno:", err);
                setError("Não foi possível salvar as alterações.");
            })
            .finally(() => setSaving(false));
    }

    if (loading) return <p>Carregando...</p>;
    if (!form) return <p>{error}</p>;

    return (
        <div>
            <h1>Editar Aluno</h1>

            {error && <p>{error}</p>}

            <form onSubmit={handleSubmit}>
                <label>
                    Nome
                    <input name="name" value={form.name} onChange={handleChange} required />
                </label>

                <label>
                    E-mail
                    <input
                        type="email"
                        name="email"
                        value={form.email}
                        onChange={handleChange}
                        required
                    />
                </label>

                <label>
                    Ano escolar
                    <input
                        name="schollarYear"
                        value={form.schollarYear}
                        onChange={handleChange}
                    />
                </label>

                <label>
                    Tipo de escola
                    <input
                        name="schoolType"
                        value={form.schoolType}
                        onChange={handleChange}
                    />
                </label>

                <button type="submit" disabled={saving}>
                    {saving ? "Salvando..." : "Salvar"}
                </button>
                <button type="button" onClick={() => navigate(-1)}>
                    Cancelar
                </button>
            </form>
        </div>
    );
}