package app.sharehub.service;

import app.sharehub.domain.AuditLogEntity;
import app.sharehub.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogMapper mapper;
    public AuditService(AuditLogMapper mapper) { this.mapper = mapper; }
    public void record(Long actorId, String action, String targetType, Long targetId) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActorId(actorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        mapper.insert(log);
    }
}
