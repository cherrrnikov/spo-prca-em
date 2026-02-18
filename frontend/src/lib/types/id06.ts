export interface Id06MainDto {
    id: number;
    n_ka: number;      
    d_np: string;  
    data_zap: string;  
    rnf: number;
    n_sp: number;        
    dsf: string;
    k_zajv: number;      
    n_form_id: number; 
    used: number;
}

export interface Id06KvdDto {
    id: number; 
    id_main: number;  
    dn: string;
    dk: string;
    pr_msu: number;    
    pr_bssd: number;    
    pr_zg: number;     
}

export interface Id06OnaDto {
    id: number;
    id_main: number;     
    dn: string;
    dk: string;
    n_ona: number;       
    dlit: number;
}

export interface Id06TnpDto {
    id: number;
    id_main: number;     
    dn: string;
    dk: string;
    dlit: number;
}

export interface Id06TsDto {
    id: number;
    id_main: number;
    dn: string;
    dk: string;
    tip: number;
    reg: number;
    pr_msu1: number;
    pr_vd_msu1: number;
    pr_ik_msu1: number;
    pr_vd1_1: number;       
    pr_vd2_1: number;
    pr_vd3_1: number;
    pr_ik4_1: number;
    pr_ik5_1: number;
    pr_ik6_1: number;
    pr_ik7_1: number;
    pr_ik8_1: number;
    pr_ik9_1: number;
    pr_ik10_1: number;
    pr_msu2: number;
    pr_vd_msu2: number;
    pr_ik_msu2: number;
    pr_vd1_2: number;
    pr_vd2_2: number;
    pr_vd3_2: number;
    pr_ik4_2: number;
    pr_ik5_2: number;
    pr_ik6_2: number;
    pr_ik7_2: number;
    pr_ik8_2: number;
    pr_ik9_2: number;
    pr_ik10_2: number;
    pr_otkl_zg: number;
}

export interface OperatorData {
    main: Id06MainDto;
    kvd_list: Id06KvdDto[];
    tnp_list: Id06TnpDto[];
    ts_list: Id06TsDto[];
    ona_list: Id06OnaDto[];
    total_intervals: number;
}