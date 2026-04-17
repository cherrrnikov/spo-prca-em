import type { Id02Dto } from '$lib/types';

/**
 * Единый маппер для msuData (данные МСУ-ГС)
 */
export class MsuMapper {

    /**
     * Маппинг из записи ИД06 (ts_list) → msuData
     */
    static fromId06(ts: any, mainId: number, bortData: Id02Dto | null, dlit: number) {
        return {
            id: ts.id,
            idMain: mainId,

            tip: ts.tip ?? 1,
            reg: ts.reg ?? 0,
            dlit,

            prMsu1: ts.pr_msu1,
            vd1Msu1: ts.pr_vd1_1,
            vd2Msu1: ts.pr_vd2_1,
            vd3Msu1: ts.pr_vd3_1,
            ik4Msu1: ts.pr_ik4_1,
            ik5Msu1: ts.pr_ik5_1,
            ik6Msu1: ts.pr_ik6_1,
            ik7Msu1: ts.pr_ik7_1,
            ik8Msu1: ts.pr_ik8_1,
            ik9Msu1: ts.pr_ik9_1,
            ik10Msu1: ts.pr_ik10_1,

            prMsu2: ts.pr_msu2,
            vd1Msu2: ts.pr_vd1_2,
            vd2Msu2: ts.pr_vd2_2,
            vd3Msu2: ts.pr_vd3_2,
            ik4Msu2: ts.pr_ik4_2,
            ik5Msu2: ts.pr_ik5_2,
            ik6Msu2: ts.pr_ik6_2,
            ik7Msu2: ts.pr_ik7_2,
            ik8Msu2: ts.pr_ik8_2,
            ik9Msu2: ts.pr_ik9_2,
            ik10Msu2: ts.pr_ik10_2,

            prBssd: bortData?.pr_bssd ?? 0,
            prZg: bortData?.pr_zg ?? 0,
            prOtklZgBssd: ts.pr_otkl_zg
        };
    }

    /**
     * Маппинг из формы → msuData
     */
    static fromForm(formData: any, base?: any) {
        return {
            ...base,

            tip: formData.tip ?? base?.tip ?? 1,
            reg: formData.reg ?? base?.reg ?? 0,

            prMsu1: formData.prMsu1,
            vd1Msu1: formData.vd1Msu1,
            vd2Msu1: formData.vd2Msu1,
            vd3Msu1: formData.vd3Msu1,
            ik4Msu1: formData.ik4Msu1,
            ik5Msu1: formData.ik5Msu1,
            ik6Msu1: formData.ik6Msu1,
            ik7Msu1: formData.ik7Msu1,
            ik8Msu1: formData.ik8Msu1,
            ik9Msu1: formData.ik9Msu1,
            ik10Msu1: formData.ik10Msu1,

            prMsu2: formData.prMsu2,
            vd1Msu2: formData.vd1Msu2,
            vd2Msu2: formData.vd2Msu2,
            vd3Msu2: formData.vd3Msu2,
            ik4Msu2: formData.ik4Msu2,
            ik5Msu2: formData.ik5Msu2,
            ik6Msu2: formData.ik6Msu2,
            ik7Msu2: formData.ik7Msu2,
            ik8Msu2: formData.ik8Msu2,
            ik9Msu2: formData.ik9Msu2,
            ik10Msu2: formData.ik10Msu2,

            prBssd: formData.prBssd,
            prZg: formData.prZg,
            prOtklZgBssd: formData.prOtklZg
        };
    }
}