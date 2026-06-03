package com.financebot.telegrambot;

import com.financebot.telegrambot.config.TelegramBotProperties;
import com.financebot.telegrambot.conversation.config.TelegramConversationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        TelegramBotProperties.class,
        TelegramConversationProperties.class
})
public class FinancebotTelegramBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancebotTelegramBotApplication.class, args);
    }
}
