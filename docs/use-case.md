# TenaMed Use Case Diagrams (Per Actor)

## Guest
@startuml
left to right direction
skinparam actorStyle awesome

actor Guest

rectangle "TenaMed" {
  (Register As a Hospital Owner)
  (Register As a Pharmacist)
  (Register As an Athlete)
  (Register As a customer)
  (Register As a doctor)
  (Login)
  (Verify OTP)
  (View Invitation)
  (View Medicine)
  (Search Medicines)
}

Guest --> (Register As a Hospital Owner)
Guest --> (Register As a Pharmacist)
Guest --> (Register As an Athlete)
Guest --> (Register As a customer)
Guest -->  (Register As a doctor)
Guest --> (Login)
Guest --> (Verify OTP)
Guest --> (View Invitation)
Guest --> (View Medicine)
Guest --> (Search Medicines)
@enduml


## Customer/Patient

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome
skinparam shadowing false
skinparam packageStyle rectangle

actor "Customer/Patient" as Customer

(View Account Info) as UC1
(Update Account Info) as UC2

(Manage Cart) as UC3
(Add Cart Item) as UC4
(Update Cart Item Quantity) as UC5
(Remove Cart Item) as UC6
(Clear Cart) as UC7

(Checkout) as UC8
(Create Order) as UC9
(View Orders) as UC10

(Initialize Payment) as UC12
(Cancel Payment) as UC14

(View Medicine) as UC15
(Search Medicines) as UC16
(Get Prescription by Code) as UC17

(Create Patient Profile) as UC18
(View Patient Profile) as UC19
(Update Patient Profile) as UC20

(Manage Allergies) as UC21
(Check Medicine Safety) as UC22

(Upload Prescription Image) as UC23
(Check OCR Pipeline Status) as UC24
(Get Pharmacy Matches) as UC25


/' ---------------- ACTOR LINKS ---------------- '/

Customer --> UC1
Customer --> UC2

Customer --> UC3
Customer --> UC8
Customer --> UC10

Customer --> UC12
Customer --> UC14

Customer --> UC15
Customer --> UC16
Customer --> UC17

Customer --> UC18
Customer --> UC19
Customer --> UC20

Customer --> UC21
Customer --> UC22

Customer --> UC23
Customer --> UC24
Customer --> UC25


/' ---------------- RELATIONS ---------------- '/

UC3 .> UC4 : <<include>>
UC3 .> UC5 : <<include>>
UC3 .> UC6 : <<include>>
UC3 .> UC7 : <<include>>

UC8 .> UC9 : <<include>>

UC23 .> UC24 : <<include>>
UC24 .> UC25 : <<include>>

UC22 .> UC21 : <<include>>


/' ---------------- HORIZONTAL ALIGNMENT ---------------- '/

UC1 -[hidden]right-> UC3
UC3 -[hidden]right-> UC8
UC8 -[hidden]right-> UC12
UC12 -[hidden]right-> UC15
UC15 -[hidden]right-> UC18
UC18 -[hidden]right-> UC23

UC4 -[hidden]down-> UC5
UC5 -[hidden]down-> UC6
UC6 -[hidden]down-> UC7

UC18 -[hidden]down-> UC19
UC19 -[hidden]down-> UC20

@enduml
```

## Athlete

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor Athlete

rectangle "TenaMed" {
  (Register Athlete)
  (Check Doping Status)
  (Check Athlete Profile)
}

Athlete --> (Register Athlete)
Athlete --> (Check Doping Status)
Athlete --> (Check Athlete Profile)
@enduml
```

## Doctor

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor Doctor

rectangle "TenaMed" {
  (View Doctor Profile)
  (Create Prescription)
  (View Assigned Prescriptions)
  (Edit Prescription Items)
  (Generate Prescription Code)
  (Create Temporary Patient)
}

Doctor --> (View Doctor Profile)
Doctor --> (Create Prescription)
Doctor --> (View Assigned Prescriptions)
Doctor --> (Edit Prescription Items)
Doctor --> (Generate Prescription Code)
Doctor --> (Create Temporary Patient)
@enduml
```

## Hospital Owner

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Hospital Owner" as HospitalOwner

rectangle "TenaMed" {
  (Register As Hospital Owner)
  (Create Hospital)
  (Update Hospital)
  (View Hospital)
  (Invite Doctor)
  (View Hospital Doctors)
  (Manage Hospital Doctors)
  (Accept Doctor)
  (Reject Doctor)
  (View Hospital Statistics)
}

HospitalOwner --> (Register As Hospital Owner)
HospitalOwner --> (Create Hospital)
HospitalOwner --> (Update Hospital)
HospitalOwner --> (View Hospital)
HospitalOwner --> (Invite Doctor)
HospitalOwner --> (View Hospital Doctors)
HospitalOwner --> (Manage Hospital Doctors)
HospitalOwner --> (Accept Doctor)
HospitalOwner --> (Reject Doctor)
HospitalOwner --> (View Hospital Statistics)
@enduml
```

## Pharmacy Owner

@startuml
left to right direction
skinparam actorStyle awesome
skinparam shadowing false
skinparam packageStyle rectangle

actor "Pharmacy Owner" as PharmacyOwner

(Create Pharmacy) as UC1
(View Pharmacy) as UC2

(Invite Pharmacist) as UC3
(Add Staff) as UC4
(List Staff) as UC5
(Verify Staff) as UC6

(View Pharmacy Orders) as UC7
(Accept Order) as UC8
(Reject Order) as UC9

(Create Inventory) as UC10
(Add Inventory Batch) as UC11
(Edit Inventory Batch) as UC12
(Delete Inventory Batch) as UC13

(Dispatch Delivery) as UC14
(Mark Delivered) as UC15
(Mark Delivery Failed) as UC16


/' ---------------- ACTOR LINKS ---------------- '/

PharmacyOwner --> UC1
PharmacyOwner --> UC2

PharmacyOwner --> UC3
PharmacyOwner --> UC4
PharmacyOwner --> UC5
PharmacyOwner --> UC6

PharmacyOwner --> UC7
PharmacyOwner --> UC8
PharmacyOwner --> UC9

PharmacyOwner --> UC10
PharmacyOwner --> UC14


/' ---------------- RELATIONSHIPS ---------------- '/

UC4 .> UC5 : <<include>>
UC6 .> UC5 : <<extend>>

UC8 .> UC7 : <<extend>>
UC9 .> UC7 : <<extend>>

UC10 .> UC11 : <<include>>
UC10 .> UC12 : <<include>>
UC10 .> UC13 : <<include>>

UC15 .> UC14 : <<extend>>
UC16 .> UC14 : <<extend>>


/' ---------------- HORIZONTAL ALIGNMENT ---------------- '/

UC1 -[hidden]right-> UC3
UC3 -[hidden]right-> UC7
UC7 -[hidden]right-> UC10
UC10 -[hidden]right-> UC14

UC4 -[hidden]down-> UC5
UC5 -[hidden]down-> UC6

UC11 -[hidden]down-> UC12
UC12 -[hidden]down-> UC13

UC15 -[hidden]down-> UC16

@enduml

## Pharmacist


@startuml
left to right direction
skinparam actorStyle awesome

actor Pharmacist

rectangle "TenaMed" {
  (Accept Order)
  (Reject Order)
  (View Inventory List)
  (View Batch Details)
  (View Deliveries)
}

Pharmacist --> (Accept Order)
Pharmacist --> (Reject Order)
Pharmacist --> (View Inventory List)
Pharmacist --> (View Batch Details)
Pharmacist --> (View Deliveries)
@enduml

## Admin Pharmacist

@startuml
left to right direction
skinparam actorStyle awesome
skinparam shadowing false
skinparam packageStyle rectangle

actor "Admin Pharmacist" as AdminPharmacist

(View Review Tasks) as UC1
(Claim Review Task) as UC2
(Complete Review Task) as UC3
(Reject Prescription) as UC4


/' ---------------- ACTOR LINKS ---------------- '/

AdminPharmacist --> UC1
AdminPharmacist --> UC2
AdminPharmacist --> UC3
AdminPharmacist --> UC4


/' ---------------- RELATIONSHIPS ---------------- '/

UC2 .> UC1 : <<extend>>
UC3 .> UC2 : <<include>>
UC4 .> UC2 : <<extend>>


/' ---------------- HORIZONTAL ALIGNMENT ---------------- '/
UC2 -[hidden]left-> UC3
UC1 -[hidden]right-> UC2

UC3 -[hidden]right-> UC4

@enduml

## Admin

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome
skinparam shadowing false

actor Admin


/' ================= COLUMN 1 ================= '/

(View Admin Dashboard) as DASH
(View OCR Stats) as OCR
(View Audit Logs) as LOGS
(View Prescriptions) as PRES

Admin --> DASH

DASH .> OCR : <<include>>
DASH .> LOGS : <<include>>
DASH .> PRES : <<include>>

DASH -[hidden]down-> OCR
OCR -[hidden]down-> LOGS
LOGS -[hidden]down-> PRES


/' ================= COLUMN 2 ================= '/

(View Pending Pharmacies) as PENDING_PH
(Approve Pharmacy) as APPROVE_PH
(Reject Pharmacy) as REJECT_PH
(View Pharmacies Legal Document) as PH_DOCS

Admin --> PENDING_PH

APPROVE_PH .> PENDING_PH : <<extend>>
REJECT_PH .> PENDING_PH : <<extend>>
PH_DOCS .> PENDING_PH : <<include>>

PENDING_PH -[hidden]down-> APPROVE_PH
APPROVE_PH -[hidden]down-> REJECT_PH
REJECT_PH -[hidden]down-> PH_DOCS


/' ================= COLUMN 3 ================= '/

(Search Pharmacies) as SEARCH_PH
(Suspend Pharmacy) as SUSPEND_PH
(Unsuspend Pharmacy) as UNSUSPEND_PH
(View Pharmacy Stats) as PH_STATS

Admin --> SEARCH_PH

SUSPEND_PH .> SEARCH_PH : <<extend>>
UNSUSPEND_PH .> SEARCH_PH : <<extend>>
PH_STATS .> SEARCH_PH : <<include>>

SEARCH_PH -[hidden]down-> SUSPEND_PH
SUSPEND_PH -[hidden]down-> UNSUSPEND_PH
UNSUSPEND_PH -[hidden]down-> PH_STATS


/' ================= COLUMN 4 ================= '/

(View Pending Hospitals) as PENDING_HOSP
(Approve Hospital) as APPROVE_HOSP
(Reject Hospital) as REJECT_HOSP
(View Hospital Legal Document) as HOSP_DOCS

Admin --> PENDING_HOSP

APPROVE_HOSP .> PENDING_HOSP : <<extend>>
REJECT_HOSP .> PENDING_HOSP : <<extend>>
HOSP_DOCS .> PENDING_HOSP : <<include>>

PENDING_HOSP -[hidden]down-> APPROVE_HOSP
APPROVE_HOSP -[hidden]down-> REJECT_HOSP
REJECT_HOSP -[hidden]down-> HOSP_DOCS


/' ================= COLUMN 5 ================= '/

(Search Hospitals) as SEARCH_HOSP
(Suspend Hospital) as SUSPEND_HOSP
(Unsuspend Hospital) as UNSUSPEND_HOSP
(View Hospital Stats) as HOSP_STATS

Admin --> SEARCH_HOSP

SUSPEND_HOSP .> SEARCH_HOSP : <<extend>>
UNSUSPEND_HOSP .> SEARCH_HOSP : <<extend>>
HOSP_STATS .> SEARCH_HOSP : <<include>>

SEARCH_HOSP -[hidden]down-> SUSPEND_HOSP
SUSPEND_HOSP -[hidden]down-> UNSUSPEND_HOSP
UNSUSPEND_HOSP -[hidden]down-> HOSP_STATS


/' ================= COLUMN 6 ================= '/

(Manage Medicines) as MEDS
(Create Admin Pharmacist) as ADMIN_PHARM

Admin --> MEDS
Admin --> ADMIN_PHARM

MEDS -[hidden]down-> ADMIN_PHARM


/' ================= HORIZONTAL DISTRIBUTION ================= '/

DASH -[hidden]right-> PENDING_PH
PENDING_PH -[hidden]right-> SEARCH_PH
SEARCH_PH -[hidden]right-> PENDING_HOSP
PENDING_HOSP -[hidden]right-> SEARCH_HOSP
SEARCH_HOSP -[hidden]right-> MEDS

@enduml
```

## Payment Gateway (Chapa)

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Payment Gateway (Chapa)" as PaymentGateway

rectangle "TenaMed" {
  (Initialize Payment)
  (Verify Payment)
  (Cancel Payment)
  (Process Payment Webhook)
}

PaymentGateway --> (Initialize Payment)
PaymentGateway --> (Verify Payment)
PaymentGateway --> (Cancel Payment)
PaymentGateway --> (Process Payment Webhook)

(Process Payment Webhook) .> (Verify Payment) : <<include>>
@enduml
```

## OCR/AI Service

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "OCR/AI Service" as OcrService

rectangle "TenaMed" {
  (Run OCR Pipeline)
}

OcrService --> (Run OCR Pipeline)
@enduml
```

## Storage Service (Supabase)

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Storage Service (Supabase)" as StorageService

rectangle "TenaMed" {
  (Upload Prescription Image)
  (Upload Product Image)
}

StorageService --> (Upload Prescription Image)
StorageService --> (Upload Product Image)
@enduml
```

## Email Service

```plantuml
@startuml
left to right direction
skinparam actorStyle awesome

actor "Email Service" as EmailService

rectangle "TenaMed" {
  (Send OTP)
}

EmailService --> (Send OTP)
@enduml
```
