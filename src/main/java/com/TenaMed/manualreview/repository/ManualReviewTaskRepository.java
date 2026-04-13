package com.TenaMed.manualreview.repository;

import com.TenaMed.manualreview.entity.ManualReviewTask;
import com.TenaMed.manualreview.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualReviewTaskRepository extends JpaRepository<ManualReviewTask, UUID> {

    List<ManualReviewTask> findByStatus(TaskStatus status);

    List<ManualReviewTask> findByAssignedTo(UUID assignedTo);

    Optional<ManualReviewTask> findFirstByPrescriptionIdAndStatusNot(UUID prescriptionId, TaskStatus completed);

    @Modifying
    @Query("""
            update ManualReviewTask t
            set t.status = com.TenaMed.manualreview.entity.TaskStatus.IN_PROGRESS,
                t.assignedTo = :userId,
                t.updatedAt = CURRENT_TIMESTAMP
            where t.id = :taskId and t.status = com.TenaMed.manualreview.entity.TaskStatus.PENDING
            """)
    int claimPendingTask(@Param("taskId") UUID taskId, @Param("userId") UUID userId);
}
