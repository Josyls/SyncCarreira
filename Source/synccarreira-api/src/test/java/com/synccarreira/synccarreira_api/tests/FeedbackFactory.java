package com.synccarreira.synccarreira_api.tests;

import com.synccarreira.synccarreira_api.dto.feedback.FeedbackDTO;
import com.synccarreira.synccarreira_api.dto.feedback.JourneyFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.SessionFeedbackInsertDTO;
import com.synccarreira.synccarreira_api.dto.feedback.StudentFeedbackStatusDTO;
import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Feedback;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.Student;
import com.synccarreira.synccarreira_api.entities.enums.FeedbackType;

import java.time.Instant;

public class FeedbackFactory {

    public static final String CONTENT = "Parabéns por concluir a jornada! Vale explorar mais os cursos da área de Artes.";
    public static final Instant CREATED_AT = Instant.parse("2026-10-05T14:00:00Z");

    public static Feedback createJourneyFeedback(Student student, Psychologist psychologist, Integer journeyNumber) {
        Feedback feedback = new Feedback();
        feedback.setId(1L);
        feedback.setType(FeedbackType.JORNADA);
        feedback.setStudent(student);
        feedback.setPsychologist(psychologist);
        feedback.setJourneyNumber(journeyNumber);
        feedback.setContent(CONTENT);
        feedback.setCreatedAt(CREATED_AT);
        return feedback;
    }

    public static Feedback createSessionFeedback(Student student, Psychologist psychologist, Appointment appointment) {
        Feedback feedback = new Feedback();
        feedback.setId(2L);
        feedback.setType(FeedbackType.SESSAO);
        feedback.setStudent(student);
        feedback.setPsychologist(psychologist);
        feedback.setAppointment(appointment);
        feedback.setContent("Gostei de como você falou sobre seus planos na sessão.");
        feedback.setCreatedAt(CREATED_AT);
        return feedback;
    }

    public static JourneyFeedbackInsertDTO createJourneyFeedbackInsertDTO(Long studentId) {
        return new JourneyFeedbackInsertDTO(studentId, null, "  " + CONTENT + "  ");
    }

    public static SessionFeedbackInsertDTO createSessionFeedbackInsertDTO(Long appointmentId, Long studentId) {
        return new SessionFeedbackInsertDTO(appointmentId, studentId, "Gostei de como você falou sobre seus planos na sessão.");
    }

    public static FeedbackDTO createJourneyFeedbackDTO() {
        return new FeedbackDTO(createJourneyFeedback(StudentFactory.createStudent(), PsychologistFactory.createPsychologist(), 1));
    }

    public static StudentFeedbackStatusDTO createStudentFeedbackStatusDTO() {
        return new StudentFeedbackStatusDTO(1L, "Ana Souza", "3º A", 1, 1, true, 2L);
    }
}
