package com.certificateservice.repository;

import com.certificateservice.model.Design;
import com.certificateservice.model.DesignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DesignRepository extends JpaRepository<Design, UUID> {
    List<Design> findAllByOrderByCreatedAtDesc();
    List<Design> findByStatusOrderByCreatedAtDesc(DesignStatus status);
}
