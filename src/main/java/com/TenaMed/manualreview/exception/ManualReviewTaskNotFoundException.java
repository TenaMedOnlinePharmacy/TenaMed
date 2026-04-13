package com.TenaMed.manualreview.exception;

import java.util.UUID;

public class ManualReviewTaskNotFoundException extends ManualReviewException {

    public ManualReviewTaskNotFoundException(UUID taskId) {
        super("Manual review task not found: " + taskId);
    }
}
