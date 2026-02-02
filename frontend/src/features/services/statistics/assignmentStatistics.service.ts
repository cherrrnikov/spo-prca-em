import type { OperatorData, PpiAssignment } from '$lib/types/schedule';

export class AssignmentStatisticsService {
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
}