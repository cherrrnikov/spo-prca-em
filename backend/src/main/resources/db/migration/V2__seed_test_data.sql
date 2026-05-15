-- ============================================================
-- V2: тестовые данные для разработки
-- Только для dev-окружения (db/seed/)
-- ============================================================

-- ===== ИД02: исходные данные МСУ-ГС =====

-- 22.01.2026 — все системы исправны
INSERT INTO id02 (
    rnf, n_ka, d_np, n_sp, dsf, data_zap,
    i_msu1, i_vd_1, i_ik_1,
    vd1_1, vd2_1, vd3_1,
    ik4_1, ik5_1, ik6_1, ik7_1, ik8_1, ik9_1, ik10_1,
    i_msu2, i_vd_2, i_ik_2,
    vd1_2, vd2_2, vd3_2,
    ik4_2, ik5_2, ik6_2, ik7_2, ik8_2, ik9_2, ik10_2,
    pr_bssd, bssd1, bssd2, pr_zg, pr_otkl_zg
) VALUES (
    220126001, 1, '2026-01-22', 5100, '2026-01-22', '2026-01-22',
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 0
);

-- 27.01.2026 — ВД МСУ-ГС1 неисправен, БССД2 неисправен
INSERT INTO id02 (
    rnf, n_ka, d_np, n_sp, dsf, data_zap,
    i_msu1, i_vd_1, i_ik_1,
    vd1_1, vd2_1, vd3_1,
    ik4_1, ik5_1, ik6_1, ik7_1, ik8_1, ik9_1, ik10_1,
    i_msu2, i_vd_2, i_ik_2,
    vd1_2, vd2_2, vd3_2,
    ik4_2, ik5_2, ik6_2, ik7_2, ik8_2, ik9_2, ik10_2,
    pr_bssd, bssd1, bssd2, pr_zg, pr_otkl_zg
) VALUES (
    270126001, 1, '2026-01-27', 5150, '2026-01-27', '2026-01-27',
    1, 0, 1,
    0, 0, 0,
    1, 1, 0, 1, 1, 0, 1,
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    1, 1, 0, 1, 1
);

-- 01.04.2026 — ВД МСУ-ГС1 неисправен, БССД2 неисправен
INSERT INTO id02 (
    rnf, n_ka, d_np, n_sp, dsf, data_zap,
    i_msu1, i_vd_1, i_ik_1,
    vd1_1, vd2_1, vd3_1,
    ik4_1, ik5_1, ik6_1, ik7_1, ik8_1, ik9_1, ik10_1,
    i_msu2, i_vd_2, i_ik_2,
    vd1_2, vd2_2, vd3_2,
    ik4_2, ik5_2, ik6_2, ik7_2, ik8_2, ik9_2, ik10_2,
    pr_bssd, bssd1, bssd2, pr_zg, pr_otkl_zg
) VALUES (
    270426001, 1, '2026-04-01', 5200, '2026-04-01', '2026-04-01',
    1, 0, 1,
    0, 0, 0,
    1, 1, 0, 1, 1, 0, 1,
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    1, 1, 0, 1, 1
);

-- 02.04.2026 — ВД МСУ-ГС1 неисправен, БССД выключен, ЗГ выключена
INSERT INTO id02 (
    rnf, n_ka, d_np, n_sp, dsf, data_zap,
    i_msu1, i_vd_1, i_ik_1,
    vd1_1, vd2_1, vd3_1,
    ik4_1, ik5_1, ik6_1, ik7_1, ik8_1, ik9_1, ik10_1,
    i_msu2, i_vd_2, i_ik_2,
    vd1_2, vd2_2, vd3_2,
    ik4_2, ik5_2, ik6_2, ik7_2, ik8_2, ik9_2, ik10_2,
    pr_bssd, bssd1, bssd2, pr_zg, pr_otkl_zg
) VALUES (
    270426002, 1, '2026-04-02', 5201, '2026-04-02', '2026-04-02',
    1, 0, 1,
    0, 0, 0,
    1, 1, 0, 1, 1, 0, 1,
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    0, 1, 0, 0, 0
);

-- ===== ИД06: режимы работы ЦА =====
-- Используем CTE чтобы не хардкодить id

-- 21.01.2026
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114001, 1, '2026-01-21', 5001, '2026-01-21', 3, 101)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
SELECT id, '2026-01-21 06:00:00', '2026-01-21 23:00:00', 0, 1, 1, 1, 1, 1, 1, 0
FROM ins;

-- 22.01.2026 — с ОНА и КВД
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114002, 1, '2026-01-22', 5002, '2026-01-22', 3, 101)
    RETURNING id
),
ins_ts AS (
    INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
    SELECT id,
           '2026-01-22 01:00:00'::timestamp,
           '2026-01-22 23:00:00'::timestamp,
           1, 1, 1, 1, 1, 1, 1, 0
    FROM ins
),
ins_ona AS (
    INSERT INTO id06_ona (id_main, dn, dk, n_ona, dlit)
    SELECT id,
           '2026-01-22 00:00:00'::timestamp,
           '2026-01-22 00:25:00'::timestamp,
           1, 1500
    FROM ins
    UNION ALL
    SELECT id,
           '2026-01-22 23:00:00'::timestamp,
           '2026-01-22 23:47:00'::timestamp,
           2, 2820
    FROM ins
)
INSERT INTO id06_kvd (id_main, dn, dk, pr_msu, pr_bssd, pr_zg)
SELECT id,
       '2026-01-22 00:00:00'::timestamp,
       '2026-01-22 00:25:00'::timestamp,
       1, 1, 1
FROM ins;

-- 25.01.2026
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114003, 1, '2026-01-25', 5003, '2026-01-25', 3, 101)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
SELECT id, '2026-01-25 01:00:00', '2026-01-25 23:00:00', 1, 1, 1, 1, 1, 1, 1, 0
FROM ins;

-- 26.01.2026
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114004, 1, '2026-01-26', 5004, '2026-01-26', 3, 101)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
SELECT id, '2026-01-26 01:00:00', '2026-01-26 23:00:00', 1, 1, 1, 1, 1, 1, 1, 0
FROM ins;

-- 27.01.2026
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114005, 1, '2026-01-27', 5005, '2026-01-27', 3, 101)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
SELECT id, '2026-01-27 01:00:00', '2026-01-27 23:00:00', 1, 1, 1, 1, 1, 1, 1, 0
FROM ins;

-- 28.01.2026
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, n_form_id)
    VALUES (240114006, 1, '2026-01-28', 5006, '2026-01-28', 3, 101)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg, pr_msu1, pr_vd_msu1, pr_vd1_1, pr_vd2_1, pr_vd3_1, pr_otkl_zg)
SELECT id, '2026-01-28 01:00:00', '2026-01-28 23:00:00', 1, 1, 1, 1, 1, 1, 1, 0
FROM ins;

-- ===== ПРОГНОЗ: тени и засветки =====

-- 22.01.2026
WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-01-22 00:00:00', '2026-01-22 23:59:59', 1, 1001)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-01-22 13:40:00', '2026-01-22 14:20:00', '40 minutes'::interval
    FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-01-22 13:00:00', '2026-01-22 15:00:00', '120 minutes'::interval
FROM ins;

-- 27.01.2026
WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-01-27 00:00:00', '2026-01-27 23:59:59', 1, 1002)
    RETURNING id
)
INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-01-27 12:00:00', '2026-01-27 13:00:00', '60 minutes'::interval
FROM ins;

-- ===== КР01: коррекция орбиты =====

WITH ins AS (
    INSERT INTO kr01_main (rnf, n_ka, dsf, n_bc, n_zad, k_imp, dt_zap, n_form_id, used)
    VALUES (260122001, 1, '2026-01-22 10:00:00', 1, 1001, 2, '2026-01-22 10:05:00', 101, 0)
    RETURNING id
)
INSERT INTO kr01 (id_main, n_vit, date_im, dlit, pr_or, ugl_v, massa, n_du, pr_var)
SELECT id, 1250, '2026-01-22 08:30:00'::timestamp, 120, 1, 0.5, 1500.5, 3, 1 FROM ins
UNION ALL
SELECT id, 1251, '2026-01-25 10:15:00'::timestamp, 90,  1, 0.5, 1499.8, 5, 1 FROM ins;

-- ===== РО02: сезонный разворот =====

INSERT INTO ro_02 (rnf, dsf, n_ka, data_n, data_razv, data_k, n_form_id)
VALUES (260125001, '2026-01-25 10:00:00', 1,
        '2026-01-25 10:00:00', '2026-01-27 10:00:00', '2026-01-29 10:00:00', 102);