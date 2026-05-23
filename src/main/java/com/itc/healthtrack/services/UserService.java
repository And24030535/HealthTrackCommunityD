package com.itc.healthtrack.services;

import com.itc.healthtrack.dao.GenericDAO;
import com.itc.healthtrack.models.User;

import java.util.ArrayList;
import java.util.List;

// servicio que centraliza la logica para obtener la lista de pacientes visible para cada usuario
public class UserService {

    // dao para consultar la coleccion de usuarios en firestore
    private final GenericDAO<User> userDao = new GenericDAO<>(User.class, "users");

    // devuelve los pacientes que el usuario logeado puede ver
    // si es admin ve todos los pacientes del sistema
    // si es medico solo ve los pacientes que tiene asignados a el
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
