package com.financebot.telegrambot.ai.application.port.out;

import com.financebot.telegrambot.ai.application.model.AiInterpretation;

import java.util.Optional;

public interface AiInterpretationPort {

    Optional<AiInterpretation> interpret(String message);
}
