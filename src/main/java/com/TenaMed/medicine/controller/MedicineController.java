package com.TenaMed.medicine.controller;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.dto.MedicineSearchDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;
import com.TenaMed.medicine.exception.MedicineAlreadyExistsException;
import com.TenaMed.medicine.exception.MedicineNotFoundException;
import com.TenaMed.medicine.exception.MedicineValidationException;
import com.TenaMed.medicine.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {

    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    public ResponseEntity<?> createMedicine(@Valid @RequestBody MedicineRequestDto requestDto) {
        try {
            MedicineResponseDto response = medicineService.createMedicine(requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable UUID id) {
        try {
            MedicineResponseDto response = medicineService.getMedicineById(id);
            return ResponseEntity.ok(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<MedicineResponseDto>> getAllMedicines() {
        return ResponseEntity.ok(medicineService.getAllMedicines());
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicineResponseDto>> searchMedicines(
            @ModelAttribute MedicineSearchDto searchDto) {
        return ResponseEntity.ok(medicineService.searchMedicines(searchDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMedicine(@PathVariable UUID id,
                                            @Valid @RequestBody MedicineRequestDto requestDto) {
        try {
            MedicineResponseDto response = medicineService.updateMedicine(id, requestDto);
            return ResponseEntity.ok(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{medicineId}/allergens/{allergenId}")
    public ResponseEntity<?> addAllergenToMedicine(@PathVariable UUID medicineId,
                                                    @PathVariable UUID allergenId) {
        try {
            MedicineResponseDto response = medicineService.addAllergenToMedicine(medicineId, allergenId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{medicineId}/allergens/{allergenId}")
    public ResponseEntity<?> removeAllergenFromMedicine(@PathVariable UUID medicineId,
                                                         @PathVariable UUID allergenId) {
        try {
            MedicineResponseDto response = medicineService.removeAllergenFromMedicine(medicineId, allergenId);
            return ResponseEntity.ok(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{medicineId}/doping-rules")
    public ResponseEntity<?> addDopingRuleToMedicine(@PathVariable UUID medicineId,
                                                     @Valid @RequestBody MedicineDopingRuleRequestDto requestDto) {
        try {
            MedicineDopingRuleResponseDto response = medicineService.addDopingRuleToMedicine(medicineId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{medicineId}/doping-rules/{ruleId}")
    public ResponseEntity<?> removeDopingRuleFromMedicine(@PathVariable UUID medicineId,
                                                           @PathVariable UUID ruleId) {
        try {
            medicineService.removeDopingRuleFromMedicine(medicineId, ruleId);
            return ResponseEntity.noContent().build();
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicine(@PathVariable UUID id) {
        try {
            medicineService.deleteMedicine(id);
            return ResponseEntity.noContent().build();
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
