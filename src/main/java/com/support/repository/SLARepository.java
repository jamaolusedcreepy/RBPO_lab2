package com.support.repository;

import com.support.domain.SLA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SLARepository extends JpaRepository<SLA, Long> {
    Optional<SLA> findByName(String name);
    boolean existsByName(String name);
}