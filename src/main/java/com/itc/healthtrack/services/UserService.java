package com.itc.healthtrack.services;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    // DAO para acceder a la colección "users" en Firestore
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    /**
     * Devuelve la lista de pacientes que el usuario dado puede ver.
     *
     * Reglas:
     *   - Admin: ve todos los pacientes del sistema.
     *   - Médico: ve solo los pacientes asignados a él.
     *
     * @param viewer el usuario que solicita la lista (médico o admin)
     * @return lista de pacientes visibles para ese usuario
     * @throws Exception si ocurre un error al leer Firestore
     */
    public List<User> getPatientsForUser(User viewer) throws Exception {
        // Obtener todos los usuarios con rol de paciente
        List<User> allPatients = userDao.getByField("role", "patient");
        List<User> visiblePatients = new ArrayList<>();

        for (User patient : allPatients) {
            if ("admin".equals(viewer.getRole())) {
                // El administrador tiene acceso a todos los pacientes
                visiblePatients.add(patient);
            } else if (viewer.getUid() != null
                    && viewer.getUid().equals(patient.getAssignedDoctorId())) {
                // El médico solo ve los pacientes que tiene asignados
                visiblePatients.add(patient);
            }
        }

        return visiblePatients;
    }
}
