package com.certificateservice.repository;

import com.certificateservice.model.ProgrammeDesignAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgrammeDesignAssignmentRepository extends JpaRepository<ProgrammeDesignAssignment, UUID> {

    List<ProgrammeDesignAssignment> findByProgrammeIdOrderByEffectiveFromDesc(UUID programmeId);

    Optional<ProgrammeDesignAssignment> findFirstByProgrammeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            UUID programmeId, Instant effectiveFrom);

    boolean existsByDesignId(UUID designId);
}
