package com.researchassistant.audit;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class AuditEventService {
    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;

    public AuditEventService(AuditEventRepository auditEventRepository, UserRepository userRepository) {
        this.auditEventRepository = auditEventRepository;
        this.userRepository = userRepository;
    }

    public AuditEvent record(UUID actorId, String actorType, Workspace workspace, ResearchProject project,
                             AuditEventType type, String targetType, UUID targetId, String metadataJson) {
        AuditEvent event = new AuditEvent();
        User actor = actorId == null ? null : userRepository.findById(actorId).orElse(null);
        event.setActor(actor);
        event.setActorType(actorType == null ? "SYSTEM" : actorType);
        event.setWorkspace(workspace);
        event.setProject(project);
        event.setType(type);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setMetadataJson(sanitize(metadataJson));
        return auditEventRepository.save(event);
    }

    private String sanitize(String value) {
        if (value == null) return null;
        String lower = value.toLowerCase();
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")
                || lower.contains("key") || lower.contains("sk_" + "test_") || lower.contains("pk_" + "test_")
                || lower.contains("sk_" + "live_") || lower.contains("pk_" + "live_")) {
            return "{\"redacted\":true}";
        }
        return value;
    }
}
