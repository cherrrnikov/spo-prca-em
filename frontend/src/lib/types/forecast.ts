export interface ForecastDto {
    id: number;
    dn: string;
    dk: string;
    n_ka: number;
    n_init: number;
}

export interface ShadowDto {
    id: number;
    n_rec: number;
    d_t_in: string;
    d_t_out: string;
    duration: number;
}

export interface ZasvetkaDto {
    id: number;
    n_rec: number;
    d_t_in: string;
    d_t_out: string;
    duration: number;
}

export interface ForecastDataResponse {
    forecast: ForecastDto;
    shadows: ShadowDto[];
    zasvetki: ZasvetkaDto[];
}

export interface ForecastData {
    main: ForecastDto;
    shadows: ShadowDto[];
    zasvetki: ZasvetkaDto[];
    total_intervals: number;
}

export interface ShadowInterval {
    id: string;
    type: 'shadow';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}

export interface ZasvetkaInterval {
    id: string;
    type: 'zasvetka';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}