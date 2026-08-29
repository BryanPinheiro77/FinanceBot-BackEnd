package com.financebot.telegram.application.usecase;

import com.financebot.account.domain.Account;
import com.financebot.telegram.dto.response.TelegramDefaultAccountResponse;
import com.financebot.telegram.exception.TelegramUserNotFoundException;
import com.financebot.telegram.service.TelegramAccountResolverService;
import com.financebot.user.domain.User;
import com.financebot.user.dto.response.TelegramUserProfileResponse;
import com.financebot.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TelegramUserApplicationUseCase {

    private final UserRepository userRepository;
    private final TelegramAccountResolverService accountResolver;

    @Transactional(readOnly = true)
    public TelegramUserProfileResponse getProfile(Long telegramId) {
        return toProfile(findUser(telegramId));
    }

    @Transactional
    public TelegramUserProfileResponse updateMonthlyIncome(Long telegramId, java.math.BigDecimal income) {
        User user = findUser(telegramId);
        user.setMonthlyBaseIncome(income);
        return toProfile(userRepository.save(user));
    }

    @Transactional
    public void disconnect(Long telegramId) {
        User user = findUser(telegramId);
        user.setTelegramId(null);
        user.setTelegramLinkCode(null);
        user.setTelegramLinkCodeExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public TelegramDefaultAccountResponse getDefaultAccount(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado para este Telegram."));
        Account account = accountResolver.getOrCreateDefaultAccount(user);
        return new TelegramDefaultAccountResponse(account.getId(), account.getName());
    }

    private User findUser(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(TelegramUserNotFoundException::new);
    }

    private TelegramUserProfileResponse toProfile(User user) {
        return new TelegramUserProfileResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getMonthlyBaseIncome(), user.getTelegramId()
        );
    }
}
