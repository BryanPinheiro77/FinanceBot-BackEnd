package com.financebot.telegrambot.media.adapter.out.telegram;

import com.financebot.telegrambot.media.application.exception.MediaExtractionException;
import com.financebot.telegrambot.media.application.port.out.TelegramFileDownloader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class TelegramApiFileDownloader implements TelegramFileDownloader {

    private final TelegramClient telegramClient;

    @Override
    public InputStream download(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new MediaExtractionException("O identificador do arquivo é obrigatório");
        }

        try {
            var file = telegramClient.execute(new GetFile(fileId));
            return telegramClient.downloadFileAsStream(file);
        } catch (Exception exception) {
            throw new MediaExtractionException("Não foi possível baixar o arquivo do Telegram", exception);
        }
    }
}
