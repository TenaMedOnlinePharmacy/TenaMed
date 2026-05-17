# 4.13 Testing and Validation

## 4.13.1 Testing Frameworks and Tooling
This project uses a layered testing strategy centered on Spring Boot Test and JUnit 5 for unit and integration testing. Mockito provides isolation for service and controller logic. MockMvc is used for HTTP-layer validation in controller tests without full server startup. H2 (PostgreSQL mode) powers repository and integration tests, and Spring Security Test supports authenticated/role-based scenarios.

**Evidence:**
- Dependencies and testing stack: [pom.xml](pom.xml)
- H2 and test properties: [src/test/resources/application.properties](src/test/resources/application.properties)

## 4.13.2 Unit Testing Coverage (By Module)
Unit tests cover user identity, pharmacy, inventory, payment, invitation, hospital, doctor, email, OCR/normalization, and verification logic. The list below summarizes the test classes and core behaviors validated.

### User and Identity
- [src/test/java/com/TenaMed/user/controller/AuthControllerTests.java](src/test/java/com/TenaMed/user/controller/AuthControllerTests.java): registration, onboarding flows, response validation.
- [src/test/java/com/TenaMed/user/controller/IdentityControllerTests.java](src/test/java/com/TenaMed/user/controller/IdentityControllerTests.java): register and login endpoints.
- [src/test/java/com/TenaMed/user/controller/UserAdminControllerTests.java](src/test/java/com/TenaMed/user/controller/UserAdminControllerTests.java): role management and user detail retrieval.
- [src/test/java/com/TenaMed/user/service/IdentityServiceImplTests.java](src/test/java/com/TenaMed/user/service/IdentityServiceImplTests.java): registration validation and login attempt handling.
- [src/test/java/com/TenaMed/user/service/PharmacistOnboardingServiceImplTests.java](src/test/java/com/TenaMed/user/service/PharmacistOnboardingServiceImplTests.java): license upload + pharmacy creation logic.
- [src/test/java/com/TenaMed/user/service/HospitalOwnerOnboardingServiceImplTests.java](src/test/java/com/TenaMed/user/service/HospitalOwnerOnboardingServiceImplTests.java): owner onboarding and license validation.
- [src/test/java/com/TenaMed/user/service/AthleteOnboardingServiceImplTests.java](src/test/java/com/TenaMed/user/service/AthleteOnboardingServiceImplTests.java): athlete onboarding and defaults.

### Pharmacy and Orders
- [src/test/java/com/TenaMed/pharmacy/controller/PharmacyControllerTests.java](src/test/java/com/TenaMed/pharmacy/controller/PharmacyControllerTests.java): create, fetch, verify, invite flows.
- [src/test/java/com/TenaMed/pharmacy/controller/OrderControllerTests.java](src/test/java/com/TenaMed/pharmacy/controller/OrderControllerTests.java): create/accept/reject/update-payment behaviors and authorization.
- [src/test/java/com/TenaMed/pharmacy/controller/StaffControllerTests.java](src/test/java/com/TenaMed/pharmacy/controller/StaffControllerTests.java): staff add/list/verify endpoints.
- [src/test/java/com/TenaMed/pharmacy/controller/PharmacistControllerTests.java](src/test/java/com/TenaMed/pharmacy/controller/PharmacistControllerTests.java): invite token flows.
- [src/test/java/com/TenaMed/pharmacy/service/PharmacyServiceImplTests.java](src/test/java/com/TenaMed/pharmacy/service/PharmacyServiceImplTests.java): role-based verification, invitation rules, creation.
- [src/test/java/com/TenaMed/pharmacy/service/OrderServiceImplTests.java](src/test/java/com/TenaMed/pharmacy/service/OrderServiceImplTests.java): create, accept, payment state transitions.
- [src/test/java/com/TenaMed/pharmacy/service/StaffServiceImplTests.java](src/test/java/com/TenaMed/pharmacy/service/StaffServiceImplTests.java): add/verify staff validations.
- [src/test/java/com/TenaMed/pharmacy/service/PrescriptionInventoryMatchServiceImplTests.java](src/test/java/com/TenaMed/pharmacy/service/PrescriptionInventoryMatchServiceImplTests.java): inventory match response correctness.
- [src/test/java/com/TenaMed/pharmacy/service/PrescriptionVerifiedEventListenerTests.java](src/test/java/com/TenaMed/pharmacy/service/PrescriptionVerifiedEventListenerTests.java): event-driven integration.
- Mapper tests: [src/test/java/com/TenaMed/pharmacy/mapper/PharmacyMapperTests.java](src/test/java/com/TenaMed/pharmacy/mapper/PharmacyMapperTests.java), [src/test/java/com/TenaMed/pharmacy/mapper/UserPharmacyMapperTests.java](src/test/java/com/TenaMed/pharmacy/mapper/UserPharmacyMapperTests.java), [src/test/java/com/TenaMed/pharmacy/mapper/OrderMapperTests.java](src/test/java/com/TenaMed/pharmacy/mapper/OrderMapperTests.java)
- Domain model wiring: [src/test/java/com/TenaMed/pharmacy/entity/PharmacyDomainModelTests.java](src/test/java/com/TenaMed/pharmacy/entity/PharmacyDomainModelTests.java)

### Inventory
- [src/test/java/com/TenaMed/inventory/controller/InventoryControllerTests.java](src/test/java/com/TenaMed/inventory/controller/InventoryControllerTests.java): create, add batch, reserve/release error cases.
- [src/test/java/com/TenaMed/inventory/service/InventoryServiceImplTests.java](src/test/java/com/TenaMed/inventory/service/InventoryServiceImplTests.java): validation, price resolution, reservation rules.
- [src/test/java/com/TenaMed/inventory/service/InventoryServiceFlowIntegrationTests.java](src/test/java/com/TenaMed/inventory/service/InventoryServiceFlowIntegrationTests.java): FIFO confirmation and stock release.
- [src/test/java/com/TenaMed/inventory/service/InventoryReservationConcurrencyTests.java](src/test/java/com/TenaMed/inventory/service/InventoryReservationConcurrencyTests.java): parallel reservation safety.
- [src/test/java/com/TenaMed/inventory/repository/InventoryRepositoryDataJpaTests.java](src/test/java/com/TenaMed/inventory/repository/InventoryRepositoryDataJpaTests.java): repository query validation.
- [src/test/java/com/TenaMed/inventory/entity/InventoryEntityPersistenceTests.java](src/test/java/com/TenaMed/inventory/entity/InventoryEntityPersistenceTests.java): persistence of stock and movement records.
- Mapper tests: [src/test/java/com/TenaMed/inventory/mapper/InventoryMapperTests.java](src/test/java/com/TenaMed/inventory/mapper/InventoryMapperTests.java), [src/test/java/com/TenaMed/inventory/mapper/BatchMapperTests.java](src/test/java/com/TenaMed/inventory/mapper/BatchMapperTests.java)

### Medicine and Doping Rules
- [src/test/java/com/TenaMed/medicine/controller/MedicineControllerTests.java](src/test/java/com/TenaMed/medicine/controller/MedicineControllerTests.java): allergen and anti-doping rule association.

### Payment
- [src/test/java/com/TenaMed/payment/service/PaymentServiceTests.java](src/test/java/com/TenaMed/payment/service/PaymentServiceTests.java): cancellation mapping and error handling.
- [src/test/java/com/TenaMed/payment/controller/PaymentControllerTests.java](src/test/java/com/TenaMed/payment/controller/PaymentControllerTests.java): cancellation endpoint response mapping.

### Hospital and Doctor
- [src/test/java/com/TenaMed/hospital/service/HospitalServiceImplTests.java](src/test/java/com/TenaMed/hospital/service/HospitalServiceImplTests.java): owner verification, invite flows.
- [src/test/java/com/TenaMed/doctor/service/DoctorServiceImplTests.java](src/test/java/com/TenaMed/doctor/service/DoctorServiceImplTests.java): invite registration and role checks.
- [src/test/java/com/TenaMed/doctor/controller/DoctorControllerTests.java](src/test/java/com/TenaMed/doctor/controller/DoctorControllerTests.java): doctor invite token usage.

### Invitations and Email
- [src/test/java/com/TenaMed/invitation/service/InvitationServiceImplTests.java](src/test/java/com/TenaMed/invitation/service/InvitationServiceImplTests.java): token expiry, acceptance, and email send failures.
- [src/test/java/com/TenaMed/email/service/SmtpEmailServiceTests.java](src/test/java/com/TenaMed/email/service/SmtpEmailServiceTests.java): SMTP failure handling and HTML/plain text paths.

### OCR, Normalization, and Verification
- [src/test/java/com/TenaMed/ocr/service/PrescriptionPipelineServiceTests.java](src/test/java/com/TenaMed/ocr/service/PrescriptionPipelineServiceTests.java): manual review routing and successful pipeline persistence.
- [src/test/java/com/TenaMed/Normalization/service/DrugNormalizationServiceTests.java](src/test/java/com/TenaMed/Normalization/service/DrugNormalizationServiceTests.java): exact/synonym/fuzzy match logic and thresholds.
- [src/test/java/com/TenaMed/Normalization/service/OcrDrugNormalizationServiceTests.java](src/test/java/com/TenaMed/Normalization/service/OcrDrugNormalizationServiceTests.java): persistence and missing-prescription paths.
- [src/test/java/com/TenaMed/Normalization/entity/PrescriptionItemTests.java](src/test/java/com/TenaMed/Normalization/entity/PrescriptionItemTests.java): entity defaults.
- [src/test/java/com/TenaMed/verification/service/PrescriptionVerificationServiceTests.java](src/test/java/com/TenaMed/verification/service/PrescriptionVerificationServiceTests.java): normalized confidence checks.

### Application Bootstrapping
- [src/test/java/com/TenaMed/demo/DemoApplicationTests.java](src/test/java/com/TenaMed/demo/DemoApplicationTests.java): context load validation.

## 4.13.3 Integration Testing
Integration tests validate realistic flows using actual repositories and transactional boundaries. Key flows include the pharmacy lifecycle, inventory FIFO confirmation, and reservation concurrency.

**Evidence:**
- Pharmacy lifecycle integration: [src/test/java/com/TenaMed/pharmacy/integration/PharmacyLifecycleIntegrationTests.java](src/test/java/com/TenaMed/pharmacy/integration/PharmacyLifecycleIntegrationTests.java)
- Inventory flow integration: [src/test/java/com/TenaMed/inventory/service/InventoryServiceFlowIntegrationTests.java](src/test/java/com/TenaMed/inventory/service/InventoryServiceFlowIntegrationTests.java)
- Concurrency integration: [src/test/java/com/TenaMed/inventory/service/InventoryReservationConcurrencyTests.java](src/test/java/com/TenaMed/inventory/service/InventoryReservationConcurrencyTests.java)

## 4.13.4 Validation Rules and Input Constraints
The system enforces validation at DTO and entity levels using Jakarta Bean Validation, ensuring only safe and complete data enters core workflows.

**Representative constraints:**
- Medicine validation: [src/main/java/com/TenaMed/medicine/entity/Medicine.java](src/main/java/com/TenaMed/medicine/entity/Medicine.java)
- Pharmacy validation: [src/main/java/com/TenaMed/pharmacy/entity/Pharmacy.java](src/main/java/com/TenaMed/pharmacy/entity/Pharmacy.java)
- Prescription validation: [src/main/java/com/TenaMed/prescription/entity/Prescription.java](src/main/java/com/TenaMed/prescription/entity/Prescription.java)
- Auth and onboarding DTOs: [src/main/java/com/TenaMed/user/dto/RegisterRequestDto.java](src/main/java/com/TenaMed/user/dto/RegisterRequestDto.java), [src/main/java/com/TenaMed/user/dto/RegisterPharmacistRequestDto.java](src/main/java/com/TenaMed/user/dto/RegisterPharmacistRequestDto.java), [src/main/java/com/TenaMed/user/dto/RegisterHospitalOwnerRequestDto.java](src/main/java/com/TenaMed/user/dto/RegisterHospitalOwnerRequestDto.java), [src/main/java/com/TenaMed/user/dto/RegisterAthleteRequestDto.java](src/main/java/com/TenaMed/user/dto/RegisterAthleteRequestDto.java)
- Prescription item verification DTO: [src/main/java/com/TenaMed/verification/dto/PrescriptionItemRequestDto.java](src/main/java/com/TenaMed/verification/dto/PrescriptionItemRequestDto.java)
- Inventory and pharmacy request DTOs: [src/main/java/com/TenaMed/inventory/dto/request/InventoryCreateRequest.java](src/main/java/com/TenaMed/inventory/dto/request/InventoryCreateRequest.java), [src/main/java/com/TenaMed/pharmacy/dto/request/CreatePharmacyRequest.java](src/main/java/com/TenaMed/pharmacy/dto/request/CreatePharmacyRequest.java)

## 4.13.5 Security Validation
Security validation is applied through role-based access and JWT session handling. Tests assert authorization checks in services and controllers, and production code enforces authenticated contexts via the security layer.

**Evidence:**
- JWT and authentication flow: [src/main/java/com/TenaMed/user/security/JwtService.java](src/main/java/com/TenaMed/user/security/JwtService.java), [src/main/java/com/TenaMed/user/security/JwtAuthenticationFilter.java](src/main/java/com/TenaMed/user/security/JwtAuthenticationFilter.java)
- User principal mapping: [src/main/java/com/TenaMed/user/security/CustomUserDetailsService.java](src/main/java/com/TenaMed/user/security/CustomUserDetailsService.java)
- Role checks validated in tests: [src/test/java/com/TenaMed/pharmacy/service/PharmacyServiceImplTests.java](src/test/java/com/TenaMed/pharmacy/service/PharmacyServiceImplTests.java)

## 4.13.6 Database and Persistence Validation
Repository tests verify query correctness, entity relationships, and persistence behavior under the configured H2 test database.

**Evidence:**
- Pharmacy and order repository tests: [src/test/java/com/TenaMed/pharmacy/repository/PharmacyRepositoryDataJpaTests.java](src/test/java/com/TenaMed/pharmacy/repository/PharmacyRepositoryDataJpaTests.java)
- Inventory repository tests: [src/test/java/com/TenaMed/inventory/repository/InventoryRepositoryDataJpaTests.java](src/test/java/com/TenaMed/inventory/repository/InventoryRepositoryDataJpaTests.java)
- Entity persistence: [src/test/java/com/TenaMed/inventory/entity/InventoryEntityPersistenceTests.java](src/test/java/com/TenaMed/inventory/entity/InventoryEntityPersistenceTests.java)
- Test database configuration: [src/test/resources/application.properties](src/test/resources/application.properties)

## 4.13.7 OCR and AI Workflow Validation
The OCR pipeline includes validation thresholds for confidence scoring and normalization outcomes. Tests verify manual review routing, successful persistence, and threshold-based acceptance.

**Evidence:**
- OCR pipeline behavior: [src/test/java/com/TenaMed/ocr/service/PrescriptionPipelineServiceTests.java](src/test/java/com/TenaMed/ocr/service/PrescriptionPipelineServiceTests.java)
- Normalization algorithm coverage: [src/test/java/com/TenaMed/Normalization/service/DrugNormalizationServiceTests.java](src/test/java/com/TenaMed/Normalization/service/DrugNormalizationServiceTests.java)
- OCR normalization persistence: [src/test/java/com/TenaMed/Normalization/service/OcrDrugNormalizationServiceTests.java](src/test/java/com/TenaMed/Normalization/service/OcrDrugNormalizationServiceTests.java)
- Normalized confidence checks: [src/test/java/com/TenaMed/verification/service/PrescriptionVerificationServiceTests.java](src/test/java/com/TenaMed/verification/service/PrescriptionVerificationServiceTests.java)
- Threshold configuration: [src/test/resources/application.properties](src/test/resources/application.properties)

## 4.13.8 Exception Handling and Error Validation
Global exception handlers enforce consistent error responses across modules and reduce ambiguity in client-facing failures.

**Evidence:**
- User identity exception mapping: [src/main/java/com/TenaMed/user/controller/IdentityExceptionHandler.java](src/main/java/com/TenaMed/user/controller/IdentityExceptionHandler.java)
- Verification exception mapping: [src/main/java/com/TenaMed/verification/controller/VerificationExceptionHandler.java](src/main/java/com/TenaMed/verification/controller/VerificationExceptionHandler.java)
- Anti-doping exception mapping: [src/main/java/com/TenaMed/antidoping/controller/AntiDopingGlobalExceptionHandler.java](src/main/java/com/TenaMed/antidoping/controller/AntiDopingGlobalExceptionHandler.java)
- Complaint exception mapping: [src/main/java/com/TenaMed/complaint/controller/ComplaintExceptionHandler.java](src/main/java/com/TenaMed/complaint/controller/ComplaintExceptionHandler.java)
- Hospital/doctor invitation exception mapping: [src/main/java/com/TenaMed/hospital/controller/HospitalDoctorInvitationExceptionHandler.java](src/main/java/com/TenaMed/hospital/controller/HospitalDoctorInvitationExceptionHandler.java)

## 4.13.9 Manual Testing Evidence
Manual testing artifacts exist for WebSocket/STOMP verification workflows. These artifacts are useful for validating real-time pharmacist review features and messaging behavior.

**Evidence:**
- STOMP web test client: [src/main/resources/static/manualreview-stomp-test.html](src/main/resources/static/manualreview-stomp-test.html)
- Manual review browser and node clients: [tmp/manualreview-stomp-client.html](tmp/manualreview-stomp-client.html), [tmp/manualreview-stomp-client.js](tmp/manualreview-stomp-client.js), [tmp/package.json](tmp/package.json)

## 4.13.10 Quality Assurance and Reliability Mechanisms
The codebase includes transactional boundaries, auditing, caching, and concurrency controls that support reliability in production workflows.

**Evidence:**
- Transactional verification and review flows: [src/main/java/com/TenaMed/verification/service/PrescriptionVerificationService.java](src/main/java/com/TenaMed/verification/service/PrescriptionVerificationService.java), [src/main/java/com/TenaMed/verification/service/ManualReviewService.java](src/main/java/com/TenaMed/verification/service/ManualReviewService.java)
- Inventory consistency checks: [src/main/java/com/TenaMed/inventory/service/impl/InventoryServiceImpl.java](src/main/java/com/TenaMed/inventory/service/impl/InventoryServiceImpl.java)
- Normalization caching: [src/main/java/com/TenaMed/Normalization/service/DatabaseDrugLookupService.java](src/main/java/com/TenaMed/Normalization/service/DatabaseDrugLookupService.java)
- Anti-doping ingredient resolution and matching: [src/main/java/com/TenaMed/antidoping/service/IngredientResolverService.java](src/main/java/com/TenaMed/antidoping/service/IngredientResolverService.java), [src/main/java/com/TenaMed/antidoping/service/DopingCheckService.java](src/main/java/com/TenaMed/antidoping/service/DopingCheckService.java)
- Payment event logging and verification updates: [src/main/java/com/TenaMed/payment/service/PaymentService.java](src/main/java/com/TenaMed/payment/service/PaymentService.java)

## 4.13.11 Test Metrics and Summary
- Test classes: 46
- Test methods (annotated with `@Test`): 150
- Coverage breadth: strong for service/controller logic in pharmacy, inventory, identity, and verification; moderate for payment and invitation flows; limited explicit unit tests for JWT/Redis session validation and payment webhook processing.

**Evidence:**
- Test classes and method counts collected from the test directory.

## 4.13.12 Limitations and Improvement Opportunities
- Add explicit tests for JWT refresh, Redis session validation, and token revocation.
- Add payment webhook success/failure integration tests and signature validation.
- Extend manual review WebSocket tests into automated integration coverage.
- Add mutation testing or coverage gates to quantify deeper branch coverage.
