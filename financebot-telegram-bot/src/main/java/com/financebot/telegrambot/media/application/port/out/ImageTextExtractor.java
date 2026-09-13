package com.financebot.telegrambot.media.application.port.out;

import java.io.InputStream;

public interface ImageTextExtractor {

    String extract(InputStream content, String contentType);
}
