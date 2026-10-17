import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getStudentById } from "../../services/studentService";

export default function StudentDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [student, setStudent] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        setLoading(true);
        getStudentById(id)
            .then((res) => setStudent(res.data))
            .catch((err) => {
                console.error("Erro ao buscar aluno:", err);
                setError("Não foi possível carregar os dados do aluno.");
            })
            .finally(() => setLoading(false));
    }, [id]);

    if (loading) return <p>Carregando...</p>;
    if (error) return <p>{error}</p>;
    if (!student) return null;

    return (
        <div>
            <button onClick={() => navigate("/alunos")}>Voltar</button>
            <button onClick={() => navigate(`/alunos/${id}/editar`)}>Editar</button>

            <h1>{student.name}</h1>

            <p>E-mail: {student.email}</p>
            <p>Ano escolar: {student.schollarYear}</p>
            <p>Tipo de escola: {student.schoolType}</p>

            <h2>Perfis de acesso</h2>
            <ul>
                {student.roles?.map((role) => (
                    <li key={role.id}>{role.authority}</li>
                ))}
            </ul>
        </div>
    );
}