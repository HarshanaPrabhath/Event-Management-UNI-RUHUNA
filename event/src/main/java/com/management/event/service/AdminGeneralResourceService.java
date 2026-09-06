package com.management.event.service;

import com.management.event.dto.GeneralResourceResponseDto;
import com.management.event.dto.GeneralResourceUpsertRequestDto;
import com.management.event.entity.AppRole;
import com.management.event.entity.GeneralResource;
import com.management.event.entity.Role;
import com.management.event.entity.User;
import com.management.event.exception.BadRequestException;
import com.management.event.exception.ResourceNotFoundException;
import com.management.event.repository.GeneralResourceRepository;
import com.management.event.repository.RoleRepository;
import com.management.event.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGeneralResourceService {

    private final GeneralResourceRepository generalResourceRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<GeneralResourceResponseDto> listAll() {
        return generalResourceRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public GeneralResourceResponseDto create(GeneralResourceUpsertRequestDto req) {
        if (!StringUtils.hasText(req.getName())) throw new BadRequestException("name is required");
        if (req.getQuantity() == null || req.getQuantity() < 0) {
            throw new BadRequestException("quantity must be zero or more");
        }
        if (!StringUtils.hasText(req.getResponsiblePersonRegNumber())) {
            throw new BadRequestException("responsiblePersonRegNumber is required");
        }

        GeneralResource resource = new GeneralResource();
        resource.setName(req.getName().trim());
        resource.setQuantity(req.getQuantity());
        resource.setResponsiblePerson(resolveResponsiblePerson(req.getResponsiblePersonRegNumber()));
        return toDto(generalResourceRepository.save(resource));
    }

    @Transactional
    public GeneralResourceResponseDto update(Long id, GeneralResourceUpsertRequestDto req) {
        GeneralResource resource = generalResourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        if (StringUtils.hasText(req.getName())) resource.setName(req.getName().trim());
        if (req.getQuantity() != null) {
            if (req.getQuantity() < 0) throw new BadRequestException("quantity must be zero or more");
            resource.setQuantity(req.getQuantity());
        }
        if (StringUtils.hasText(req.getResponsiblePersonRegNumber())) {
            resource.setResponsiblePerson(resolveResponsiblePerson(req.getResponsiblePersonRegNumber()));
        }
        return toDto(generalResourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        if (!generalResourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource", "id", id);
        }
        generalResourceRepository.deleteById(id);
    }

    // Picking someone as a resource's responsible person is how they become a Technical Officer
    // in this system - grant the role automatically, same as assigning a place's responsible person.
    private User resolveResponsiblePerson(String regNumber) {
        User user = userRepository.findByRegNumber(regNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "regNumber", regNumber));
        if (!RoleUtil.hasRole(user, AppRole.ROLE_TO)) {
            Role toRole = roleRepository.findByRoleName(AppRole.ROLE_TO)
                    .orElseThrow(() -> new BadRequestException("ROLE_TO missing in role table"));
            user.getRoles().add(toRole);
            userRepository.save(user);
        }
        return user;
    }

    private GeneralResourceResponseDto toDto(GeneralResource r) {
        User responsible = r.getResponsiblePerson();
        return GeneralResourceResponseDto.builder()
                .id(r.getId())
                .name(r.getName())
                .quantity(r.getQuantity())
                .responsiblePersonRegNumber(responsible != null ? responsible.getRegNumber() : null)
                .responsiblePersonName(responsible != null ? responsible.getUserName() : null)
                .build();
    }
}
