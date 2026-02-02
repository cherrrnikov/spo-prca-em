export class CityService {
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
}