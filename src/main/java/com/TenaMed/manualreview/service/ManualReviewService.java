package com.TenaMed.manualreview.service;

import com.TenaMed.manualreview.entity.ManualReviewTask;
import com.TenaMed.manualreview.entity.TaskPriority;
import com.TenaMed.manualreview.entity.TaskReason;
import com.TenaMed.verification.dto.PrescriptionItemRequestDto;

import java.util.List;
import java.util.UUID;

public interface ManualReviewService {

    ManualReviewTask createTask(UUID prescriptionId, TaskReason reason, TaskPriority priority);

    boolean claimTask(UUID taskId, UUID pharmacistId);

    void completeTask(UUID taskId, List<PrescriptionItemRequestDto> items);

    void rejectTask(UUID taskId, String rejectionReason);

    List<ManualReviewTask> getPendingTasks();

    List<ManualReviewTask> getMyTasks(UUID pharmacistId);
}
