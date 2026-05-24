package com.itc.healthtrack.services;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;

import java.util.ArrayList;
import java.util.List;

// centraliza la logica para obtener la lista de pacientes visibles para cada usuario
public class UserService {

    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    // devuelve los pacientes que el usuario logeado puede ver
    // si es admin ve todos y si es medico solo ve los suyos
    public List<User> getPatientsForUser(User viewer) throws Exception {
        List<User> allPatients = userDao.getByField("role", "patient");
        List<User> visiblePatients = new ArrayList<>();

        for (User patient : allPatients) {
            if ("admin".equals(viewer.getRole())) {
                // el admin tiene acceso a todos
                visiblePatients.add(patient);
            } else if (viewer.getUid() != null
                    && viewer.getUid().equals(patient.getAssignedDoctorId())) {
                // el medico solo ve sus propios pacientes
                visiblePatients.add(patient);
            }
        }

        return visiblePatients;
    }
}
