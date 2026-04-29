-- ============================================================
-- BACKEND SERVICE — полная схема БД
-- V1: начальное состояние на момент внедрения Flyway
-- ============================================================

-- ===== ВХОДНЫЕ ДАННЫЕ (созданы вручную, не управляются Hibernate) =====

CREATE TABLE IF NOT EXISTS form_in (
    id BIGSERIAL PRIMARY KEY,
    ident_n INTEGER NOT NULL,
    content TEXT NOT NULL,
    prasp INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_form_in_ident_n ON form_in (ident_n);

INSERT INTO form_in (id, ident_n, content, prasp)
SELECT 1, 16,
    E'НУ04:1523,11,0,10,1:016;\n1.1523,0,9;\n2.25.03.2026,0;\n3.18.43.54.781;\n4.4.21648075820192e+04;\n5.4.03192402396074e-06;\n6.4.3303034096213e+01;\n7.0.264391446802152e-03;\n8.3.07465163923183e+00;\n9.8.53195785529424e-05;\n10.0.0;\n11.0.0;\n12.161600300,14;\n13.0.0;\nС334285.',
    1
WHERE NOT EXISTS (SELECT 1 FROM form_in WHERE id = 1);

-- ---

CREATE TABLE IF NOT EXISTS t_constants (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ks_r INT NOT NULL,
    ka_r INT NOT NULL,
    c_id INT NOT NULL,
    c_name TEXT NOT NULL,
    c_value_sec INT NOT NULL,
    CONSTRAINT chk_t_constants_ka_r CHECK (ka_r > 0),
    CONSTRAINT chk_t_constants_c_id CHECK (c_id > 0)
);

INSERT INTO t_constants (ks_r, ka_r, c_id, c_name, c_value_sec)
SELECT v.ks_r, v.ka_r, v.c_id, v.c_name, v.c_value_sec
FROM (VALUES
    (1, 5, 1, '(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)_(Коррекция орбиты - ВКИ1)', 4820),
    (1, 5, 2, '(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)_(Коррекция орбиты - ВКИ2)', 1810),
    (1, 5, 3, '(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)_(Сезонный разворот)', 3900),
    (1, 5, 6, '(Астрокоррекции)_(Шт./Уч. съемки,МЦИ,ТНП,КВД,ТС,Юст.МСУ,Юст.ОНА)', 300),
    (1, 5, 7, '(Шт./Уч. съемки,ТНП,КВД,ТС,Юст.ОНА)_(Астрокоррекции)', -10),
    (1, 5, 10, '(МЦИ, ТС,  Съемки)_(ОМИ)', 60),
    (1, 5, 11, '(ОМИ)_(МЦИ,ТС, Съемки)', 60),
    (1, 5, 14, '(Начало тени)_(Начало шт. съемки)', 300),
    (1, 5, 15, '(Конец съемки)_(Конец тени)', 300),
    (1, 5, 16, '(Коррекция орбиты - ВКИ1)_(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)', 6250),
    (1, 5, 17, '(Коррекция орбиты - ВКИ2)_(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)', 1210),
    (1, 5, 34, '(Шт. съемка)_(Уч. съемка)', 1260),
    (1, 5, 38, '(Шт./Уч. съемки,ТНП,КВД,ТС,Юст.МСУ,Юст.ОНА)_(Засветки)', 60),
    (1, 5, 42, '(Уч. съемка)_(Шт. съемка)', 1260),
    (1, 5, 48, '(Сезонный разворот)_(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)', 5700),
    (1, 5, 77, '(Юст.ОНА)_(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС)', 60),
    (1, 5, 78, '(Шт./Уч. съемки,ОМИ,ТНП,КВД,ТС,Юст.ОНА)_(Юст.ОНА)', 60),
    (1, 5, 81, '(Засветки)_(Шт./Уч. съемки,ТНП,КВД,ТС,Юст.МСУ,Юст.ОНА)', 300),
    (1, 5, 82, '(ППИ недоступен)_', 0),
    (1, 5, 87, 'Длительность калибровки ВД', 420),
    (1, 5, 88, 'Длительность режима ТНП', 516),
    (1, 5, 89, 'Длительность технологической съемки', 420),
    (1, 5, 90, 'Длительность учащенной съемки МСУ-ГС', 420),
    (1, 5, 91, 'Длительность штатной съемки МСУ-ГС', 420),
    (1, 5, 92, 'Длительность ОМИ', 720)
) AS v(ks_r, ka_r, c_id, c_name, c_value_sec)
WHERE NOT EXISTS (SELECT 1 FROM t_constants LIMIT 1);

-- ---

CREATE TABLE IF NOT EXISTS t_forecast (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dn TIMESTAMP NOT NULL,
    dk TIMESTAMP NOT NULL,
    n_ka INT NOT NULL,
    n_init INT NOT NULL,
    CONSTRAINT chk_t_forecast_time CHECK (dk > dn),
    CONSTRAINT chk_t_forecast_n_ka CHECK (n_ka > 0),
    CONSTRAINT chk_t_forecast_n_init CHECK (n_init > 0)
);

CREATE TABLE IF NOT EXISTS t_shadow (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    n_rec INT,
    d_t_in TIMESTAMP NOT NULL,
    d_t_out TIMESTAMP NOT NULL,
    duration INTERVAL,
    CONSTRAINT chk_t_shadow_valid_time CHECK (d_t_out > d_t_in)
);

CREATE TABLE IF NOT EXISTS t_zasvetka (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    n_rec INT,
    d_t_in TIMESTAMP NOT NULL,
    d_t_out TIMESTAMP NOT NULL,
    duration INTERVAL,
    CONSTRAINT chk_t_zasvetka_valid_time CHECK (d_t_out > d_t_in)
);

-- ---

CREATE TABLE IF NOT EXISTS id02 (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rnf INT NOT NULL,
    n_ka INT NOT NULL,
    d_np DATE NOT NULL,
    n_sp INT NOT NULL,
    dsf DATE NOT NULL,
    data_zap DATE NOT NULL,
    i_msu1 INT,
    i_vd_1 INT,
    i_ik_1 INT,
    vd1_1 INT,
    vd2_1 INT,
    vd3_1 INT,
    ik4_1 INT,
    ik5_1 INT,
    ik6_1 INT,
    ik7_1 INT,
    ik8_1 INT,
    ik9_1 INT,
    ik10_1 INT,
    i_msu2 INT,
    i_vd_2 INT,
    i_ik_2 INT,
    vd1_2 INT,
    vd2_2 INT,
    vd3_2 INT,
    ik4_2 INT,
    ik5_2 INT,
    ik6_2 INT,
    ik7_2 INT,
    ik8_2 INT,
    ik9_2 INT,
    ik10_2 INT,
    pr_bssd INT,
    bssd1 INT,
    bssd2 INT,
    pr_zg INT,
    pr_otkl_zg INT,
    CONSTRAINT chk_i_msu1 CHECK (i_msu1 IN (0, 1)),
    CONSTRAINT chk_i_vd_1 CHECK (i_vd_1 IN (0, 1)),
    CONSTRAINT chk_i_ik_1 CHECK (i_ik_1 IN (0, 1)),
    CONSTRAINT chk_vd1_1 CHECK (vd1_1 IN (0, 1)),
    CONSTRAINT chk_vd2_1 CHECK (vd2_1 IN (0, 1)),
    CONSTRAINT chk_vd3_1 CHECK (vd3_1 IN (0, 1)),
    CONSTRAINT chk_ik4_1 CHECK (ik4_1 IN (0, 1)),
    CONSTRAINT chk_ik5_1 CHECK (ik5_1 IN (0, 1)),
    CONSTRAINT chk_ik6_1 CHECK (ik6_1 IN (0, 1)),
    CONSTRAINT chk_ik7_1 CHECK (ik7_1 IN (0, 1)),
    CONSTRAINT chk_ik8_1 CHECK (ik8_1 IN (0, 1)),
    CONSTRAINT chk_ik9_1 CHECK (ik9_1 IN (0, 1)),
    CONSTRAINT chk_ik10_1 CHECK (ik10_1 IN (0, 1)),
    CONSTRAINT chk_i_msu2 CHECK (i_msu2 IN (0, 1)),
    CONSTRAINT chk_i_vd_2 CHECK (i_vd_2 IN (0, 1)),
    CONSTRAINT chk_i_ik_2 CHECK (i_ik_2 IN (0, 1)),
    CONSTRAINT chk_vd1_2 CHECK (vd1_2 IN (0, 1)),
    CONSTRAINT chk_vd2_2 CHECK (vd2_2 IN (0, 1)),
    CONSTRAINT chk_vd3_2 CHECK (vd3_2 IN (0, 1)),
    CONSTRAINT chk_ik4_2 CHECK (ik4_2 IN (0, 1)),
    CONSTRAINT chk_ik5_2 CHECK (ik5_2 IN (0, 1)),
    CONSTRAINT chk_ik6_2 CHECK (ik6_2 IN (0, 1)),
    CONSTRAINT chk_ik7_2 CHECK (ik7_2 IN (0, 1)),
    CONSTRAINT chk_ik8_2 CHECK (ik8_2 IN (0, 1)),
    CONSTRAINT chk_ik9_2 CHECK (ik9_2 IN (0, 1)),
    CONSTRAINT chk_ik10_2 CHECK (ik10_2 IN (0, 1)),
    CONSTRAINT chk_pr_bssd CHECK (pr_bssd IN (0, 1)),
    CONSTRAINT chk_bssd1 CHECK (bssd1 IN (0, 1)),
    CONSTRAINT chk_bssd2 CHECK (bssd2 IN (0, 1)),
    CONSTRAINT chk_pr_zg CHECK (pr_zg IN (0, 1)),
    CONSTRAINT chk_pr_otkl_zg CHECK (pr_otkl_zg IN (0, 1)),
    CONSTRAINT chk_id02_dates_valid CHECK (d_np <= dsf AND dsf <= data_zap),
    CONSTRAINT chk_id02_ka_number CHECK (n_ka > 0),
    CONSTRAINT chk_id02_rnf CHECK (rnf > 0),
    CONSTRAINT chk_id02_n_sp CHECK (n_sp > 0)
);

-- ---

CREATE TABLE IF NOT EXISTS id06 (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rnf INT NOT NULL,
    n_ka INT NOT NULL,
    d_np DATE NOT NULL,
    n_sp INT NOT NULL,
    dsf DATE NOT NULL,
    k_zajv INT NOT NULL DEFAULT 0,
    data_zap TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    n_form_id INT,
    used INT DEFAULT 0,
    CONSTRAINT chk_id06_used CHECK (used IN (0, 1)),
    CONSTRAINT chk_id06_k_zajv CHECK (k_zajv >= 0),
    CONSTRAINT chk_id06_n_ka CHECK (n_ka > 0),
    CONSTRAINT chk_id06_rnf CHECK (rnf > 0),
    CONSTRAINT chk_id06_n_sp CHECK (n_sp > 0),
    CONSTRAINT chk_id06_dates_valid CHECK (d_np <= dsf AND dsf <= data_zap::DATE)
);

CREATE TABLE IF NOT EXISTS id06_kvd (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_main INT NOT NULL,
    dn TIMESTAMP NOT NULL,
    dk TIMESTAMP NOT NULL,
    pr_msu INT,
    pr_bssd INT,
    pr_zg INT,
    CONSTRAINT chk_id06_kvd_time CHECK (dk > dn),
    CONSTRAINT chk_id06_kvd_pr_msu CHECK (pr_msu IN (0, 1)),
    CONSTRAINT chk_id06_kvd_pr_bssd CHECK (pr_bssd IN (0, 1)),
    CONSTRAINT chk_id06_kvd_pr_zg CHECK (pr_zg IN (0, 1)),
    CONSTRAINT fk_id06_kvd_to_id06 FOREIGN KEY (id_main) REFERENCES id06 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS id06_tnp (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_main INT NOT NULL,
    dn TIMESTAMP NOT NULL,
    dk TIMESTAMP NOT NULL,
    dlit INT NOT NULL,
    CONSTRAINT chk_id06_tnp_time CHECK (dk > dn),
    CONSTRAINT chk_id06_tnp_dlit CHECK (dlit > 0),
    CONSTRAINT fk_id06_tnp_to_id06 FOREIGN KEY (id_main) REFERENCES id06 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS id06_ts (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_main INT NOT NULL,
    dn TIMESTAMP NOT NULL,
    dk TIMESTAMP NOT NULL,
    tip INT NOT NULL,
    reg INT NOT NULL,
    pr_msu1 INT,
    pr_vd_msu1 INT,
    pr_ik_msu1 INT,
    pr_vd1_1 INT,
    pr_vd2_1 INT,
    pr_vd3_1 INT,
    pr_ik4_1 INT,
    pr_ik5_1 INT,
    pr_ik6_1 INT,
    pr_ik7_1 INT,
    pr_ik8_1 INT,
    pr_ik9_1 INT,
    pr_ik10_1 INT,
    pr_msu2 INT,
    pr_vd_msu2 INT,
    pr_ik_msu2 INT,
    pr_vd1_2 INT,
    pr_vd2_2 INT,
    pr_vd3_2 INT,
    pr_ik4_2 INT,
    pr_ik5_2 INT,
    pr_ik6_2 INT,
    pr_ik7_2 INT,
    pr_ik8_2 INT,
    pr_ik9_2 INT,
    pr_ik10_2 INT,
    pr_otkl_zg INT,
    CONSTRAINT chk_id06_ts_time CHECK (dk > dn),
    CONSTRAINT chk_id06_ts_pr_msu1 CHECK (pr_msu1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd_msu1 CHECK (pr_vd_msu1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik_msu1 CHECK (pr_ik_msu1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd1_1 CHECK (pr_vd1_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd2_1 CHECK (pr_vd2_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd3_1 CHECK (pr_vd3_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik4_1 CHECK (pr_ik4_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik5_1 CHECK (pr_ik5_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik6_1 CHECK (pr_ik6_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik7_1 CHECK (pr_ik7_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik8_1 CHECK (pr_ik8_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik9_1 CHECK (pr_ik9_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik10_1 CHECK (pr_ik10_1 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_msu2 CHECK (pr_msu2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd_msu2 CHECK (pr_vd_msu2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik_msu2 CHECK (pr_ik_msu2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd1_2 CHECK (pr_vd1_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd2_2 CHECK (pr_vd2_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_vd3_2 CHECK (pr_vd3_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik4_2 CHECK (pr_ik4_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik5_2 CHECK (pr_ik5_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik6_2 CHECK (pr_ik6_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik7_2 CHECK (pr_ik7_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik8_2 CHECK (pr_ik8_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik9_2 CHECK (pr_ik9_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_ik10_2 CHECK (pr_ik10_2 IN (0, 1)),
    CONSTRAINT chk_id06_ts_pr_otkl_zg CHECK (pr_otkl_zg IN (0, 1)),
    CONSTRAINT fk_id06_ts_to_id06 FOREIGN KEY (id_main) REFERENCES id06 (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS id06_ona (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_main INT NOT NULL,
    dn TIMESTAMP NOT NULL,
    dk TIMESTAMP NOT NULL,
    n_ona INT NOT NULL,
    dlit INT NOT NULL,
    CONSTRAINT chk_id06_ona_n_ona CHECK (n_ona IN (1, 2)),
    CONSTRAINT chk_id06_ona_time_valid CHECK (dk > dn),
    CONSTRAINT chk_id06_ona_dlit_positive CHECK ((dk - dn) > INTERVAL '0 seconds'),
    CONSTRAINT fk_id06_ona_to_id06 FOREIGN KEY (id_main) REFERENCES id06 (id) ON DELETE CASCADE
);

-- ---

CREATE TABLE IF NOT EXISTS kr01_main (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rnf INT,
    n_ka INT,
    dsf TIMESTAMP,
    n_bc INT,
    n_zad INT,
    k_imp INT,
    dt_zap TIMESTAMP,
    n_form_id INT,
    used INT DEFAULT 0,
    CONSTRAINT chk_kr01_main_n_bc CHECK (n_bc IN (1, 2, 3)),
    CONSTRAINT chk_kr01_main_used CHECK (used IN (0, 1)),
    CONSTRAINT chk_kr01_main_k_imp CHECK (k_imp >= 0),
    CONSTRAINT chk_kr01_main_n_ka CHECK (n_ka > 0),
    CONSTRAINT chk_kr01_main_rnf CHECK (rnf > 0),
    CONSTRAINT chk_kr01_main_n_zad CHECK (n_zad > 0),
    CONSTRAINT chk_kr01_main_dates_valid CHECK (dsf <= dt_zap)
);

CREATE TABLE IF NOT EXISTS kr01 (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_main INT NOT NULL,
    n_vit INT,
    date_im TIMESTAMP NOT NULL,
    dlit INT NOT NULL,
    pr_or SMALLINT,
    ugl_v DOUBLE PRECISION,
    massa DOUBLE PRECISION,
    n_du INT,
    pr_var SMALLINT,
    CONSTRAINT chk_kr01_pr_or CHECK (pr_or IN (0, 1)),
    CONSTRAINT chk_kr01_n_du CHECK (n_du BETWEEN 1 AND 6),
    CONSTRAINT chk_kr01_pr_var CHECK (pr_var IN (0, 1)),
    CONSTRAINT chk_kr01_dlit CHECK (dlit > 0),
    CONSTRAINT chk_kr01_massa CHECK (massa > 0),
    CONSTRAINT chk_kr01_n_vit CHECK (n_vit > 0),
    CONSTRAINT fk_kr01_to_kr01_main FOREIGN KEY (id_main) REFERENCES kr01_main (id) ON DELETE CASCADE
);

-- ---

CREATE TABLE IF NOT EXISTS ro_02 (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rnf INT NOT NULL,
    dsf TIMESTAMP NOT NULL,
    n_ka INT NOT NULL,
    data_n TIMESTAMP NOT NULL,
    data_razv TIMESTAMP NOT NULL,
    data_k TIMESTAMP,
    n_form_id INT,
    CONSTRAINT chk_ro_02_rnf CHECK (rnf > 0),
    CONSTRAINT chk_ro_02_n_ka CHECK (n_ka > 0),
    CONSTRAINT chk_ro_02_dates_valid CHECK (data_n <= data_razv AND (data_k IS NULL OR data_razv <= data_k)),
    CONSTRAINT chk_ro_02_dsf_valid CHECK (dsf >= data_n),
    CONSTRAINT chk_ro_02_data_razv_valid CHECK (data_razv >= data_n)
);

-- ===== PROGRAMS (управляются Hibernate) =====

CREATE TABLE IF NOT EXISTS programs_main (
    id BIGSERIAL PRIMARY KEY,
    num_rp INTEGER NOT NULL,
    num_ka INTEGER NOT NULL,
    date_on TIMESTAMP,
    date_off TIMESTAMP,
    type_rp INTEGER,
    pr_otpr INTEGER,
    CONSTRAINT programs_main_unique UNIQUE (num_rp, num_ka)
);

CREATE TABLE IF NOT EXISTS programs_mode (
    id BIGSERIAL PRIMARY KEY,
    id_programs_main BIGINT REFERENCES programs_main (id),
    num_rp INTEGER,
    num_ka INTEGER,
    date_on TIMESTAMP,
    date_off TIMESTAMP,
    kod_mode INTEGER,
    num_ppi INTEGER,
    dlit INTEGER,
    zakazchik VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS programs_mode_msu (
    id BIGSERIAL PRIMARY KEY,
    id_programs_mode BIGINT REFERENCES programs_mode (id),
    tip INTEGER,
    reg INTEGER,
    dlit INTEGER,
    pr_msu1 INTEGER,
    pr_vd_msu1 INTEGER,
    pr_ik_msu1 INTEGER,
    vd1_msu1 INTEGER,
    vd2_msu1 INTEGER,
    vd3_msu1 INTEGER,
    ik4_msu1 INTEGER,
    ik5_msu1 INTEGER,
    ik6_msu1 INTEGER,
    ik7_msu1 INTEGER,
    ik8_msu1 INTEGER,
    ik9_msu1 INTEGER,
    ik10_msu1 INTEGER,
    pr_msu2 INTEGER,
    pr_vd_msu2 INTEGER,
    pr_ik_msu2 INTEGER,
    vd1_msu2 INTEGER,
    vd2_msu2 INTEGER,
    vd3_msu2 INTEGER,
    ik4_msu2 INTEGER,
    ik5_msu2 INTEGER,
    ik6_msu2 INTEGER,
    ik7_msu2 INTEGER,
    ik8_msu2 INTEGER,
    ik9_msu2 INTEGER,
    ik10_msu2 INTEGER,
    pr_bssd INTEGER,
    pr_zg INTEGER,
    pr_otkl_zg_bssd INTEGER
);

CREATE TABLE IF NOT EXISTS programs_mode_kvd (
    id BIGSERIAL PRIMARY KEY,
    id_programs_mode BIGINT REFERENCES programs_mode (id),
    pr_msu INTEGER,
    pr_bssd INTEGER,
    pr_zg INTEGER
);

CREATE TABLE IF NOT EXISTS programs_mode_omi (
    id BIGSERIAL PRIMARY KEY,
    id_programs_mode BIGINT REFERENCES programs_mode (id),
    num_omi INTEGER,
    type_omi INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    dlit INTEGER
);

CREATE TABLE IF NOT EXISTS programs_mode_ona (
    id BIGSERIAL PRIMARY KEY,
    id_programs_mode BIGINT REFERENCES programs_mode (id),
    d_n TIMESTAMP,
    d_k TIMESTAMP,
    n_ona INTEGER,
    n_ppi INTEGER
);

CREATE TABLE IF NOT EXISTS programs_ona (
    id BIGSERIAL PRIMARY KEY,
    id_programs BIGINT REFERENCES programs_main (id),
    n_ona INTEGER,
    d_n TIMESTAMP,
    d_k TIMESTAMP,
    n_ppi INTEGER,
    type_mode INTEGER
);

-- ===== VP01 (управляются Hibernate) =====

CREATE TABLE IF NOT EXISTS vp01 (
    id BIGSERIAL PRIMARY KEY,
    num_ka INTEGER,
    dsf TIMESTAMP,
    num_rp INTEGER,
    rnf INTEGER,
    data_zap TIMESTAMP,
    k INTEGER DEFAULT 0,
    d INTEGER DEFAULT 0,
    s INTEGER DEFAULT 0,
    p INTEGER DEFAULT 0,
    dt_n_rp TIMESTAMP,
    dt_k_rp TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vp01_msu (
    id BIGSERIAL PRIMARY KEY,
    id_pr BIGINT REFERENCES vp01 (id),
    kod_reg INTEGER,
    num_msu INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    complect_msu1 INTEGER,
    vd11 INTEGER,
    vd12 INTEGER,
    vd13 INTEGER,
    ik14 INTEGER,
    ik15 INTEGER,
    ik16 INTEGER,
    ik17 INTEGER,ik18 INTEGER,
    ik19 INTEGER,
    ik110 INTEGER,
    complect_msu2 INTEGER,
    vd21 INTEGER,
    vd22 INTEGER,
    vd23 INTEGER,
    ik24 INTEGER,
    ik25 INTEGER,
    ik26 INTEGER,
    ik27 INTEGER,
    ik28 INTEGER,
    ik29 INTEGER,
    ik210 INTEGER,
    tip INTEGER,
    num_ppi INTEGER,
    dlit INTEGER,
    duration_cycle INTEGER
);

CREATE TABLE IF NOT EXISTS vp01_kvd (
    id BIGSERIAL PRIMARY KEY,
    id_pr BIGINT REFERENCES vp01 (id),
    num_kvd INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    complect_msu INTEGER,
    num_ppi INTEGER,
    dlit INTEGER
);

CREATE TABLE IF NOT EXISTS vp01_omi (
    id BIGSERIAL PRIMARY KEY,
    id_pr BIGINT REFERENCES vp01 (id),
    num_omi INTEGER,
    type_omi INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    num_ppi INTEGER,
    dlit INTEGER
);

CREATE TABLE IF NOT EXISTS vp01_ona (
    id BIGSERIAL PRIMARY KEY,
    id_pr BIGINT REFERENCES vp01 (id),
    num_ust_ona INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    num_ppi INTEGER,
    dlit INTEGER
);

CREATE TABLE IF NOT EXISTS vp01_tnp (
    id BIGSERIAL PRIMARY KEY,
    id_pr BIGINT REFERENCES vp01 (id),
    num_tnp INTEGER,
    date_nach TIMESTAMP,
    date_con TIMESTAMP,
    num_ppi INTEGER,
    dlit INTEGER
);

-- ===== RN (регистрационные номера) =====

CREATE TABLE IF NOT EXISTS rn_pr01 (
    id BIGSERIAL PRIMARY KEY,
    rnf INTEGER NOT NULL,
    dsf TIMESTAMP,
    n_rp INTEGER,
    n_ka INTEGER,
    fo TEXT,
    CONSTRAINT rn_pr01_unique UNIQUE (rnf)
);

CREATE TABLE IF NOT EXISTS rn_pr03 (
    id BIGSERIAL PRIMARY KEY,
    rnf INTEGER NOT NULL,
    dsf TIMESTAMP,
    n_rp INTEGER,
    n_ka INTEGER,
    fo TEXT,
    CONSTRAINT rn_pr03_unique UNIQUE (rnf)
);

CREATE TABLE IF NOT EXISTS rn_pr04 (
    id BIGSERIAL PRIMARY KEY,
    rnf INTEGER NOT NULL,
    dsf TIMESTAMP,
    n_rp INTEGER,
    n_ka INTEGER,
    fo TEXT,
    CONSTRAINT rn_pr04_unique UNIQUE (rnf)
);

CREATE TABLE IF NOT EXISTS rn_vp01 (
    id BIGSERIAL PRIMARY KEY,
    rnf INTEGER NOT NULL,
    dsf TIMESTAMP,
    n_rp INTEGER,
    n_ka INTEGER,
    fo TEXT,
    CONSTRAINT rn_vp01_unique UNIQUE (rnf)
);

-- ===== Индексы =====

CREATE INDEX IF NOT EXISTS idx_programs_main_num ON programs_main (num_rp, num_ka);
CREATE INDEX IF NOT EXISTS idx_programs_mode_main ON programs_mode (id_programs_main);
CREATE INDEX IF NOT EXISTS idx_programs_mode_msu_mode ON programs_mode_msu (id_programs_mode);
CREATE INDEX IF NOT EXISTS idx_programs_mode_kvd_mode ON programs_mode_kvd (id_programs_mode);
CREATE INDEX IF NOT EXISTS idx_programs_mode_omi_mode ON programs_mode_omi (id_programs_mode);
CREATE INDEX IF NOT EXISTS idx_programs_mode_ona_mode ON programs_mode_ona (id_programs_mode);
CREATE INDEX IF NOT EXISTS idx_programs_ona_main ON programs_ona (id_programs);
CREATE INDEX IF NOT EXISTS idx_vp01_msu_pr ON vp01_msu (id_pr);
CREATE INDEX IF NOT EXISTS idx_vp01_kvd_pr ON vp01_kvd (id_pr);
CREATE INDEX IF NOT EXISTS idx_vp01_omi_pr ON vp01_omi (id_pr);
CREATE INDEX IF NOT EXISTS idx_vp01_ona_pr ON vp01_ona (id_pr);
CREATE INDEX IF NOT EXISTS idx_vp01_tnp_pr ON vp01_tnp (id_pr);