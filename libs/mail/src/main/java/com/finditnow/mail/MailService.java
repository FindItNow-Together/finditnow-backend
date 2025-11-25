package com.finditnow.mail;

import com.finditnow.config.Config;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class MailService {
    private final Session session;
    private final String from;

    public MailService() {
        String userName = Config.get("EMAIL_APP_USERNAME", "example@example.com");
        String userPwd = Config.get("EMAIL_APP_PWD", "very_secure_app_password");

        this.session = MailConfig.createSession(userName, userPwd);
        this.from = userName;
    }

    public void sendMail(String to, String subject, String body) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            message.setContent(body, "text/html; charset=utf-8");

            if(!"development".equals(Config.get("ENVIRONMENT", "development"))){
                Transport.send(message);
            };

            System.out.println("Mail sent successfully to " + to);

        } catch (MessagingException e) {
            System.err.println("Mail communication failed:: REASON: " + e.getMessage());
        }
    }
}