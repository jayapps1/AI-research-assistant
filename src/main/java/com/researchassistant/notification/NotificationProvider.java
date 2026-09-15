package com.researchassistant.notification;

public interface NotificationProvider {
    NotificationChannel channel();
    NotificationDelivery send(NotificationDelivery delivery);
}
