package ru.laspace.backend.dto.forecast;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDto {
    private Long id;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dn;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dk;
    private Integer nKa;
    private Integer nInit;
}
