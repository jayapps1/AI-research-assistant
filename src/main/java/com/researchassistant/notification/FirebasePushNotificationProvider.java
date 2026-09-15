package com.researchassistant.notification;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

@Component
public class FirebasePushNotificationProvider implements NotificationProvider {
    private final NotificationProperties properties;
    private final RestClient restClient;

    public FirebasePushNotificationProvider(NotificationProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://fcm.googleapis.com").build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationDelivery send(NotificationDelivery delivery) {
        if (properties.firebase() == null || !properties.firebase().enabled()) {
            delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
            delivery.setFailureCode("PUSH_DISABLED");
            return delivery;
        }
        delivery.setProvider("firebase");
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(OffsetDateTime.now());
        return delivery;
    }
}
