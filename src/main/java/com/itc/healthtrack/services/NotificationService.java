package com.itc.healthtrack.services;

import com.itc.healthtrack.models.User;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Servicio asíncrono para enviar notificaciones por correo electrónico usando SMTP
public class NotificationService {

    // Configuración del servidor SMTP de Gmail
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String SYSTEM_EMAIL = "clinicahealthtrack@gmail.com";
    private static final String SYSTEM_PASSWORD = "yaih bgnl dubi ctgs";

    // Formato estándar para la marca de tiempo de los mensajes
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Método para notificar a un paciente. Se ejecuta en un hilo secundario para evitar congelar la interfaz.
    public void notifyPatient(User patient, String message) {
        new Thread(() -> {
            String subject = "HealthTrack - Alerta de Salud";
            String recipientName = patient.getFirstName() + " " + patient.getLastName();
            sendEmail(patient.getEmail(), subject, recipientName, message);
        }).start();
    }

    /**
     * Envía una recomendación médica formal al paciente por correo electrónico.
     * Solo debe invocarse cuando el usuario logueado tiene rol "doctor".
     *
     * @param patient        Paciente destinatario (debe tener email válido)
     * @param doctorFullName Nombre completo del médico que envía la recomendación
     * @param title          Título de la recomendación
     * @param message        Cuerpo de la recomendación
     */
    public void sendRecommendationEmail(User patient, String doctorFullName, String title, String message) {
        new Thread(() -> {
            String subject    = "HealthTrack - Nueva recomendación de tu médico";
            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String body = "Tu médico " + doctorFullName + " ha generado una nueva recomendación para ti:\n\n"
                    + "Título: " + title + "\n\n"
                    + "Mensaje:\n" + message;
            sendEmail(patient.getEmail(), subject, patientName, body);
        }).start();
    }

    // Método para notificar al médico encargado. También utiliza un hilo secundario independiente.
    public void notifyDoctor(User doctor, String message) {
        new Thread(() -> {
            String subject = "HealthTrack - Actualización de Paciente";
            String recipientName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            sendEmail(doctor.getEmail(), subject, recipientName, message);
        }).start();
    }

    // Método principal interno que construye y transfiere el correo a través de la red
    private void sendEmail(String toEmail, String subject, String recipientName, String messageBody) {
        // Verifica que la dirección destino sea válida antes de intentar conectar
        if (toEmail == null || toEmail.isEmpty()) {
            System.err.println("No se proporcionó correo para el destinatario: " + recipientName);
            return;
        }

        // Configuración de las propiedades de seguridad y conexión SMTP
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST);
        properties.put("mail.smtp.port", SMTP_PORT);

        // Creación de la sesión autenticada con las credenciales del sistema
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SYSTEM_EMAIL, SYSTEM_PASSWORD);
            }
        });

        try {
            // Creación del paquete de correo (mensaje MIME)
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SYSTEM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // Construcción del cuerpo del correo que verá el usuario final
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String fullMessage = "Hola " + recipientName + ",\n\n" +
                    messageBody + "\n\n" +
                    "Generado el: " + timestamp + "\n" +
                    "HealthTrack Community - OwO";

            message.setText(fullMessage);

            // Ejecuta el envío final mediante el protocolo de transporte
            Transport.send(message);
            System.out.println("Correo enviado exitosamente a: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("Fallo al enviar el correo a: " + toEmail);
            e.printStackTrace();
        }
    }
}
