export class CityService {
    static getCityByPpi(ppiNum: number): string {
        const ppiToCity: Record<number, string> = {
            0: 'moscow',
            1: 'novosibirsk',
            2: 'vladivostok',
            3: 'moscow2',
            4: 'novosibirsk2',
            5: 'vladivostok2',
            6: 'moscow',
            7: 'novosibirsk',
            8: 'vladivostok',
            9: 'moscow2'
        };
        return ppiToCity[ppiNum] || 'moscow';
    }

    static getColorByPpi(ppiNum: number): string {
        const ppiToColor: Record<number, string> = {
            0: '#f4fc0a',
            1: '#b80afc',
            2: '#0afcf4',
            3: '#593315',
            4: '#152359',
            5: '#78866b',
            6: '#6110b3',
            7: '#6197c9',
            8: '#1a5216',
            9: '#24f016'
        };
        return ppiToColor[ppiNum] || '#4299e1';
    }
}