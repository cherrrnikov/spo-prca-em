package ru.laspace.backend.dto.schedule;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Schema(description = "Общее количество интервалов", hidden = true)
    public int getTotalIntervals() {
        int total = 0;
        if (kvdList != null)
            total += kvdList.size();
        if (tnpList != null)
            total += tnpList.size();
        if (tsList != null)
            total += tsList.size();
        return total;
    }

}