package com.researchassistant.subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRenewalReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRenewalReminderScheduler.class);

    private final SubscriptionRenewalReminderService reminderService;

    public SubscriptionRenewalReminderScheduler(SubscriptionRenewalReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "${app.subscription.renewal-reminders.cron:0 0 8 * * *}")
    public void runRenewalReminders() {
        log.info("SubscriptionRenewalReminderScheduler triggered.");
        try {
            reminderService.processRenewalRemindersAndExpirations();
        } catch (Exception ex) {
            log.error("Error running subscription renewal reminders: {}", ex.getMessage(), ex);
        }
    }
}
