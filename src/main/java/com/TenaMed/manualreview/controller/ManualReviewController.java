package com.TenaMed.manualreview.controller;

import com.TenaMed.manualreview.dto.ManualReviewRejectionRequestDto;
import com.TenaMed.manualreview.dto.ManualReviewTaskResponseDto;
import com.TenaMed.manualreview.entity.ManualReviewTask;
import com.TenaMed.manualreview.entity.TaskStatus;
import com.TenaMed.manualreview.exception.ManualReviewException;
import com.TenaMed.manualreview.service.ManualReviewService;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.verification.dto.PrescriptionItemRequestDto;
import com.TenaMed.verification.exception.VerificationException;
import com.TenaMed.user.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/manual-review")
@RequiredArgsConstructor
public class ManualReviewController {

    private final ManualReviewService manualReviewService;
    private final PrescriptionRepository prescriptionRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_PHARMACIST', 'ADMIN')")
    public ResponseEntity<?> getTasks(@RequestParam(value = "status", required = false) TaskStatus status) {
        TaskStatus resolvedStatus = status != null ? status : TaskStatus.PENDING;

        List<ManualReviewTaskResponseDto> response = switch (resolvedStatus) {
            case PENDING -> manualReviewService.getPendingTasks().stream().map(this::toDto).toList();
            default -> List.of();
        };

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN_PHARMACIST', 'ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN_PHARMACIST')")
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
    @PreAuthorize("hasRole('ADMIN_PHARMACIST')")
    public ResponseEntity<?> completeTask(@PathVariable("id") UUID taskId,
                                          @Valid @RequestBody List<PrescriptionItemRequestDto> items) {
        try {
            manualReviewService.completeTask(taskId, items);
            return ResponseEntity.ok(Map.of("message", "Task completed"));
        } catch (ManualReviewException ex) {
            HttpStatus status = "Authentication required".equals(ex.getMessage())
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        } catch (VerificationException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN_PHARMACIST')")
    public ResponseEntity<?> rejectTask(@PathVariable("id") UUID taskId,
                                        @Valid @RequestBody ManualReviewRejectionRequestDto request) {
        try {
            manualReviewService.rejectTask(taskId, request.getReason());
            return ResponseEntity.ok(Map.of("message", "Task rejected and completed"));
        } catch (ManualReviewException ex) {
            HttpStatus status = "Authentication required".equals(ex.getMessage())
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
        }
    }

    private ManualReviewTaskResponseDto toDto(ManualReviewTask task) {
        Optional<Prescription> prescription = prescriptionRepository.findById(task.getPrescriptionId());
        String imageUrl;
        if(prescription.isPresent()) {
             imageUrl = prescription.get().getOriginalImages();
        }else{
            imageUrl = "";
        }

        return ManualReviewTaskResponseDto.builder()
                .id(task.getId())
                .prescriptionId(task.getPrescriptionId())
                .status(task.getStatus())
                .reason(task.getReason())
                .priority(task.getPriority())
                .assignedTo(task.getAssignedTo())
                .notes(task.getNotes())
                .imageUrl(imageUrl)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .completedAt(task.getCompletedAt())
                .build();
    }

    private UUID resolveUserId(AuthenticatedUserPrincipal principal) {
        return principal == null ? null : principal.getUserId();
    }
}
