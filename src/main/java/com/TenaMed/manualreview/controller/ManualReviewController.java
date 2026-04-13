package com.TenaMed.manualreview.controller;

import com.TenaMed.manualreview.dto.ManualReviewTaskResponseDto;
import com.TenaMed.manualreview.entity.ManualReviewTask;
import com.TenaMed.manualreview.entity.TaskStatus;
import com.TenaMed.manualreview.exception.ManualReviewException;
import com.TenaMed.manualreview.service.ManualReviewService;
import com.TenaMed.prescription.verification.dto.PrescriptionItemRequestDto;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pharmacist/tasks")
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualReviewService manualReviewService;

    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(value = "status", required = false) TaskStatus status) {
        TaskStatus resolvedStatus = status != null ? status : TaskStatus.PENDING;

        List<ManualReviewTaskResponseDto> response = switch (resolvedStatus) {
            case PENDING -> manualReviewService.getPendingTasks().stream().map(this::toDto).toList();
            default -> List.of();
        };

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyTasks(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID pharmacistId = resolveUserId(principal);
        if (pharmacistId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        List<ManualReviewTaskResponseDto> response = manualReviewService.getMyTasks(pharmacistId).stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claimTask(@PathVariable("id") UUID taskId,
                                       @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        UUID pharmacistId = resolveUserId(principal);
        if (pharmacistId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        boolean claimed = manualReviewService.claimTask(taskId, pharmacistId);
        if (claimed) {
            return ResponseEntity.ok(Map.of("message", "Task claimed"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Task is already claimed or unavailable"));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable("id") UUID taskId,
                                          @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                                          @Valid @RequestBody List<PrescriptionItemRequestDto> items) {
        UUID pharmacistId = resolveUserId(principal);
        if (pharmacistId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            manualReviewService.completeTask(taskId, pharmacistId, items);
            return ResponseEntity.ok(Map.of("message", "Task completed"));
        } catch (ManualReviewException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private ManualReviewTaskResponseDto toDto(ManualReviewTask task) {
        return ManualReviewTaskResponseDto.builder()
                .id(task.getId())
                .prescriptionId(task.getPrescriptionId())
                .status(task.getStatus())
                .reason(task.getReason())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .notes(task.getNotes())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private UUID resolveUserId(AuthenticatedUserPrincipal principal) {
        return principal == null ? null : principal.getUserId();
    }
}
