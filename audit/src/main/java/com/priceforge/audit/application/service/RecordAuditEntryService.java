package com.priceforge.audit.application.service;

import com.priceforge.audit.application.dto.RecordAuditEntryCommand;
import com.priceforge.audit.application.port.in.RecordAuditEntryUseCase;
import com.priceforge.audit.application.port.out.AuditEntryRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordAuditEntryService implements RecordAuditEntryUseCase {

    private final AuditEntryRepositoryPort auditEntryRepository;

    public RecordAuditEntryService(AuditEntryRepositoryPort auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    @Override
    @Transactional
    public void execute(RecordAuditEntryCommand command) {
        auditEntryRepository.saveIfAbsent(command.entry());
    }
}
