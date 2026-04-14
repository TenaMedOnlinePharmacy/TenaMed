package com.TenaMed.ocr.event;

import java.util.UUID;

public record PrescriptionPipelinePersistedEvent(UUID prescriptionId, int medicinesCount) {
}
