package com.financebot.telegrambot;

import com.financebot.telegrambot.bot.FinanceTelegramBot;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		"FINANCEBOT_API_URL=http://localhost:8080",
		"financebot.api.base-url=http://localhost:8080",
		"TELEGRAM_BOT_TOKEN=test-token",
		"TELEGRAM_BOT_USERNAME=test-bot",
		"telegram.bot.token=test-token",
		"telegram.bot.username=test-bot"
})
class FinancebotTelegramBotApplicationTests {

	@MockitoBean
	private FinanceTelegramBot financeTelegramBot;

	@Test
	void contextLoads() {
	}
}