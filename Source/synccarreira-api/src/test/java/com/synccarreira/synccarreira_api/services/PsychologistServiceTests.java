package com.synccarreira.synccarreira_api.services;

import com.synccarreira.synccarreira_api.dto.PsychologistDTO;
import com.synccarreira.synccarreira_api.dto.PsychologistInsertDTO;
import com.synccarreira.synccarreira_api.entities.Institution;
import com.synccarreira.synccarreira_api.entities.Psychologist;
import com.synccarreira.synccarreira_api.entities.Role;
import com.synccarreira.synccarreira_api.repositories.InstitutionRepository;
import com.synccarreira.synccarreira_api.repositories.PsychologistRepository;
import com.synccarreira.synccarreira_api.repositories.RoleRepository;
import com.synccarreira.synccarreira_api.services.exceptions.ConflictException;
import com.synccarreira.synccarreira_api.services.exceptions.ResourceNotFoundException;
import com.synccarreira.synccarreira_api.tests.InstitutionFactory;
import com.synccarreira.synccarreira_api.tests.PsychologistFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class PsychologistServiceTests {

    @InjectMocks
    private PsychologistService service;

    @Mock
    private PsychologistRepository psychologistRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordRecoverService passwordRecoverService;

    @Mock
    private InstitutionRepository institutionRepository;

    private Long existingPsychologistId, nonExistingPsychologistId, psychologistRoleId, existingInstitutionId;
    private Psychologist psychologist, psychologist1, expiredContractPsychologist;
    private Institution institution;
    private Role psychologistRole;
    private PsychologistInsertDTO psychologistInsertDTO;
    private List<Psychologist> psychologistList;

    @BeforeEach
    void setUp() {
        existingPsychologistId = 1L;
        nonExistingPsychologistId = 100L;
        psychologistRoleId = 3L;
        existingInstitutionId = 1L;

        psychologist = PsychologistFactory.createPsychologist();
        psychologist1 = PsychologistFactory.createPsychologist();
        expiredContractPsychologist = PsychologistFactory.createExpiredContractPsychologist();
        psychologistRole = PsychologistFactory.createPsychologistRole();
        psychologistInsertDTO = PsychologistFactory.createPsychologistInsertDTO();
        institution = InstitutionFactory.createInstitution();
        psychologist1.setId(2L);
        psychologist1.setName("Mariana Alves");

        psychologistList = new ArrayList<>();
        psychologistList.add(psychologist);
        psychologistList.add(psychologist1);
    }

    @Test
    void findAllShouldReturnPsychologistDTOList() {
        Mockito.when(psychologistRepository.findAll()).thenReturn(psychologistList);

        List<PsychologistDTO> result = service.findAll();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(psychologist.getId(), result.getFirst().id());
        Assertions.assertEquals(psychologist.getName(), result.getFirst().name());
        Assertions.assertEquals(psychologist.getCrp(), result.getFirst().crp());
        Assertions.assertEquals(psychologist1.getId(), result.getLast().id());
        Assertions.assertEquals(psychologist1.getName(), result.getLast().name());
        Assertions.assertEquals(psychologist1.getCrp(), result.getLast().crp());
    }

    @Test
    void findByIdShouldReturnPsychologistDTOWhenIdExists() {
        Mockito.when(psychologistRepository.findById(existingPsychologistId)).thenReturn(Optional.of(psychologist));

        PsychologistDTO result = service.findById(existingPsychologistId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(existingPsychologistId, result.id());
        Assertions.assertEquals(psychologist.getName(), result.name());
        Assertions.assertEquals(psychologist.getEmail(), result.email());
        Assertions.assertEquals(psychologist.getCrp(), result.crp());
        Assertions.assertEquals(psychologist.getContractExpirationDate(), result.contractExpirationDate());
        Assertions.assertTrue(result.isContractValid());
    }

    @Test
    void findByIdShouldReturnResourceNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(psychologistRepository.findById(nonExistingPsychologistId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.findById(nonExistingPsychologistId);
        });
    }

    @Test
    void createShouldReturnPsychologistDTOWhenNameAndCrpDoNotExist() {
        Mockito.when(psychologistRepository.existsByNameAndCrp(psychologistInsertDTO.getName(), psychologistInsertDTO.getCrp())).thenReturn(false);
        Mockito.when(roleRepository.findById(psychologistRoleId)).thenReturn(Optional.of(psychologistRole));
        Mockito.when(psychologistRepository.save(any())).thenReturn(psychologist);
        Mockito.when(institutionRepository.findById(existingInstitutionId)).thenReturn(Optional.of(institution));

        PsychologistDTO result = service.create(psychologistInsertDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(psychologist.getId(), result.id());
        Assertions.assertEquals(psychologist.getName(), result.name());
        Assertions.assertEquals(psychologist.getEmail(), result.email());
        Assertions.assertEquals(psychologist.getCrp(), result.crp());
        Assertions.assertEquals(1, result.roles().size());
        Mockito.verify(passwordRecoverService).sendFirstAccessEmail(psychologistInsertDTO.getName(), psychologistInsertDTO.getEmail());
    }

    @Test
    void createShouldReturnConflictExceptionWhenNameAndCrpAlreadyExist() {
        Mockito.when(psychologistRepository.existsByNameAndCrp(psychologistInsertDTO.getName(), psychologistInsertDTO.getCrp())).thenReturn(true);

        Assertions.assertThrows(ConflictException.class, () -> {
            service.create(psychologistInsertDTO);
        });

        Mockito.verify(psychologistRepository, Mockito.never()).save(any());
        Mockito.verify(passwordRecoverService, Mockito.never()).sendFirstAccessEmail(any(), any());
    }

    @Test
    void updateShouldReturnPsychologistDTOWhenIdExists() {
        Mockito.when(psychologistRepository.findById(existingPsychologistId)).thenReturn(Optional.of(psychologist));
        Mockito.when(roleRepository.findById(psychologistRoleId)).thenReturn(Optional.of(psychologistRole));
        Mockito.when(psychologistRepository.save(any())).thenReturn(psychologist);
        Mockito.when(institutionRepository.findById(existingInstitutionId)).thenReturn(Optional.of(institution));

        PsychologistDTO result = service.update(existingPsychologistId, psychologistInsertDTO);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(existingPsychologistId, result.id());
        Assertions.assertEquals(psychologistInsertDTO.getName(), result.name());
        Assertions.assertEquals(psychologistInsertDTO.getEmail(), result.email());
        Assertions.assertEquals(psychologistInsertDTO.getCrp(), result.crp());
        Assertions.assertEquals(psychologistInsertDTO.getContractExpirationDate(), result.contractExpirationDate());
        Assertions.assertEquals(1, result.roles().size());
    }

    @Test
    void updateShouldReturnResourceNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(psychologistRepository.findById(nonExistingPsychologistId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.update(nonExistingPsychologistId, psychologistInsertDTO);
        });
    }

    @Test
    void deleteShouldDoNothingWhenIdExists() {
        Mockito.when(psychologistRepository.existsById(existingPsychologistId)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> {
            service.delete(existingPsychologistId);
        });

        Mockito.verify(psychologistRepository).deleteById(existingPsychologistId);
    }

    @Test
    void deleteShouldReturnEntityNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(psychologistRepository.existsById(nonExistingPsychologistId)).thenReturn(false);

        Assertions.assertThrows(EntityNotFoundException.class, () -> {
            service.delete(nonExistingPsychologistId);
        });

        Mockito.verify(psychologistRepository, Mockito.never()).deleteById(any());
    }

    @Test
    void validateIfContractIsActiveShouldDoNothingWhenContractIsValid() {
        Mockito.when(psychologistRepository.findById(existingPsychologistId)).thenReturn(Optional.of(psychologist));

        Assertions.assertDoesNotThrow(() -> {
            service.validateIfContractIsActive(existingPsychologistId);
        });
    }

    @Test
    void validateIfContractIsActiveShouldReturnIllegalStateExceptionWhenContractIsExpired() {
        Mockito.when(psychologistRepository.findById(existingPsychologistId)).thenReturn(Optional.of(expiredContractPsychologist));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            service.validateIfContractIsActive(existingPsychologistId);
        });
    }

    @Test
    void validateIfContractIsActiveShouldReturnResourceNotFoundExceptionWhenIdDoesNotExist() {
        Mockito.when(psychologistRepository.findById(nonExistingPsychologistId)).thenReturn(Optional.empty());

        Assertions.assertThrows(ResourceNotFoundException.class, () -> {
            service.validateIfContractIsActive(nonExistingPsychologistId);
        });
    }
}
