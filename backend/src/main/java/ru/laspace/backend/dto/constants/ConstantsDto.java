package ru.laspace.backend.dto.constants;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstantsDto {
    private Integer ksR;
    private Integer kaR;
    private Integer cId;
    private String cName;
    private Integer cValueSec;
}
