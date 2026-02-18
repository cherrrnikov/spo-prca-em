export interface Ro02Dto {
    id: number;
    rnf: number; 
    n_ka: number;    
    dsf: string;   
    data_n: string;  
    data_razv: string; 
    data_k: string | null;
    n_form_id: number; 
}

export interface Ro02DataResponse {
    rotations: Ro02Dto[];
    total_rotations: number;
}

export interface RotationInterval {
    id: string;
    type: 'rotation';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
}