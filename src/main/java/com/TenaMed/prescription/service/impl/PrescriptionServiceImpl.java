package com.TenaMed.prescription.service.impl;

import com.TenaMed.common.exception.BadRequestException;
import com.TenaMed.common.exception.ResourceNotFoundException;
import com.TenaMed.doctor.dto.DoctorPrescriptionItemRequestDto;
import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.service.PrescriptionItemService;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.prescription.entity.Prescription;
import com.TenaMed.prescription.entity.PrescriptionType;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.prescription.service.PrescriptionService;
import com.TenaMed.events.DomainEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionItemService prescriptionItemService;
    private final DomainEventService domainEventService;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   MedicineRepository medicineRepository,
                                   PrescriptionItemService prescriptionItemService,
                                   DomainEventService domainEventService) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
        this.prescriptionItemService = prescriptionItemService;
        this.domainEventService = domainEventService;
    }

    @Override
    public Prescription createUploadedPrescription() {
        Prescription prescription = new Prescription();
        prescription.setType(PrescriptionType.UPLOADED);
        prescription.setStatus("UPLOADED");
        prescription.setIsVerified(false);
        Prescription saved = prescriptionRepository.save(prescription);
        domainEventService.publish(
            "PRESCRIPTION_UPLOADED",
            "PRESCRIPTION",
            saved.getId(),
            "PLATFORM",
            null,
            Map.of("status", saved.getStatus())
        );
        return saved;
    }

    @Override
    public Prescription attachOcrDates(UUID id, String createdDate, String expirationDate) {
        Prescription prescription = prescriptionRepository.findById(id).orElse(null);
        if (prescription == null) {
            return null;
        }

        prescription.setIssueDate(parseToLocalDate(createdDate));
        prescription.setExpiryDate(parseToLocalDate(expirationDate));
        Prescription saved = prescriptionRepository.save(prescription);
        domainEventService.publish(
            "PRESCRIPTION_OCR_DATES_ATTACHED",
            "PRESCRIPTION",
            saved.getId(),
            "SYSTEM",
            null,
            "PLATFORM",
            null,
            Map.of("issueDate", String.valueOf(saved.getIssueDate()), "expiryDate", String.valueOf(saved.getExpiryDate()))
        );
        return saved;
    }

    @Override
    public Prescription createFromOcrDates(String createdDate, String expirationDate) {
        Prescription prescription = new Prescription();
        prescription.setIssueDate(parseToLocalDate(createdDate));
        prescription.setExpiryDate(parseToLocalDate(expirationDate));
        prescription.setType(PrescriptionType.UPLOADED);
        return prescriptionRepository.save(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public Prescription getPrescription(UUID id) {
        return prescriptionRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPrescriptionValid(UUID id) {
        Prescription prescription = getPrescription(id);
        if (prescription == null) {
            return false;
        }

        if (Boolean.FALSE.equals(prescription.getIsVerified())) {
            return false;
        }

        if (Boolean.TRUE.equals(prescription.getIsUsed())) {
            return false;
        }

        return prescription.getExpiryDate() == null || !prescription.getExpiryDate().isBefore(LocalDate.now());
    }

    @Override
    public Prescription createDoctorPrescription(UUID patientId,
                                                 UUID hospitalId,
                                                 UUID doctorId,
                                                 LocalDate expiryDate,
                                                 Integer maxRefillsAllowed,
                                                 String uniqueCode,
                                                 Boolean highRisk,
                                                 PrescriptionType type,
                                                 List<DoctorPrescriptionItemRequestDto> items) {
        if (patientId == null) {
            throw new BadRequestException("patientId is required");
        }
        if (hospitalId == null) {
            throw new BadRequestException("hospitalId is required");
        }
        if (doctorId == null) {
            throw new BadRequestException("doctorId is required");
        }
        if (expiryDate == null) {
            throw new BadRequestException("expiryDate is required");
        }
        if (maxRefillsAllowed == null || maxRefillsAllowed < 0) {
            throw new BadRequestException("maxRefillsAllowed must be >= 0");
        }
        if (type == null) {
            throw new BadRequestException("type is required");
        }
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("items are required");
        }

        String normalizedUniqueCode = normalizeNullable(uniqueCode);
        if (normalizedUniqueCode != null && prescriptionRepository.existsByUniqueCode(normalizedUniqueCode)) {
            throw new BadRequestException("Prescription uniqueCode already exists");
        }

        Prescription prescription = new Prescription();
        prescription.setPatientId(patientId);
        prescription.setIssueDate(LocalDate.now());
        prescription.setExpiryDate(expiryDate);
        prescription.setMaxRefillsAllowed(maxRefillsAllowed);
        prescription.setRefillsUsed(0);
        prescription.setHospitalId(hospitalId);
        prescription.setDoctorId(doctorId);
        prescription.setUniqueCode(normalizedUniqueCode);
        prescription.setHighRisk(highRisk != null && highRisk);
        prescription.setType(type);
        prescription.setIsUsed(false);
        prescription.setIsVerified(true);
        prescription.setStatus("ISSUED");

        Prescription saved = prescriptionRepository.save(prescription);

        for (DoctorPrescriptionItemRequestDto item : items) {
            Medicine medicine = medicineRepository.findById(item.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicineId()));

            PrescriptionItem prescriptionItem = new PrescriptionItem();
            prescriptionItem.setPrescription(saved);
            prescriptionItem.setMedicine(medicine);
            prescriptionItem.setForm(item.getForm());
            prescriptionItem.setStrength(item.getStrength());
            prescriptionItem.setInstructions(item.getInstruction());
            prescriptionItem.setQuantity(item.getQuantity());
            prescriptionItemService.save(prescriptionItem);
        }

        domainEventService.publish(
            "PRESCRIPTION_CREATED_BY_DOCTOR",
            "PRESCRIPTION",
            saved.getId(),
            "DOCTOR",
            doctorId,
            "HOSPITAL",
            hospitalId,
            Map.of("patientId", patientId.toString(), "type", String.valueOf(type))
        );
        return saved;
    }

    private LocalDate parseToLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
        }

        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
        }

        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
