export interface CustomerCode {
    value: number;
    label: string;
}

export interface WorkMode {
    id: number;
    label: string;
    order: string;
}

export interface City {
    id: string;
    name: string;
    color: string;
}

export interface Ppi {
    id: number;
    name: string;
    numPpi: number;
}

export interface ZgOption {
    value: number;
    label: string;
}

export interface MsuConfig {
    prMsu: number;
    prVdMsu: number;
    prIkMsu: number;
    vd1: number;
    vd2: number;
    vd3: number;
    ik4: number;
    ik5: number;
    ik6: number;
    ik7: number;
    ik8: number;
    ik9: number;
    ik10: number;
}