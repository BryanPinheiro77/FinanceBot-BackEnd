package com.financebot.telegrambot.media.application.port.out;

import java.io.InputStream;

public interface DocumentTextExtractor {

    boolean supports(String contentType, String fileName);

    String extract(InputStream content);
}
