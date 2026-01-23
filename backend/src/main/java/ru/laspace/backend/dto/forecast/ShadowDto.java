package ru.laspace.backend.dto.forecast;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowDto {
    private Long id;
    private Long nRec;
    private LocalDateTime dTIn;
    private LocalDateTime dTOut;
    private Integer duration;
}
