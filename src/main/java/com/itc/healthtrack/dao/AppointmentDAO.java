package com.itc.healthtrack.dao;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.itc.healthtrack.config.FirebaseConnection;
import com.itc.healthtrack.models.Appointment;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object para la entidad Appointment
 * Gestiona operaciones CRUD para citas médicas
 */
public class AppointmentDAO {
    private final Firestore firestore = FirebaseConnection.getInstance().getFirestore();
    private static final String COLLECTION_NAME = "Appointment";

    public void createAppointment(Appointment appointment) throws Exception {
        if (appointment == null || appointment.getId() == null) {
            throw new IllegalArgumentException("La cita y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(appointment.getId()).set(appointment).get();
    }

    public Appointment getAppointmentById(String appointmentId) throws Exception {
        if (appointmentId == null || appointmentId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return firestore.collection(COLLECTION_NAME).document(appointmentId).get().get().toObject(Appointment.class);
    }

    public List<Appointment> getAppointmentsByPatient(String patientId) throws Exception {
        if (patientId == null || patientId.isEmpty()) {
            throw new IllegalArgumentException("El ID del paciente no puede ser nulo");
        }
        List<Appointment> appointments = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("patientId", patientId)
                .orderBy("scheduledDatetime", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Appointment appointment = querySnapshot.getDocuments().get(i).toObject(Appointment.class);
            appointments.add(appointment);
        }
        return appointments;
    }

    public List<Appointment> getAppointmentsByDoctor(String doctorId) throws Exception {
        if (doctorId == null || doctorId.isEmpty()) {
            throw new IllegalArgumentException("El ID del doctor no puede ser nulo");
        }
        List<Appointment> appointments = new ArrayList<>();
        QuerySnapshot querySnapshot = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("doctorId", doctorId)
                .orderBy("scheduledDatetime", Query.Direction.DESCENDING)
                .get()
                .get();
        for (int i = 0; i < querySnapshot.getDocuments().size(); i++) {
            Appointment appointment = querySnapshot.getDocuments().get(i).toObject(Appointment.class);
            appointments.add(appointment);
        }
        return appointments;
    }

    public void updateAppointment(Appointment appointment) throws Exception {
        if (appointment == null || appointment.getId() == null) {
            throw new IllegalArgumentException("La cita y su ID no pueden ser nulos");
        }
        firestore.collection(COLLECTION_NAME).document(appointment.getId()).set(appointment).get();
    }

    public void deleteAppointment(String appointmentId) throws Exception {
        if (appointmentId == null || appointmentId.isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        firestore.collection(COLLECTION_NAME).document(appointmentId).delete().get();
    }
}

