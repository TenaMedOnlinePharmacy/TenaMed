package com.TenaMed.manualreview.service.impl;

import com.TenaMed.manualreview.entity.ManualReviewTask;
import com.TenaMed.manualreview.entity.TaskPriority;
import com.TenaMed.manualreview.entity.TaskReason;
import com.TenaMed.manualreview.entity.TaskStatus;
import com.TenaMed.manualreview.event.DomainEventPublisher;
import com.TenaMed.manualreview.exception.ManualReviewException;
import com.TenaMed.manualreview.exception.ManualReviewTaskNotFoundException;
import com.TenaMed.manualreview.repository.ManualReviewTaskRepository;
import com.TenaMed.manualreview.service.ManualReviewService;
import com.TenaMed.manualreview.websocket.ManualReviewEventPublisher;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import com.TenaMed.verification.dto.PrescriptionItemRequestDto;
import com.TenaMed.verification.service.PrescriptionVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualReviewServiceImpl implements ManualReviewService {

    private static final String MANUAL_REVIEW_TASK_CREATED = "MANUAL_REVIEW_TASK_CREATED";
    private static final String MANUAL_REVIEW_TASK_CLAIMED = "MANUAL_REVIEW_TASK_CLAIMED";
    private static final String PRESCRIPTION_READY_FOR_MATCHING = "PRESCRIPTION_READY_FOR_MATCHING";

    private final ManualReviewTaskRepository manualReviewTaskRepository;
    private final PrescriptionVerificationService prescriptionVerificationService;
    private final ManualReviewEventPublisher manualReviewEventPublisher;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Transactional
    public ManualReviewTask createTask(UUID prescriptionId, TaskReason reason, TaskPriority priority) {
        requireNotNull(prescriptionId, "prescriptionId is required");
        requireNotNull(reason, "reason is required");
        requireNotNull(priority, "priority is required");

        ManualReviewTask existing = manualReviewTaskRepository
                .findFirstByPrescriptionIdAndStatusNot(prescriptionId, TaskStatus.COMPLETED)
                .orElse(null);

        if (existing != null) {
            log.info("Manual review task already active for prescriptionId={} taskId={} status={}",
                    prescriptionId, existing.getId(), existing.getStatus());
            return existing;
        }

        ManualReviewTask task = new ManualReviewTask();
        task.setPrescriptionId(prescriptionId);
        task.setReason(reason);
        task.setPriority(priority);
        task.setStatus(TaskStatus.PENDING);

        ManualReviewTask saved = manualReviewTaskRepository.save(task);
        Map<String, Object> createMetadata = new LinkedHashMap<>();
        createMetadata.put("status", saved.getStatus().name());
        createMetadata.put("reason", saved.getReason().name());
        createMetadata.put("priority", saved.getPriority().name());
        publishTaskDomainEvent(MANUAL_REVIEW_TASK_CREATED, saved, "SYSTEM", null, createMetadata);

        log.info("Manual review task created: taskId={} prescriptionId={} reason={} priority={}",
                saved.getId(), saved.getPrescriptionId(), saved.getReason(), saved.getPriority());
        manualReviewEventPublisher.sendTaskCreated(saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public boolean claimTask(UUID taskId, UUID pharmacistId) {
        requireNotNull(taskId, "taskId is required");
        requireNotNull(pharmacistId, "pharmacistId is required");

        int updatedRows = manualReviewTaskRepository.claimPendingTask(taskId, pharmacistId);
        if (updatedRows == 1) {
            ManualReviewTask task = manualReviewTaskRepository.findById(taskId).orElse(null);
            Map<String, Object> claimMetadata = new LinkedHashMap<>();
            claimMetadata.put("status", task != null && task.getStatus() != null ? task.getStatus().name() : TaskStatus.IN_PROGRESS.name());
            publishTaskDomainEvent(MANUAL_REVIEW_TASK_CLAIMED, task, "PHARMACIST", pharmacistId, claimMetadata);

            log.info("Manual review task claimed: taskId={} pharmacistId={}", taskId, pharmacistId);
            manualReviewEventPublisher.sendTaskClaimed(taskId, pharmacistId);
            return true;
        }

        log.info("Manual review task claim rejected: taskId={} pharmacistId={} reason=not_pending_or_missing",
                taskId, pharmacistId);
        return false;
    }

    @Override
    @Transactional
    public void completeTask(UUID taskId, List<PrescriptionItemRequestDto> items) {
        requireNotNull(taskId, "taskId is required");
        UUID pharmacistId = resolveAuthenticatedPharmacistId();
        if (items == null || items.isEmpty()) {
            throw new ManualReviewException("At least one prescription item is required");
        }

        ManualReviewTask task = manualReviewTaskRepository.findById(taskId)
                .orElseThrow(() -> new ManualReviewTaskNotFoundException(taskId));

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ManualReviewException("Task must be IN_PROGRESS before completion");
        }
        if (!Objects.equals(task.getAssignedTo(), pharmacistId)) {
            throw new ManualReviewException("Only assigned pharmacist can complete this task");
        }

        prescriptionVerificationService.validateAndSaveItems(task.getPrescriptionId(), pharmacistId, items);

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        ManualReviewTask saved = manualReviewTaskRepository.save(task);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("taskId", saved.getId());
        metadata.put("status", saved.getStatus().name());
        metadata.put("itemCount", items.size());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityType", "PRESCRIPTION");
        payload.put("entityId", saved.getPrescriptionId());
        payload.put("actorType", "PHARMACIST");
        payload.put("actorId", pharmacistId);
        payload.put("contextType", "MANUAL_REVIEW_TASK");
        payload.put("contextId", saved.getId());
        payload.put("metadata", metadata);

        domainEventPublisher.publish(PRESCRIPTION_READY_FOR_MATCHING, payload);
        manualReviewEventPublisher.sendTaskCompleted(saved.getId());

        log.info("Manual review task completed: taskId={} prescriptionId={} pharmacistId={}",
                saved.getId(), saved.getPrescriptionId(), pharmacistId);
    }

    @Override
    @Transactional
    public void rejectTask(UUID taskId, String rejectionReason) {
        requireNotNull(taskId, "taskId is required");
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ManualReviewException("Rejection reason is required");
        }

        UUID pharmacistId = resolveAuthenticatedPharmacistId();
        ManualReviewTask task = manualReviewTaskRepository.findById(taskId)
                .orElseThrow(() -> new ManualReviewTaskNotFoundException(taskId));

        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ManualReviewException("Task must be IN_PROGRESS before rejection");
        }
        if (!Objects.equals(task.getAssignedTo(), pharmacistId)) {
            throw new ManualReviewException("Only assigned pharmacist can reject this task");
        }

        // Update prescription status to REJECTED
        prescriptionVerificationService.reject(task.getPrescriptionId(), pharmacistId, rejectionReason);

        // Mark task as COMPLETED
        task.setStatus(TaskStatus.COMPLETED);
        task.setNotes("Rejected: " + rejectionReason);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        manualReviewTaskRepository.save(task);

        manualReviewEventPublisher.sendTaskCompleted(taskId);
        log.info("Manual review task rejected and completed: taskId={} prescriptionId={} pharmacistId={}",
                taskId, task.getPrescriptionId(), pharmacistId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualReviewTask> getPendingTasks() {
        return manualReviewTaskRepository.findByStatus(TaskStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualReviewTask> getMyTasks(UUID pharmacistId) {
        requireNotNull(pharmacistId, "pharmacistId is required");
        return manualReviewTaskRepository.findByAssignedTo(pharmacistId);
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new ManualReviewException(message);
        }
    }

    private UUID resolveAuthenticatedPharmacistId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            throw new ManualReviewException("Authentication required");
        }

        UUID userId = principal.getUserId();
        if (userId == null) {
            throw new ManualReviewException("Authentication required");
        }
        return userId;
    }

    private void publishTaskDomainEvent(String eventType,
                                        ManualReviewTask task,
                                        String actorType,
                                        UUID actorId,
                                        Map<String, Object> metadata) {
        UUID entityId = task == null ? null : task.getId();
        UUID contextId = task == null ? null : task.getPrescriptionId();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityType", "MANUAL_REVIEW_TASK");
        payload.put("entityId", entityId);
        payload.put("actorType", actorType);
        payload.put("actorId", actorId);
        payload.put("contextType", "PRESCRIPTION");
        payload.put("contextId", contextId);
        payload.put("metadata", metadata == null ? Map.of() : metadata);

        domainEventPublisher.publish(eventType, payload);
    }
}
