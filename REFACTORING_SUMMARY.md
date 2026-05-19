# 📋 HealthTrack - Refactorización Normalizada de Base de Datos

## Resumen Ejecutivo

El proyecto HealthTrack ha sido completamente refactorizado para cumplir con los requisitos académicos de mantener exactamente **15 entidades normalizadas** en la base de datos Firestore, simulando un modelo relacional con referencias de IDs como claves foráneas. Además, se ha integrado **Firebase Authentication** para la gestión segura de credenciales de usuario.

---

## 📐 Las 15 Entidades Normalizadas

### 1. **Role** (`Role.java` + `RoleDAO.java`)
- **Campos:** `id`, `name`, `description`
- **Propósito:** Define los roles del sistema (patient, doctor, admin)
- **Relaciones:** Referenciado por UserProfile

### 2. **UserProfile** (`UserProfile.java` + `UserProfileDAO.java`)
- **Campos:** `id`, `authUid`, `roleId`, `email`, `registeredAt`
- **Propósito:** Perfil de usuario vinculado a Firebase Authentication
- **Relaciones:** 
  - `authUid` → UID de Firebase Auth
  - `roleId` → ID de Role

### 3. **Specialty** (`Specialty.java` + `SpecialtyDAO.java`)
- **Campos:** `id`, `name`
- **Propósito:** Especialidades médicas disponibles
- **Relaciones:** Referenciada por Doctor

### 4. **Doctor** (`Doctor.java` + `DoctorDAO.java`)
- **Campos:** `id`, `userProfileId`, `specialtyId`, `licenseNumber`, `firstName`, `lastName`
- **Propósito:** Información específica del médico
- **Relaciones:**
  - `userProfileId` → ID de UserProfile
  - `specialtyId` → ID de Specialty

### 5. **Patient** (`Patient.java` + `PatientDAONormalized.java`)
- **Campos:** `id`, `userProfileId`, `primaryDoctorId`, `firstName`, `lastName`, `birthDate`, `gender`, `height`
- **Propósito:** Información específica del paciente
- **Relaciones:**
  - `userProfileId` → ID de UserProfile
  - `primaryDoctorId` → ID de Doctor (referencia foránea)

### 6. **EmergencyContact** (`EmergencyContact.java` + `EmergencyContactDAO.java`)
- **Campos:** `id`, `patientId`, `fullName`, `phoneNumber`, `relationship`
- **Propósito:** Contactos de emergencia del paciente
- **Relaciones:** `patientId` → ID de Patient

### 7. **Allergy** (`Allergy.java` + `AllergyDAO.java`)
- **Campos:** `id`, `name`, `severity`
- **Propósito:** Catálogo de alergias
- **Relaciones:** Referenciada por PatientAllergy

### 8. **PatientAllergy** (`PatientAllergy.java` + `PatientAllergyDAO.java`)
- **Campos:** `id`, `patientId`, `allergyId`, `detectionDate`, `notes`
- **Propósito:** Tabla de unión (junction table) entre Patient y Allergy
- **Relaciones:**
  - `patientId` → ID de Patient
  - `allergyId` → ID de Allergy

### 9. **HealthMetric** (`HealthMetric.java` + `HealthMetricDAONormalized.java`)
- **Campos:** `id`, `patientId`, `timestamp`, `systolic`, `diastolic`, `heartRate`, `weight`, `bmi`, `glucoseLevel`
- **Propósito:** Métricas de salud registradas
- **Relaciones:** `patientId` → ID de Patient

### 10. **Appointment** (`Appointment.java` + `AppointmentDAO.java`)
- **Campos:** `id`, `patientId`, `doctorId`, `scheduledDatetime`, `status`, `reason`
- **Propósito:** Citas médicas
- **Relaciones:**
  - `patientId` → ID de Patient
  - `doctorId` → ID de Doctor

### 11. **Treatment** (`Treatment.java` + `TreatmentDAO.java`)
- **Campos:** `id`, `patientId`, `doctorId`, `startDate`, `endDate`, `diagnosis`
- **Propósito:** Tratamientos médicos
- **Relaciones:**
  - `patientId` → ID de Patient
  - `doctorId` → ID de Doctor

### 12. **Medication** (`Medication.java` + `MedicationDAO.java`)
- **Campos:** `id`, `genericName`, `brandName`, `manufacturer`
- **Propósito:** Catálogo de medicamentos
- **Relaciones:** Referenciada por TreatmentDetail

### 13. **TreatmentDetail** (`TreatmentDetail.java` + `TreatmentDetailDAO.java`)
- **Campos:** `id`, `treatmentId`, `medicationId`, `dosage`, `frequency`
- **Propósito:** Tabla de unión entre Treatment y Medication
- **Relaciones:**
  - `treatmentId` → ID de Treatment
  - `medicationId` → ID de Medication

### 14. **Recommendation** (`Recommendation.java` + `RecommendationDAONormalized.java`)
- **Campos:** `id`, `patientId`, `generatedAt`, `type`, `title`, `message`, `isRead`
- **Propósito:** Recomendaciones médicas
- **Relaciones:** `patientId` → ID de Patient

### 15. **Notification** (`Notification.java` + `NotificationDAO.java`)
- **Campos:** `id`, `userId`, `sentAt`, `message`, `isDelivered`
- **Propósito:** Notificaciones a usuarios
- **Relaciones:** `userId` → ID de UserProfile

---

## 🔐 Integración de Firebase Authentication

### Cambios en `FirebaseConnection.java`
```java
// Ahora proporciona acceso a tanto Firestore como FirebaseAuth
public Firestore getFirestore()      // Base de datos
public FirebaseAuth getAuth()         // Autenticación
```

### Flujo de Registro (Normalizado)

**Archivo:** `RegisterControllerNormalized.java`

**Proceso de dos pasos:**

1. **Crear Usuario en Firebase Auth**
   - Se utiliza `FirebaseAuth.createUser()` para crear credenciales seguras
   - Genera un `authUid` único
   - Valida email único y contraseña segura

2. **Crear UserProfile en Firestore**
   - Se crea un documento `UserProfile` con el `authUid` obtenido
   - Se vincula el usuario con su rol

3. **Crear Patient o Doctor en Firestore**
   - Se crea un documento `Patient` o `Doctor` según el rol
   - Se referencia el `userProfileId` correspondiente

```
Registro
  ↓
[Firebase Auth] → Crear Usuario → authUid
  ↓
[Firestore] → Crear UserProfile (authUid)
  ↓
[Firestore] → Crear Patient/Doctor (userProfileId)
```

### Flujo de Login (Normalizado)

**Archivo:** `LoginControllerNormalized.java`

**Proceso:**

1. **Validar en Firebase Auth**
   - Se verifica que el usuario existe en Firebase Auth
   - Se obtiene el `authUid`

2. **Buscar UserProfile en Firestore**
   - Se consulta la colección `UserProfile` por `authUid`
   - Se obtiene el `roleId`

3. **Cargar datos específicos**
   - Si es `patient`: se busca en colección `Patient`
   - Si es `doctor`: se busca en colección `Doctor`

4. **Cargar Dashboard**
   - El usuario accede al panel principal con sus datos

```
Login
  ↓
[Firebase Auth] → Validar Credenciales → authUid
  ↓
[Firestore] → Buscar UserProfile (authUid) → roleId
  ↓
[Firestore] → Buscar Patient/Doctor (userProfileId)
  ↓
Dashboard ✓
```

---

## 📁 Estructura de Carpetas Actualizada

```
src/main/java/com/itc/healthtrack/
├── models/
│   ├── Role.java                    ✓ Nueva
│   ├── UserProfile.java             ✓ Nueva
│   ├── Specialty.java               ✓ Nueva
│   ├── Doctor.java                  ✓ Nueva
│   ├── Patient.java                 ✓ Nueva
│   ├── EmergencyContact.java        ✓ Nueva
│   ├── Allergy.java                 ✓ Nueva
│   ├── PatientAllergy.java          ✓ Nueva
│   ├── HealthMetric.java            ✓ Nueva
│   ├── Appointment.java             ✓ Nueva
│   ├── Treatment.java               ✓ Nueva
│   ├── Medication.java              ✓ Nueva
│   ├── TreatmentDetail.java         ✓ Nueva
│   ├── Recommendation.java          ✓ Actualizada (ahora usa Long)
│   └── Notification.java            ✓ Nueva
│
├── dao/
│   ├── RoleDAO.java                 ✓ Nueva
│   ├── UserProfileDAO.java          ✓ Nueva
│   ├── SpecialtyDAO.java            ✓ Nueva
│   ├── DoctorDAO.java               ✓ Nueva
│   ├── PatientDAONormalized.java    ✓ Nueva
│   ├── EmergencyContactDAO.java     ✓ Nueva
│   ├── AllergyDAO.java              ✓ Nueva
│   ├── PatientAllergyDAO.java       ✓ Nueva
│   ├── HealthMetricDAONormalized.java ✓ Nueva
│   ├── AppointmentDAO.java          ✓ Nueva
│   ├── TreatmentDAO.java            ✓ Nueva
│   ├── MedicationDAO.java           ✓ Nueva
│   ├── TreatmentDetailDAO.java      ✓ Nueva
│   ├── RecommendationDAONormalized.java ✓ Nueva
│   └── NotificationDAO.java         ✓ Nueva
│
├── controllers/
│   ├── LoginControllerNormalized.java       ✓ Nueva
│   ├── RegisterControllerNormalized.java    ✓ Nueva
│   └── ... (otros controladores)
│
└── config/
    └── FirebaseConnection.java              ✓ Actualizada (FirebaseAuth)
```

---

## 🔄 Características del DAO Genérico

Todos los DAOs implementan las operaciones CRUD básicas:

```java
// CREATE - Crear un nuevo registro
public void create(Entity entity) throws Exception

// READ - Obtener por ID
public Entity getById(String id) throws Exception

// READ - Obtener todos
public List<Entity> getAll() throws Exception

// READ - Obtener con filtros
public List<Entity> getBy(String filterField, String filterValue) throws Exception

// UPDATE - Actualizar un registro
public void update(Entity entity) throws Exception

// DELETE - Eliminar un registro
public void delete(String id) throws Exception
```

**Ejemplo:**
```java
RoleDAO roleDAO = new RoleDAO();
Role role = roleDAO.getRoleById("patient");
roleDAO.updateRole(role);
roleDAO.deleteRole("patient");
```

---

## 📝 Guía de Uso

### Regreso (Backward Compatibility)

Los controladores antiguos (`LoginController`, `RegisterController`, etc.) siguen funcionando, pero se recomienda migrar a las versiones normalizadas:
- `LoginControllerNormalized` - Nuevo login con Firebase Auth
- `RegisterControllerNormalized` - Nuevo registro con flujo de dos pasos

### Migraciones Futuras

Para migrar datos existentes a la nueva estructura:

1. Importar datos del viejo `User.java` al nuevo `UserProfile` + `Patient`/`Doctor`
2. Generar `authUid` para usuarios existentes usando Firebase Admin SDK
3. Crear registros de roles en la colección `Role`
4. Actualizar referencias en todas las colecciones

---

## ✅ Validación

El proyecto compila y construye exitosamente:
- ✓ 15 Model Classes
- ✓ 15 DAO Classes
- ✓ FirebaseConnection actualizado
- ✓ RegisterControllerNormalized implementado
- ✓ LoginControllerNormalized implementado
- ✓ Guardfile actualizado con Firebase Admin SDK
- ✓ Configuración de Java 21 y JavaFX 21

---

## 🚀 Próximas Acciones

1. **Actualizar FXML** - Vincular los nuevos controladores a los archivos FXML
2. **Migrar datos** - Transferir datos existentes a la nueva estructura
3. **Pruebas** - Validar el flujo completo de registro y login
4. **Optimización** - Agregar índices Firestore para consultas frecuentes

---

**Creado:** Mayo 2026  
**Versión:** 2.0 - Normalizada  
**Estado:** ✓ Compilación Exitosa

