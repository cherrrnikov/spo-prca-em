export interface TimeInterval {
    id: string;
    mode: string;
    startTime: string;
    endTime: string;
    city: string;
    color: string;
    title?: string;
    description?: string;
}

export interface WorkMode {
    id: string;
    label: string;
    order: string;
}

export interface City {
    id: string;
    name: string;
    color: string;
}