package com.custombond.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CB_Documents")
@Data
public class CBDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "RequestId", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "DocumentName", nullable = false, length = 255)
    private String documentName;

    @Column(name = "ContentType", length = 100)
    private String contentType;

    @Column(name = "DocumentSize")
    private Long documentSize;

    @Lob
    @Column(name = "DocumentData", nullable = false, columnDefinition = "VARBINARY(MAX)")
    private byte[] documentData;

    @CreationTimestamp
    @Column(name = "UploadDate", updatable = false)
    private LocalDateTime uploadDate;

    @Column(name = "DocumentKeyInDXC")
    private Integer documentKey;

    @PrePersist
    public void prePersist() {
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
    }
}