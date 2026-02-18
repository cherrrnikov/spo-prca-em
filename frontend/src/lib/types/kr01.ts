export interface Kr01ImpulseDto {
    id: number;
    id_main: number;
    n_vit: number;
    date_im: string;
    dlit: number;
    pr_or: number;
    ugl_v: number;
    massa: number;
    n_du: number;
    pr_var: number;
}

export interface Kr01MainDto {
    id: number;
    rnf: number;
    n_ka: number;
    dsf: string;
    n_bc: number;
    n_zad: number;
    k_imp: number;
    dt_zap: string;
    n_form_id: number;
    used: number;
}

export interface Kr01DataResponse {
    main: Kr01MainDto;
    impulses: Kr01ImpulseDto[];
    total_impulses: number;
}

export interface VkiInterval {
    id: string;
    type: 'vki';
    startTime: string;
    endTime: string;
    duration: number;
    title: string;
    color: string;
    opacity: number;
    zIndex: number;
    impulseNumber: number;
    mass: number;
    angle: number;
    nVit: number;
    nDu: number;
    vkiType: 'vki1' | 'vki2';
}