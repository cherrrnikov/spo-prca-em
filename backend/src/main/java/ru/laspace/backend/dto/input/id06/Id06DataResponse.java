package ru.laspace.backend.dto.input.id06;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EqualsAndHashCode
@ToString
@Schema(description = "Полный набор данных из таблиц id06* для создания ПРЦА")
public class Id06DataResponse {

    @Schema(description = "Основная запись заявки")
    private Id06MainDto main;

    @Schema(description = "Список заявок на калибровку ВД")
    private List<Id06KvdDto> kvdList;

    @Schema(description = "Список заявок на режимы ТНП")
    private List<Id06TnpDto> tnpList;

    @Schema(description = "Список заявок на технологические съемки")
    private List<Id06TsDto> tsList;

    @Schema(description = "Список заявок на юстировки ОНА")
    private List<Id06OnaDto> onaList;

    @Schema(description = "Общее количество интервалов", hidden = true)
    public int getTotalIntervals() {
        int total = 0;
        if (kvdList != null)
            total += kvdList.size();
        if (tnpList != null)
            total += tnpList.size();
        if (tsList != null)
            total += tsList.size();
        if (onaList != null)
            total += onaList.size();
        return total;
    }

}