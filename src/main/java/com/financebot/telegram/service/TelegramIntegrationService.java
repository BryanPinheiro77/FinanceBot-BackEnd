package com.financebot.telegram.service;

import com.financebot.analysis.dto.response.FinancialCommitmentResponse;
import com.financebot.analysis.service.FinancialAnalysisService;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.user.domain.User;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import com.financebot.user.dto.request.UpdateMonthlyBaseIncomeRequest;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramIntegrationService {

    private final UserRepository userRepository;
    private final FinancialAnalysisService financialAnalysisService;

    @Transactional(readOnly = true)
    public TelegramUserProfileResponse getMe(Long telegramId) {
        User user = findUserByTelegramId(telegramId);

        return new TelegramUserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMonthlyBaseIncome(),
                user.getTelegramId()
        );
    }

    @Transactional
    public TelegramUserProfileResponse updateMonthlyBaseIncome(
            Long telegramId,
            UpdateMonthlyBaseIncomeRequest request
    ) {
        User user = findUserByTelegramId(telegramId);
        user.setMonthlyBaseIncome(request.monthlyBaseIncome());

        User savedUser = userRepository.save(user);

        return new TelegramUserProfileResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getMonthlyBaseIncome(),
                savedUser.getTelegramId()
        );
    }

    @Transactional
    public void disconnectTelegram(Long telegramId) {
        User user = findUserByTelegramId(telegramId);
        user.setTelegramId(null);
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public FinancialCommitmentResponse getFinancialAnalysis(Long telegramId) {
        User user = findUserByTelegramId(telegramId);
        return financialAnalysisService.getFinancialCommitment(user);
    }

    private User findUserByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(TelegramUserNotFoundException::new);
    }
}