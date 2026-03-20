package ru.laspace.backend.dto.programs;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProgramCreateRequest {
    private MainData mainData;
    private List<ModeData> modes;

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MainData {
        private Integer numRp;
        private Integer numKa;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOn;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOff;

        private Integer typeRp;
        private Integer prOtpr;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ModeData {
        private Integer numRp;
        private Integer numKa;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOn;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateOff;

        private Integer kodMode;
        private Integer numPpi;
        private Integer dlit;
        private String zakazchik;

        private KvdData kvdData;
        private TnpData tnpData;
        private TsData tsData;
        private OnaData onaData;
        private OmiData omiData;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class KvdData {
        private Long id;
        private Long idMain;
        private Integer prMsu;
        private Integer prBssd;
        private Integer prZg;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TnpData {
        private Long id;
        private Long idMain;
        private Integer prMsu;
        private Integer prBssd;
        private Integer prZg;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class TsData {
        private Long id;
        private Long idMain;
        private Integer tip;
        private Integer reg;
        private Integer dlit;
        private Integer prMsu1;
        private Integer vd1Msu1;
        private Integer vd2Msu1;
        private Integer vd3Msu1;
        private Integer ik4Msu1;
        private Integer ik5Msu1;
        private Integer ik6Msu1;
        private Integer ik7Msu1;
        private Integer ik8Msu1;
        private Integer ik9Msu1;
        private Integer ik10Msu1;
        private Integer prMsu2;
        private Integer vd1Msu2;
        private Integer vd2Msu2;
        private Integer vd3Msu2;
        private Integer ik4Msu2;
        private Integer ik5Msu2;
        private Integer ik6Msu2;
        private Integer ik7Msu2;
        private Integer ik8Msu2;
        private Integer ik9Msu2;
        private Integer ik10Msu2;
        private Integer prBssd;
        private Integer prZg;
        private Integer prOtklZgBssd;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OnaData {
        private Long id;
        private Long idMain;
        private Integer typeOmi;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dN;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dK;

        private Integer nOna;
        private Integer nPpi;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OmiData {
        private Long id;
        private Long idMain;
        private Integer numOmi;
        private Integer typeOmi;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateNach;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime dateCon;

        private Integer dlit;
    }
}
