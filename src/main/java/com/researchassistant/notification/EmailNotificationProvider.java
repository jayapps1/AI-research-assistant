package com.researchassistant.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@EnableConfigurationProperties(NotificationProperties.class)
public class EmailNotificationProvider implements NotificationProvider {
    private final NotificationProperties properties;
    private final JavaMailSender mailSender;

    public EmailNotificationProvider(NotificationProperties properties, ObjectProvider<JavaMailSender> mailSender) {
        this.properties = properties;
        this.mailSender = mailSender.getIfAvailable();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationDelivery send(NotificationDelivery delivery) {
        if (properties.email() == null || !properties.email().enabled()) {
            delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
            delivery.setFailureCode("EMAIL_DISABLED");
            return delivery;
        }
        if (mailSender == null) {
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setFailureCode("EMAIL_NOT_CONFIGURED");
            return delivery;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(delivery.getNotification().getRecipient().getEmail());
        message.setFrom(properties.email().fromAddress());
        message.setSubject(delivery.getNotification().getTitle());
        message.setText(delivery.getNotification().getMessage());
        mailSender.send(message);
        delivery.setProvider("email");
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(OffsetDateTime.now());
        return delivery;
    }
}
