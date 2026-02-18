import type {
    ForecastData,
    Kr01DataResponse,
    Kr01ImpulseDto,
    Ro02DataResponse,
    Ro02Dto,
    RotationInterval,
    ShadowInterval,
    TsMsuConfig,
    VkiInterval,
    ZasvetkaInterval
} from '$lib/types';
import { TimeUtils } from '$lib/utils/time';

export class ScheduleConverterService {
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

    static convertForecastToIntervals(forecastData: ForecastData): {
        shadows: ShadowInterval[],
        zasvetki: ZasvetkaInterval[]
    } {
        return {
            shadows: forecastData.shadows.map(shadow => ({
                id: `shadow_${shadow.id}`,
                type: 'shadow',
                startTime: TimeUtils.extractTimeFromTimestamp(shadow.d_t_in),
                endTime: TimeUtils.extractTimeFromTimestamp(shadow.d_t_out),
                duration: shadow.duration,
                title: 'Тень',
                color: 'rgba(83, 83, 83, 1)',
                opacity: 1,
                zIndex: 2
            })),
            zasvetki: forecastData.zasvetki.map(zasvetka => ({
                id: `zasvetka_${zasvetka.id}`,
                type: 'zasvetka',
                startTime: TimeUtils.extractTimeFromTimestamp(zasvetka.d_t_in),
                endTime: TimeUtils.extractTimeFromTimestamp(zasvetka.d_t_out),
                duration: zasvetka.duration,
                title: 'Засветка',
                color: 'rgba(175, 175, 175, 1)',
                opacity: 1,
                zIndex: 1
            }))
        };
    }

    static convertVkiToIntervals(vkiData: Kr01DataResponse | null): VkiInterval[] {
        if (!vkiData?.impulses || vkiData.impulses.length === 0) {
            return [];
        }

        return vkiData.impulses.map((impulse: Kr01ImpulseDto, index: number) => {
            const time = TimeUtils.extractTimeFromTimestamp(impulse.date_im);
            const date = impulse.date_im.split('T')[0];

            const vkiType = impulse.n_du >= 1 && impulse.n_du <= 4 ? 'vki1' : 'vki2';
            const duration = impulse.dlit || 300;
            const endTime = TimeUtils.calculateEndTime(time, duration / 60);
            
            return {
                id: `vki-${date}-${index + 1}`,
                type: 'vki',
                startTime: time,
                endTime: endTime,
                duration: duration,
                title: vkiType === 'vki1' ? 'ВКИ1' : 'ВКИ2',
                color: '#000000',
                opacity: 1,
                zIndex: 1,
                impulseNumber: index + 1,
                mass: impulse.massa,
                angle: impulse.ugl_v,
                nVit: impulse.n_vit,
                nDu: impulse.n_du,
                vkiType: vkiType
            };
        });
    }

    static convertRotationToIntervals(rotationData: Ro02DataResponse | null, targetDate: string): RotationInterval[] {
        if (!rotationData?.rotations || rotationData.rotations.length === 0) {
            return [];
        }

        return rotationData.rotations
            .filter((rotation: Ro02Dto) => {
                const rotationDate = rotation.data_razv.split('T')[0];
                return rotationDate === targetDate;
            })
            .map((rotation: Ro02Dto, index: number) => {
                const time = TimeUtils.extractTimeFromTimestamp(rotation.data_razv);
                const date = rotation.data_razv.split('T')[0];
                const duration = 300;
                const endTime = TimeUtils.calculateEndTime(time, duration / 60);
                
                return {
                    id: `rotation-${date}-${index + 1}`,
                    type: 'rotation',
                    startTime: time,
                    endTime: endTime,
                    duration: duration,
                    title: `Сезонный разворот`,
                    color: '#000000',
                    opacity: 1,
                    zIndex: 1,
                    rotationNumber: index + 1
                };
            });
    }
}