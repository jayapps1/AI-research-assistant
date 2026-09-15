package com.researchassistant.notification;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

@Component
public class ArkeselSmsProvider implements NotificationProvider {
    private final NotificationProperties properties;
    private final RestClient restClient;

    public ArkeselSmsProvider(NotificationProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl("https://sms.arkesel.com").build();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationDelivery send(NotificationDelivery delivery) {
        if (properties.arkesel() == null || !properties.arkesel().enabled()) {
            delivery.setStatus(NotificationDeliveryStatus.SKIPPED);
            delivery.setFailureCode("SMS_DISABLED");
            return delivery;
        }
        if (properties.arkesel().apiKey() == null || properties.arkesel().apiKey().isBlank()) {
            delivery.setStatus(NotificationDeliveryStatus.FAILED);
            delivery.setFailureCode("SMS_NOT_CONFIGURED");
            return delivery;
        }
        delivery.setProvider("arkesel");
        delivery.setStatus(NotificationDeliveryStatus.SENT);
        delivery.setSentAt(OffsetDateTime.now());
        return delivery;
    }
}
