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

            id: base?.id ?? 0,
            idMain: base?.idMain ?? 0,
            dlit: base?.dlit ?? formData.duration ?? 0,

            tip: formData.tip ?? base?.tip ?? 1,
            reg: formData.reg ?? base?.reg ?? 0,

            prMsu1: formData.msu1Config?.prMsu ?? 0,
            vd1Msu1: formData.msu1Config?.vd1 ?? 0,
            vd2Msu1: formData.msu1Config?.vd2 ?? 0,
            vd3Msu1: formData.msu1Config?.vd3 ?? 0,
            ik4Msu1: formData.msu1Config?.ik4 ?? 0,
            ik5Msu1: formData.msu1Config?.ik5 ?? 0,
            ik6Msu1: formData.msu1Config?.ik6 ?? 0,
            ik7Msu1: formData.msu1Config?.ik7 ?? 0,
            ik8Msu1: formData.msu1Config?.ik8 ?? 0,
            ik9Msu1: formData.msu1Config?.ik9 ?? 0,
            ik10Msu1: formData.msu1Config?.ik10 ?? 0,

            prMsu2: formData.msu2Config?.prMsu ?? 0,
            vd1Msu2: formData.msu2Config?.vd1 ?? 0,
            vd2Msu2: formData.msu2Config?.vd2 ?? 0,
            vd3Msu2: formData.msu2Config?.vd3 ?? 0,
            ik4Msu2: formData.msu2Config?.ik4 ?? 0,
            ik5Msu2: formData.msu2Config?.ik5 ?? 0,
            ik6Msu2: formData.msu2Config?.ik6 ?? 0,
            ik7Msu2: formData.msu2Config?.ik7 ?? 0,
            ik8Msu2: formData.msu2Config?.ik8 ?? 0,
            ik9Msu2: formData.msu2Config?.ik9 ?? 0,
            ik10Msu2: formData.msu2Config?.ik10 ?? 0,

            prBssd: formData.prBssd ?? 0,
            prZg: formData.prZg ?? 0,
            prOtklZgBssd: formData.prOtklZg ?? 0
        };
    }

/**
     * Маппинг из MsuConfig (msu1Config/msu2Config) → msuData
     * Используется в creators.ts
     */
    static fromMsuConfigs(
        msu1Config: { prMsu: number; vd1: number; vd2: number; vd3: number; ik4: number; ik5: number; ik6: number; ik7: number; ik8: number; ik9: number; ik10: number },
        msu2Config: { prMsu: number; vd1: number; vd2: number; vd3: number; ik4: number; ik5: number; ik6: number; ik7: number; ik8: number; ik9: number; ik10: number },
        base: { id?: number; idMain?: number; tip?: number; reg?: number; dlit?: number; prBssd?: number; prZg?: number; prOtklZgBssd?: number }
    ) {
        return {
            id: base.id ?? 0,
            idMain: base.idMain ?? 0,
            tip: base.tip ?? 1,
            reg: base.reg ?? 0,
            dlit: base.dlit ?? 0,

            prMsu1: msu1Config.prMsu || 0,
            vd1Msu1: msu1Config.vd1 || 0,
            vd2Msu1: msu1Config.vd2 || 0,
            vd3Msu1: msu1Config.vd3 || 0,
            ik4Msu1: msu1Config.ik4 || 0,
            ik5Msu1: msu1Config.ik5 || 0,
            ik6Msu1: msu1Config.ik6 || 0,
            ik7Msu1: msu1Config.ik7 || 0,
            ik8Msu1: msu1Config.ik8 || 0,
            ik9Msu1: msu1Config.ik9 || 0,
            ik10Msu1: msu1Config.ik10 || 0,

            prMsu2: msu2Config.prMsu || 0,
            vd1Msu2: msu2Config.vd1 || 0,
            vd2Msu2: msu2Config.vd2 || 0,
            vd3Msu2: msu2Config.vd3 || 0,
            ik4Msu2: msu2Config.ik4 || 0,
            ik5Msu2: msu2Config.ik5 || 0,
            ik6Msu2: msu2Config.ik6 || 0,
            ik7Msu2: msu2Config.ik7 || 0,
            ik8Msu2: msu2Config.ik8 || 0,
            ik9Msu2: msu2Config.ik9 || 0,
            ik10Msu2: msu2Config.ik10 || 0,

            prBssd: base.prBssd ?? 0,
            prZg: base.prZg ?? 0,
            prOtklZgBssd: base.prOtklZgBssd ?? 0
        };
    }
}