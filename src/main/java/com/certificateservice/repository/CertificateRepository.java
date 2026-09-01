package com.certificateservice.repository;

import com.certificateservice.model.Certificate;
import com.certificateservice.model.CertificateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Page<Certificate> findByPersonEmailIgnoreCase(String personEmail, Pageable pageable);

    Page<Certificate> findByPersonEmailIgnoreCaseAndStatus(String personEmail, CertificateStatus status, Pageable pageable);

    Optional<Certificate> findByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
            UUID programmeId, String personEmail, CertificateStatus status);

    boolean existsByProgrammeIdAndPersonEmailIgnoreCaseAndStatus(
            UUID programmeId, String personEmail, CertificateStatus status);
}
