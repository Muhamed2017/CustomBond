package com.custombond.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.custombond.entity.CBDocument;
import java.util.List;
import java.util.UUID;


public interface CBDocumentRepository extends JpaRepository<CBDocument, Integer> {
    List<CBDocument> findByRequestId(UUID requestId);
}