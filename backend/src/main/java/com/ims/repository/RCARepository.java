package com.ims.repository;

import com.ims.model.RCA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RCARepository extends JpaRepository<RCA, Long> {

    // Find RCA by Work Item ID
    Optional<RCA> findByWorkItemId(Long workItemId);

    boolean existsByWorkItemId(Long workItemId);

    void deleteByWorkItemId(Long workItemId);
}