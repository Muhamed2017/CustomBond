package com.custombond.entity;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CB_General_Log")
@Data
public class CBGeneralLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "RequestId", nullable = false)
    private UUID requestId;

    @Column(name = "APIName")
    private String apiName;

    @CreationTimestamp
    @Column(name = "CreationDateTime")
    private LocalDateTime creationDateTime;

    @UpdateTimestamp
    @Column(name = "ResponseDateTime")
    private LocalDateTime responseDateTime;

    @Column(name = "ParentRequestId")
    private UUID parentRequestId;

    @Column(name = "RequestBody", columnDefinition = "nvarchar(max)")
    private String requestBody;

    @Column(name = "VendorRequestBody", columnDefinition = "nvarchar(max)")
    private String vendorRequestBody;

    @Column(name = "ResponseBody", columnDefinition = "nvarchar(max)")
    private String responseBody;

    @Column(name = "URL", columnDefinition = "nvarchar(max)")
    private String url;

    @Column(name = "MethodType", columnDefinition = "nvarchar(max)")
    private String methodType;

    @Column(name = "VendorId", columnDefinition = "nvarchar(max)")
    private String vendorId;

    @Column(name = "CallType", columnDefinition = "nvarchar(max)")
    @Enumerated(EnumType.STRING)
    private Enums.CallType callType;

    @Column(name = "VendorRequestId", nullable = false)
    private String vendorRequestId;

    @PrePersist
    public void prePersist() {
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
    }
}