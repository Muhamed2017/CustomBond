package com.custombond.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.custombond.entity.CBGeneralLog;
import java.util.List;
import java.util.UUID;


@Repository
public interface CBGeneralLogRepository extends JpaRepository<CBGeneralLog, Integer> {
    List<CBGeneralLog> findByRequestId(UUID requestId);
}