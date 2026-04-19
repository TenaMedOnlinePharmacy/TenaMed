package com.TenaMed.audit.service;

import com.TenaMed.audit.entity.AuditLog;

public interface AuditLogService {

    AuditLog write(AuditLogWriteRequest request);
}
