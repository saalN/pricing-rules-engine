package com.priceforge.audit.infrastructure.out.postgres.repository;

import com.priceforge.audit.infrastructure.out.postgres.entity.AuditEntryDocument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataAuditEntryPostgresRepository
    extends JpaRepository<AuditEntryDocument, UUID>, JpaSpecificationExecutor<AuditEntryDocument> {
}
