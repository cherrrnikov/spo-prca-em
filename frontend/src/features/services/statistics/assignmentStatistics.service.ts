import type { OperatorData, PpiAssignment } from '$lib/types';

export class AssignmentStatisticsService {
    static getAssignmentStatistics(
        operatorData: OperatorData,
        ppiAssignments: PpiAssignment[]
    ) {
        const kvdCount = operatorData.kvd_list?.length || 0;
        const tnpCount = operatorData.tnp_list?.length || 0;
        const tsCount = operatorData.ts_list?.length || 0;
        const onaCount = operatorData.ona_list?.length || 0;
        
        const kvdWithPpi = ppiAssignments.filter(a => a.recordType === 'kvd').length;
        const tnpWithPpi = ppiAssignments.filter(a => a.recordType === 'tnp').length;
        const tsWithPpi = ppiAssignments.filter(a => a.recordType === 'ts').length;
        const onaWithPpi = ppiAssignments.filter(a => a.recordType === 'ona').length;
        
        return {
            total: kvdCount + tnpCount + tsCount,
            kvd: { total: kvdCount, withPpi: kvdWithPpi },
            tnp: { total: tnpCount, withPpi: tnpWithPpi },
            ts: { total: tsCount, withPpi: tsWithPpi },
            ona: {total: onaCount, withPpi: onaWithPpi}
        };
    }
}