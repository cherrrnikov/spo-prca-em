package ru.laspace.backend.config;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PrConfig {
    private PrConfig() {
    }

    // Дата запуска для расчёта сквозного номера планируемых суток
    public static final LocalDate LAUNCH_DATE = LocalDate.of(2026, 4, 12);

    // Номер ФО ПР01
    public static final String FORM_PR01_NUMBER = "003";

    // Номер ФО ПР03
    public static final String FORM_PR03_NUMBER = "077";

    // Номер ФО ПР04
    public static final String FORM_PR04_NUMBER = "079";

    // Номер ФО ВП01
    public static final String FORM_VP01_NUMBER = "009";

    /**
     * Считает сквозной номер планируемых суток
     * Количество суток от даты запуска до даты планируемых суток
     */
    public static long calcDayNumber(LocalDate planDate) {
        return ChronoUnit.DAYS.between(LAUNCH_DATE, planDate);
    }
}
