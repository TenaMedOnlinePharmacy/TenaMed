package com.TenaMed.medicine.controller;

import com.TenaMed.medicine.dto.MedicineRequestDto;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.dto.MedicineSearchDto;
import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleRequestDto;
import com.TenaMed.medicine.dto.MedicineDopingRuleResponseDto;
import com.TenaMed.medicine.dto.MedicineNameCategoryResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.entity.Product;
import com.TenaMed.medicine.exception.MedicineAlreadyExistsException;
import com.TenaMed.medicine.exception.MedicineNotFoundException;
import com.TenaMed.medicine.exception.MedicineValidationException;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.medicine.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final ProductRepository productRepository;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMedicineWithImage(@Valid @RequestPart("medicine") MedicineRequestDto requestDto,
                                                     @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            MedicineResponseDto response = medicineService.createMedicine(requestDto, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MedicineAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (MedicineValidationException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable UUID id) {
        try {
            Optional<Product> product = productRepository.findById(id);
            if (product.isPresent()) {
                UUID medcineID = product.get().getMedicine().getId();

                MedicineResponseDto response = medicineService.getMedicineById(medcineID);

                return ResponseEntity.ok(response);
            }
            else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MedicineNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/{pharmacyId}")
    public ResponseEntity<?> getMedicineById(@PathVariable UUID id, @PathVariable UUID pharmacyId) {
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new MedicineNotFoundException("Product not found with id: " + id));
            
            UUID medicineId = product.getMedicine().getId();
            MedicineResponseDto response = medicineService.getMedicineById(medicineId, pharmacyId);
            
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
    public ResponseEntity<List<MedicinePharmacySearchResponseDto>> searchMedicines(
            @ModelAttribute MedicineSearchDto searchDto) {
        return ResponseEntity.ok(medicineService.searchMedicines(searchDto));
    }

    @GetMapping("/search/name-category")
    public ResponseEntity<List<MedicineNameCategoryResponseDto>> searchMedicineNameCategory(@RequestParam String keyword) {
        return ResponseEntity.ok(medicineService.searchMedicineNamesByKeyword(keyword));
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
