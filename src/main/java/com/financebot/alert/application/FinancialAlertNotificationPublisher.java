package com.financebot.alert.application;

public interface FinancialAlertNotificationPublisher {

    void publish(FinancialAlertNotificationEvent event);
}
