package com.solarl.nado.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Ответ на запрос раскрытия контакта продавца.
 * Расширяемый контракт: можно добавить аудит, маскирование, rate-limit без ломки API.
 */
@Data
@Builder
public class PhoneResponse {
    /** Полный номер телефона (если раскрыт) или null */
    private String phone;

    /** Маскированная версия номера, например "+7 *** ** 45" */
    private String masked;

    /** true если номер раскрыт полностью */
    private boolean revealed;
}
