import { AstrocorrectionService } from '$lib/utils/astrocorrection.service';
import { get } from 'svelte/store';
import { ScheduleApiService } from '../../../../features/services/api/scheduleApi.service';
import { ScheduleConverterService } from '../../../../features/services/data/scheduleConverter.service';
import type { createStores } from '../stores';
import type { createValidation } from '../validation';

export function createDataLoaders(
    stores: ReturnType<typeof createStores>,
    validation: ReturnType<typeof createValidation>
) {
    const {
        userData,
        intervals,
        bortData,
        hasAstrocorrectionData,
        vkiIntervals,
        rotationIntervals,
        contextDate
    } = stores;

    const { updateAllConflicts } = validation;

    async function loadUserData() {
        try {
            const userDataCookie = document.cookie
                .split('; ')
                .find(row => row.startsWith('user_data='));
            
            if (userDataCookie) {
                const userDataStr = userDataCookie.split('=')[1];
                const parsedData = JSON.parse(decodeURIComponent(userDataStr));
                
                const user = {
                    username: parsedData.username,
                    firstName: parsedData.firstName,
                    lastName: parsedData.lastName,
                    enabled: parsedData.enabled !== undefined ? parsedData.enabled : true,
                    accountLocked: parsedData.accountLocked !== undefined ? parsedData.accountLocked : false,
                    failedAttempts: parsedData.failedAttempts || 0,
                    lastLoginAt: parsedData.lastLoginAt,
                    lastLogoutAt: parsedData.lastLogoutAt || '',
                    roles: parsedData.roles || []
                };
                
                userData.set(user);
            }
        } catch (error) {
            console.error('Error parsing user data:', error);
            userData.set(null);
        }
    }

    async function loadBortData(date: string) {
        try {
            const data = await ScheduleApiService.loadBortData(date);
            bortData.set(data);
            return data;
        } catch (error) {
            console.error('Ошибка загрузки данных ID02:', error);
            bortData.set(null);
            return null;
        }
    }

    async function loadAstroEvents(date: string) {
        try {
            const [vkiData, rotationData] = await Promise.all([
                ScheduleApiService.loadVkiData(date),
                ScheduleApiService.loadRotationData(date)
            ]);
            
            const vkiList = ScheduleConverterService.convertVkiToIntervals(vkiData);
            const rotationList = ScheduleConverterService.convertRotationToIntervals(rotationData, date);
            
            vkiIntervals.set(vkiList);
            rotationIntervals.set(rotationList);

            updateAllConflicts();
        } catch (error) {
            console.error("Ошибка загрузки событий астрокоррекции: ", error);
            vkiIntervals.set([]);
            rotationIntervals.set([]);
        }
    }

    async function checkAndAddAstrocorrection(date: string): Promise<boolean> {
        try {
            const hasAstro = await ScheduleApiService.hasAstrocorrectionData(date);
            const currentIntervals = get(intervals);
            hasAstrocorrectionData.set(hasAstro);

            if (currentIntervals.length > 0) {
                const intervalsWithAstro = AstrocorrectionService.mergeAstrocorrection(
                    currentIntervals,
                    date,
                    hasAstro
                );
                intervals.set(intervalsWithAstro);
                updateAllConflicts();
            }

            return hasAstro;
        } catch (error) {
            console.error("Ошибка при проверке астрокоррекции: ", error);
            hasAstrocorrectionData.set(false);
            return false;
        }
    }

    function setContextDate(date: string) {
        contextDate.set(date);
        loadBortData(date);
    }

    if (typeof window !== 'undefined') {
        window.addEventListener('user-data-updated', () => {
            loadUserData();
        })
    }

    return {
        loadUserData,
        loadBortData,
        loadAstroEvents,
        checkAndAddAstrocorrection,
        setContextDate
    };
}