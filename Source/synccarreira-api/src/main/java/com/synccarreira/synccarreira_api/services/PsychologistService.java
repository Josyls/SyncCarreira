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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PsychologistService {

    private final PsychologistRepository psychologistRepository;

    private final RoleRepository roleRepository;

    private final PasswordRecoverService passwordRecoverService;

    private final InstitutionRepository institutionRepository;

    public PsychologistService(
            final PsychologistRepository psychologistRepository,
            final RoleRepository roleRepository,
            final PasswordRecoverService passwordRecoverService,
            final InstitutionRepository institutionRepository) {
        this.psychologistRepository = psychologistRepository;
        this.roleRepository = roleRepository;
        this.passwordRecoverService = passwordRecoverService;
        this.institutionRepository = institutionRepository;
    }

    @Transactional(readOnly = true)
    public List<PsychologistDTO> findAll() {
        return psychologistRepository.findAll()
                .stream()
                .map(PsychologistDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PsychologistDTO findById(Long id) {
        Psychologist psychologist = psychologistRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado na base de dados."));
        return new PsychologistDTO(psychologist);
    }

    @Transactional
    public PsychologistDTO create(PsychologistInsertDTO dto) {
        if (psychologistRepository.existsByNameAndCrp(dto.getName(), dto.getCrp())) {
            throw new ConflictException(
                    "Já existe um(a) psicólogo(a) cadastrado(a) com o nome '" + dto.getName() +
                    "' e CRP '" + dto.getCrp() + "'.");
        }
        Psychologist psychologist = new Psychologist();
        copyDtoToEntity(dto, psychologist);
        psychologist = psychologistRepository.save(psychologist);

        passwordRecoverService.sendFirstAccessEmail(dto.getName(), dto.getEmail());

        return new PsychologistDTO(psychologist);
    }

    @Transactional
    public PsychologistDTO update(Long id, PsychologistInsertDTO dto) {
        Psychologist psychologist = psychologistRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
        copyDtoToEntity(dto, psychologist);
        psychologist = psychologistRepository.save(psychologist);
        return new PsychologistDTO(psychologist);
    }

    @Transactional
    public void delete(Long id) {
        if (!psychologistRepository.existsById(id)) {
            throw new EntityNotFoundException("Psicóloga não encontrada. ID: " + id);
        }
        psychologistRepository.deleteById(id);
    }

    public void validateIfContractIsActive(Long id) {
        Psychologist psychologist = psychologistRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Resource not found."));
        if (!psychologist.isContractValid()) {
            throw new IllegalStateException(
                    "Operação bloqueada: o contrato da psicóloga '" +
                            psychologist.getName() + "' está vencido desde " +
                            psychologist.getContractExpirationDate() + ".");
        }
    }

    private void copyDtoToEntity(PsychologistInsertDTO dto, Psychologist entity) {
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setContractExpirationDate(dto.getContractExpirationDate());
        entity.setCrp(dto.getCrp());
        Optional<Institution> institution = institutionRepository.findById(dto.getInstitutionId());
        institution.ifPresent(entity::setInstitution);
        entity.getRoles().clear();
        Optional<Role> role = roleRepository.findById(3L);
        role.ifPresent(entity::addRole);
    }
}
