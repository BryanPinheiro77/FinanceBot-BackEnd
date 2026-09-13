package com.financebot.telegrambot.media.application.port.out;

import java.io.InputStream;

public interface TelegramFileDownloader {

    InputStream download(String fileId);
}
