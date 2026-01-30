import type {
    CreatedProgramData,
    CreateProgramRequest,
    ForecastData,
    Id06TsDto,
    OperatorData,
    PpiAssignment,
    ProgramModeData,
    ShadowInterval,
    TimeInterval,
    TsMsuConfig,
    WorkMode,
    ZasvetkaInterval
} from '$lib/types/schedule';
import type { ScheduleStatus } from '../schedule-creation/types';

export class ScheduleCreationService {
    static async loadOperatorData(date: string): Promise<OperatorData> {
        const response = await fetch(`/api/schedule/proxy?date=${date}`);

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет данных для выбранной даты");
            }
            throw new Error(`Ошибка сервера: ${response.status}`);
        }

        return await response.json();
    }

    static async saveProgram(programData: CreateProgramRequest): Promise<any> {
        const response = await fetch(`/api/programs/create`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(programData)
        });

        if (!response.ok) {
            throw new Error(`Ошибка сохранения: ${response.status}`);
        }

        return await response.json();
    }

    static prepareProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ) : CreateProgramRequest {
        const numRp = this.generateProgramNumber();
        const numKa = operatorData.main.nKa;

        const mainData = {
            numRp,
            numKa,
            dateOn: `${selectedDate}T${selectedTime}:00`,
            dateOff: this.calculateDateOff(operatorData, selectedDate, selectedTime),
            typeRp: scheduleStatus === 'main' ? 3 : 5,
            prOtpr: 0
        };

        const modes: ProgramModeData[] = [];

        if (operatorData.kvdList && operatorData.kvdList.length > 0) {
            for (const kvd of operatorData.kvdList) {
                const assignment = ppiAssignments.find(a => a.recordId === kvd.id && a.recordType === 'kvd');

                if (assignment) {
                    modes.push({
                        numRp,
                        numKa,
                        dateOn: kvd.dn,
                        dateOff: kvd.dk,
                        kodMode: 7,
                        numPpi: assignment.ppiNum,
                        dlit: this.calculateDuration(kvd.dn, kvd.dk),
                        kvdData: {
                            id: kvd.id,
                            idMain: kvd.idMain,
                            dn: kvd.dn,
                            dk: kvd.dk,
                            prMsu: kvd.prMsu,
                            prBssd: kvd.prBssd,
                            prZg: kvd.prZg
                        }
                    })
                }
            }
        }

        if (operatorData.tnpList && operatorData.tnpList.length > 0) {
            for (const tnp of operatorData.tnpList) {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === tnp.id && a.recordType === 'tnp'
                );
                
                if (assignment) {
                    modes.push({
                        numRp,
                        numKa,
                        dateOn: tnp.dn,
                        dateOff: tnp.dk,
                        kodMode: 4,
                        numPpi: assignment.ppiNum,
                        dlit: tnp.dlit,
                        tnpData: {
                            id: tnp.id,
                            idMain: tnp.idMain,
                            dn: tnp.dn,
                            dk: tnp.dk,
                            dlit: tnp.dlit,
                        }
                    });
                }
            }
        }
        
        if (operatorData.tsList && operatorData.tsList.length > 0) {
            for (const ts of operatorData.tsList) {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === ts.id && a.recordType === 'ts'
                );
                
                if (assignment) {
                    // Для ТС отправляем ОДНУ запись с полным интервалом
                    // Разбивка на подынтервалы делается только для отображения
                    modes.push({
                        numRp,
                        numKa,
                        dateOn: ts.dn,
                        dateOff: ts.dk,
                        kodMode: 8, // Код режима для ТС
                        numPpi: assignment.ppiNum,
                        dlit: this.calculateDuration(ts.dn, ts.dk),
                        zakazchik: this.getCustomerLabel(operatorData.main?.kZajv || 5),
                        tsData: {
                            id: ts.id,
                            idMain: ts.idMain,
                            dn: ts.dn,
                            dk: ts.dk,
                            tip: ts.tip,
                            reg: ts.reg,
                            prMsu1: ts.prMsu1,
                            prVdMsu1: ts.prVdMsu1,
                            prIkMsu1: ts.prIkMsu1,
                            prVd1_1: ts.prVd1_1,
                            prVd2_1: ts.prVd2_1,
                            prVd3_1: ts.prVd3_1,
                            prIk4_1: ts.prIk4_1,
                            prIk5_1: ts.prIk5_1,
                            prIk6_1: ts.prIk6_1,
                            prIk7_1: ts.prIk7_1,
                            prIk8_1: ts.prIk8_1,
                            prIk9_1: ts.prIk9_1,
                            prIk10_1: ts.prIk10_1,
                            prMsu2: ts.prMsu2,
                            prVdMsu2: ts.prVdMsu2,
                            prIkMsu2: ts.prIkMsu2,
                            prVd1_2: ts.prVd1_2,
                            prVd2_2: ts.prVd2_2,
                            prVd3_2: ts.prVd3_2,
                            prIk4_2: ts.prIk4_2,
                            prIk5_2: ts.prIk5_2,
                            prIk6_2: ts.prIk6_2,
                            prIk7_2: ts.prIk7_2,
                            prIk8_2: ts.prIk8_2,
                            prIk9_2: ts.prIk9_2,
                            prIk10_2: ts.prIk10_2,
                            prOtklZg: ts.prOtklZg
                        }
                    });
                }
            }
        }
        
        console.log("Подготовленная ПРЦА: ", mainData);
        console.log("Подготовленные записи режимов ПРЦА: ", modes);

        return { mainData, modes };
    }

    static getCustomerLabel(code: number): string {
        const customerLabels: Record<number, string> = {
            1: 'Заказчик 1',
            2: 'Заказчик 2',
            3: 'Заказчик 3',
            4: 'Заказчик 4',
            5: 'Заказчик 5'
        };
        return customerLabels[code] || 'Неизвестный заказчик';
    }

    static generateProgramNumber(): number {
        return Math.floor(Date.now() / 1000);
    }

    static calculateDateOff(
        operatorData: OperatorData,
        selectedDate: string,
        selectedTime: string
    ): string {
        let latestDate = new Date(`${selectedDate}T${selectedTime}:00`);

        const allRecords = [
            ...(operatorData.kvdList || []),
            ...(operatorData.tnpList || []),
            ...(operatorData.tsList || [])
        ];

        allRecords.forEach(record => {
            const endDate = new Date(record.dk);
            if (endDate > latestDate) {
                latestDate = endDate;
            }
        });

        return latestDate.toISOString();
    }

    static calculateDuration(startStr: string, endStr: string): number {
        const start = new Date(startStr);
        const end = new Date(endStr);
        return Math.floor((end.getTime() - start.getTime()) / 1000);
    }

    static getAssignmentStatistics(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[]
    ) {
        const kvdCount = operatorData.kvdList?.length || 0;
        const tnpCount = operatorData.tnpList?.length || 0;
        const tsCount = operatorData.tsList?.length || 0;
        
        const kvdWithPpi = ppiAssignments.filter(a => a.recordType === 'kvd').length;
        const tnpWithPpi = ppiAssignments.filter(a => a.recordType === 'tnp').length;
        const tsWithPpi = ppiAssignments.filter(a => a.recordType === 'ts').length;
        
        return {
            total: kvdCount + tnpCount + tsCount,
            kvd: { total: kvdCount, withPpi: kvdWithPpi },
            tnp: { total: tnpCount, withPpi: tnpWithPpi },
            ts: { total: tsCount, withPpi: tsWithPpi }
        };
    }

    static formatDateTime(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleString('ru-RU', {
                day: '2-digit',
                month: '2-digit',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static formatTimeOnly(dateStr: string): string {
        try {
            const date = new Date(dateStr);
            return date.toLocaleTimeString('ru-RU', {
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch {
            return dateStr;
        }
    }

    static convertToTimeIntervals(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        workModes: WorkMode[]
    ): TimeInterval[] {
        const intervals: TimeInterval[] = [];
        
        if (operatorData.kvdList && operatorData.kvdList.length > 0) {
            operatorData.kvdList.forEach(kvd => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === kvd.id && a.recordType === 'kvd'
                );
                
                if (assignment) {
                    intervals.push({
                        id: `kvd_${kvd.id}`,
                        mode: 7, // Калибр. ВД
                        startTime: this.formatTimeFromISO(kvd.dn),
                        endTime: this.formatTimeFromISO(kvd.dk),
                        city: this.getCityByPpi(assignment.ppiNum),
                        color: this.getColorByPpi(assignment.ppiNum),
                        title: `Калибровка ВД (ППИ ${assignment.ppiNum})`,
                        description: `Калибровка ВД, ID: ${kvd.id}`,
                        ppi: assignment.ppiNum,
                        dlit: this.calculateDuration(kvd.dn, kvd.dk),
                        customerCode: operatorData.main?.kZajv || 5,
                        hasConflict: false,
                        conflictWith: [],
                        nearZasvetka: false,
                        zasvetkaConflict: false,
                        zasvetkaDistance: 0,
                        willBeSaved: true
                    });
                }
            });
        }
        
        if (operatorData.tnpList && operatorData.tnpList.length > 0) {
            operatorData.tnpList.forEach(tnp => {
                const assignment = ppiAssignments.find(a => 
                    a.recordId === tnp.id && a.recordType === 'tnp'
                );
                
                if (assignment) {
                    intervals.push({
                        id: `tnp_${tnp.id}`,
                        mode: 4, // Режимы ТНП
                        startTime: this.formatTimeFromISO(tnp.dn),
                        endTime: this.formatTimeFromISO(tnp.dk),
                        city: this.getCityByPpi(assignment.ppiNum),
                        color: this.getColorByPpi(assignment.ppiNum),
                        title: `ТНП (ППИ ${assignment.ppiNum})`,
                        description: `Режим ТНП, длительность: ${tnp.dlit} сек`,
                        ppi: assignment.ppiNum,
                        dlit: tnp.dlit,
                        customerCode: operatorData.main?.kZajv || 5,
                        hasConflict: false,
                        conflictWith: [],
                        nearZasvetka: false,
                        zasvetkaConflict: false,
                        zasvetkaDistance: 0,
                        willBeSaved: true
                    });
                }
            });
        }
        
        if (operatorData.tsList && operatorData.tsList.length > 0) {
            operatorData.tsList.forEach(ts => {
                const tsSubIntervals = this.convertTsToSubIntervals(ts, ppiAssignments);
                intervals.push(...tsSubIntervals);
            });
        }
        
        return intervals.map(interval => ({
            ...interval,
            msu1Vd: interval.msu1Vd || [],
            msu2Vd: interval.msu2Vd || [],
            msu1Config: interval.msu1Config || this.getDefaultMsuConfig(),
            msu2Config: interval.msu2Config || this.getDefaultMsuConfig(),
            customerCode: interval.customerCode || operatorData.main?.kZajv || 1,
            hasConflict: false,
            conflictWith: [],
            nearZasvetka: false,
            zasvetkaConflict: false,
            zasvetkaDistance: 0,
            willBeSaved: true
        }));
    }

    static convertTsToSubIntervals(
        tsRecord: Id06TsDto,
        ppiAssignments: PpiAssignment[]
    ): TimeInterval[] {
        const subIntervals: TimeInterval[] = [];
        
        const assignment = ppiAssignments.find(
            a => a.recordId === tsRecord.id && a.recordType === 'ts'
        );
        
        const ppiNum = assignment?.ppiNum || 1;
        const city = this.getCityByPpi(ppiNum);
        const color = this.getColorByPpi(ppiNum);
        
        const stepMinutes = tsRecord.tip === 0 ? 30 : 15;
        const subIntervalDuration = 7; 
        
        const startDate = new Date(tsRecord.dn);
        const endDate = new Date(tsRecord.dk);
        
        let currentTime = new Date(startDate);
        
        // Генерируем подынтервалы до тех пор, пока следующий интервал не выйдет за границы
        while (currentTime.getTime() + (subIntervalDuration * 60000) <= endDate.getTime()) {
            const subStartTime = new Date(currentTime);
            const subEndTime = new Date(subStartTime.getTime() + (subIntervalDuration * 60000));
            
            const formatTime = (date: Date): string => {
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                return `${hours}:${minutes}`;
            };
            
            const subInterval: TimeInterval = {
                id: `ts_${tsRecord.id}_${formatTime(subStartTime)}`,
                mode: 8, 
                startTime: formatTime(subStartTime),
                endTime: formatTime(subEndTime),
                city,
                color,
                title: `Техн. съёмка (tip=${tsRecord.tip}, ППИ ${ppiNum})`,
                description: `Технологическая съемка, тип: ${tsRecord.tip}, режим: ${tsRecord.reg}`,
                ppi: ppiNum,
                dlit: subIntervalDuration * 60, 
                customerCode: 5, 
                
                msu1Config: {
                    prMsu: tsRecord.prMsu1 || 0,
                    prVdMsu: tsRecord.prVdMsu1 || 0,
                    prIkMsu: tsRecord.prIkMsu1 || 0,
                    vd1: tsRecord.prVd1_1 || 0,
                    vd2: tsRecord.prVd2_1 || 0,
                    vd3: tsRecord.prVd3_1 || 0,
                    ik4: tsRecord.prIk4_1 || 0,
                    ik5: tsRecord.prIk5_1 || 0,
                    ik6: tsRecord.prIk6_1 || 0,
                    ik7: tsRecord.prIk7_1 || 0,
                    ik8: tsRecord.prIk8_1 || 0,
                    ik9: tsRecord.prIk9_1 || 0,
                    ik10: tsRecord.prIk10_1 || 0
                },
                msu2Config: {
                    prMsu: tsRecord.prMsu2 || 0,
                    prVdMsu: tsRecord.prVdMsu2 || 0,
                    prIkMsu: tsRecord.prIkMsu2 || 0,
                    vd1: tsRecord.prVd1_2 || 0,
                    vd2: tsRecord.prVd2_2 || 0,
                    vd3: tsRecord.prVd3_2 || 0,
                    ik4: tsRecord.prIk4_2 || 0,
                    ik5: tsRecord.prIk5_2 || 0,
                    ik6: tsRecord.prIk6_2 || 0,
                    ik7: tsRecord.prIk7_2 || 0,
                    ik8: tsRecord.prIk8_2 || 0,
                    ik9: tsRecord.prIk9_2 || 0,
                    ik10: tsRecord.prIk10_2 || 0
                },
                hasConflict: false,
                conflictWith: [],
                willBeSaved: true,
                nearZasvetka: false,
                zasvetkaConflict: false,
                zasvetkaDistance: 0
            };
            
            subIntervals.push(subInterval);
            
            // Переходим к следующему временному слоту
            // Добавляем stepMinutes минут к текущему времени
            currentTime.setMinutes(currentTime.getMinutes() + stepMinutes);
        }
        
        console.log(`ТС запись ${tsRecord.id} разбита на ${subIntervals.length} подынтервалов (шаг: ${stepMinutes} мин)`);
        return subIntervals;
    }

    static getDefaultMsuConfig(): TsMsuConfig {
        return {
            prMsu: 0,
            prVdMsu: 0,
            prIkMsu: 0,
            vd1: 0,
            vd2: 0,
            vd3: 0,
            ik4: 0,
            ik5: 0,
            ik6: 0,
            ik7: 0,
            ik8: 0,
            ik9: 0,
            ik10: 0
        };
    }

    static formatTimeFromISO(isoString: string): string {
        try {
            const date = new Date(isoString);
            return date.toTimeString().substring(0, 5); 
        } catch {
            return "00:00";
        }
    }

    static getCityByPpi(ppiNum: number): string {
        const ppiToCity: Record<number, string> = {
            1: 'moscow',
            2: 'novosibirsk',
            3: 'vladivostok',
            4: 'moscow2',
            5: 'novosibirsk2',
            6: 'vladivostok2',
            7: 'moscow',
            8: 'novosibirsk',
            9: 'vladivostok',
            10: 'moscow2'
        };
        return ppiToCity[ppiNum] || 'moscow';
    }

    static getColorByPpi(ppiNum: number): string {
        const ppiToColor: Record<number, string> = {
            1: '#f4fc0a',
            2: '#b80afc',
            3: '#0afcf4', 
            4: '#593315', 
            5: '#152359',
            6: '#78866b', 
            7: '#6110b3', 
            8: '#6197c9',
            9: '#1a5216', 
            10: '#24f016' 
        };
        return ppiToColor[ppiNum] || '#4299e1';
    }

    static async loadForecastData(date: string): Promise<ForecastData> {
        const response = await fetch(`/api/schedule/forecast-proxy?date=${date}`);

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error("Нет прогнозных данных для выбранной даты");
            }
            throw new Error(`Ошибка сервера: ${response.status}`);
        }

        const responseData = await response.json();
        
        return {
            main: responseData.forecast,
            shadows: responseData.shadows,
            zasvetki: responseData.zasvetki,
            totalIntervals: responseData.shadows.length + responseData.zasvetki.length
        };
    }

    static convertForecastToIntervals(forecastData: ForecastData): {
        shadows: ShadowInterval[],
        zasvetki: ZasvetkaInterval[]
    } {
        return {
            shadows: forecastData.shadows.map(shadow => ({
                id: `shadow_${shadow.id}`,
                type: 'shadow',
                startTime: this.formatTimeFromISO(shadow.dTIn),
                endTime: this.formatTimeFromISO(shadow.dTOut),
                duration: shadow.duration,
                title: 'Тень',
                color: 'rgba(83, 83, 83, 1)',
                opacity: 1,
                zIndex: 2
            })),
            zasvetki: forecastData.zasvetki.map(zasvetka => ({
                id: `zasvetka_${zasvetka.id}`,
                type: 'zasvetka',
                startTime: this.formatTimeFromISO(zasvetka.dTIn),
                endTime: this.formatTimeFromISO(zasvetka.dTOut),
                duration: zasvetka.duration,
                title: 'Засветка',
                color: 'rgba(175, 175, 175, 1)',
                opacity: 1,
                zIndex: 1
            }))
        };
    }

    static prepareFullProgramData(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[],
        createdPrograms: CreatedProgramData[],
        selectedDate: string,
        selectedTime: string,
        scheduleStatus: ScheduleStatus
    ): CreateProgramRequest {
        const baseRequest = this.prepareProgramData(
            operatorData,
            ppiAssignments,
            selectedDate,
            selectedTime,
            scheduleStatus
        );
        
        createdPrograms.forEach(created => {
            baseRequest.modes.push(created.modeData);
        });
        
        return baseRequest;
    }
}