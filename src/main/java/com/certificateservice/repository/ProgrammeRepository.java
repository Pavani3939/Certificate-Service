package com.certificateservice.repository;

import com.certificateservice.model.Programme;
import com.certificateservice.model.ProgrammeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {
    List<Programme> findAllByOrderByCreatedAtDesc();
    List<Programme> findByStatusOrderByCreatedAtDesc(ProgrammeStatus status);
}
