package com.solarl.nado.service;

import com.solarl.nado.config.MailNotificationProperties;
import com.solarl.nado.entity.Notification.NotificationType;
import com.solarl.nado.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * email-канал уведомлений. вызывается из NotificationService.routeToChannels().
 * отправляет только при выполнении всех условий:
 * - app.mail.enabled = true
 * - у пользователя emailVerified = true
 * - тип события входит в поддерживаемый набор
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final MailNotificationProperties mailProperties;

    // mvp: только реально полезные типы
    private static final Set<NotificationType> EMAIL_TYPES = Set.of(
            NotificationType.AD_APPROVED,
            NotificationType.AD_REJECTED,
            NotificationType.WANTED_MATCH,
            NotificationType.AUCTION_WON,
            NotificationType.AUCTION_OUTBID,
            NotificationType.AUCTION_NO_BIDS
    );

    public void trySend(User recipient, NotificationType type, Map<String, Object> payload) {
        if (!mailProperties.isEnabled()) return;
        if (!Boolean.TRUE.equals(recipient.getEmailVerified())) return;
        if (!EMAIL_TYPES.contains(type)) return;
        if (recipient.getEmail() == null || recipient.getEmail().isBlank()) return;

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getFrom());
            message.setTo(recipient.getEmail());
            message.setSubject(buildSubject(type, payload));
            message.setText(buildBody(type, payload));
            mailSender.send(message);
            log.info("EMAIL_SENT: userId={}, type={}", recipient.getId(), type);
        } catch (Exception e) {
            // не валим основной flow при ошибке smtp
            log.warn("EMAIL_FAILED: userId={}, type={}, error={}",
                    recipient.getId(), type, e.getMessage());
        }
    }

    private String buildSubject(NotificationType type, Map<String, Object> payload) {
        String title = String.valueOf(payload.getOrDefault("adTitle", ""));
        switch (type) {
            case AD_APPROVED:
                return "Nado: ваше объявление одобрено";
            case AD_REJECTED:
                return "Nado: объявление отклонено";
            case WANTED_MATCH:
                return "Nado: найдено совпадение с вашим запросом";
            case AUCTION_WON:
                return "Nado: вы выиграли аукцион — " + title;
            case AUCTION_OUTBID:
                return "Nado: вашу ставку перебили — " + title;
            case AUCTION_NO_BIDS:
                return "Nado: аукцион завершился без ставок";
            default:
                return "Nado: уведомление";
        }
    }

    private String buildBody(NotificationType type, Map<String, Object> payload) {
        String title = String.valueOf(payload.getOrDefault("adTitle", ""));
        String reason = String.valueOf(payload.getOrDefault("reason", ""));

        switch (type) {
            case AD_APPROVED:
                return "Ваше объявление «" + title + "» прошло модерацию и опубликовано.\n\n"
                        + "Перейти: https://nado.ru/ads/" + payload.getOrDefault("adId", "");
            case AD_REJECTED:
                return "Ваше объявление «" + title + "» отклонено модератором.\n"
                        + "Причина: " + reason + "\n\n"
                        + "Вы можете исправить и отправить повторно.";
            case WANTED_MATCH:
                return "По вашему запросу «Хочу купить» найдено подходящее объявление: " + title + ".\n\n"
                        + "Перейти: https://nado.ru/ads/" + payload.getOrDefault("adId", "");
            case AUCTION_WON:
                return "Поздравляем! Вы выиграли аукцион по объявлению «" + title + "».\n"
                        + "Свяжитесь с продавцом для завершения сделки.";
            case AUCTION_OUTBID:
                return "Вашу ставку на «" + title + "» перебили.\n"
                        + "Вы можете сделать новую ставку.";
            case AUCTION_NO_BIDS:
                return "Ваш аукцион по объявлению «" + title + "» завершился без ставок.\n"
                        + "Вы можете продлить его или перевести в обычную продажу.";
            default:
                return "У вас новое уведомление на платформе Nado.";
        }
    }
}
