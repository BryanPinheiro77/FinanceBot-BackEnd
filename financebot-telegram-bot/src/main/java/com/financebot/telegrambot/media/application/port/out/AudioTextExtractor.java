package com.financebot.telegrambot.media.application.port.out;

import java.io.InputStream;

public interface AudioTextExtractor {

    String extract(InputStream content, String fileName, String contentType);
}
