package com.synccarreira.synccarreira_api.dto.feedback;

import com.synccarreira.synccarreira_api.entities.Appointment;
import com.synccarreira.synccarreira_api.entities.Feedback;
import com.synccarreira.synccarreira_api.entities.enums.FeedbackType;

import java.time.Instant;
import java.time.LocalDateTime;

public record FeedbackDTO(
        Long id,
        FeedbackType type,
        String content,
        Long studentId,
        String studentName,
        Long psychologistId,
        String psychologistName,
        Integer journeyNumber,
        Long appointmentId,
        String appointmentTitle,
        LocalDateTime appointmentDateTime,
        Instant createdAt,
        Instant readAt,
        boolean read
) {

    public static final int MAX_CONTENT_LENGTH = 5000;

    public FeedbackDTO(Feedback feedback) {
        this(
                feedback.getId(),
                feedback.getType(),
                feedback.getContent(),
                feedback.getStudent().getId(),
                feedback.getStudent().getName(),
                feedback.getPsychologist().getId(),
                feedback.getPsychologist().getName(),
                feedback.getJourneyNumber(),
                appointmentId(feedback.getAppointment()),
                feedback.getAppointment() != null ? feedback.getAppointment().getTitle() : null,
                feedback.getAppointment() != null ? feedback.getAppointment().getDateTime() : null,
                feedback.getCreatedAt(),
                feedback.getReadAt(),
                feedback.getReadAt() != null
        );
    }

    private static Long appointmentId(Appointment appointment) {
        return appointment != null ? appointment.getId() : null;
    }
}
