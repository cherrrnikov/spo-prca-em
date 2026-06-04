-- ============================================================
-- V3: тестовые данные июль 2026
-- ============================================================

-- ИД06 — 01.07.2026: ТНП в 09:00, 09:30, 10:00, 10:30
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, data_zap)
    VALUES (23, 1525, '2026-07-01', 80, '2026-07-01', 4, '2026-07-02 00:00:00'::timestamp)
    RETURNING id
)
INSERT INTO id06_tnp (id_main, dn, dk, dlit)
SELECT id, '2026-07-01 09:00:00'::timestamp, '2026-07-01 09:08:36'::timestamp, 516 FROM ins
UNION ALL
SELECT id, '2026-07-01 09:30:00'::timestamp, '2026-07-01 09:38:36'::timestamp, 516 FROM ins
UNION ALL
SELECT id, '2026-07-01 10:00:00'::timestamp, '2026-07-01 10:08:36'::timestamp, 516 FROM ins
UNION ALL
SELECT id, '2026-07-01 10:30:00'::timestamp, '2026-07-01 10:38:36'::timestamp, 516 FROM ins;

-- ИД06 — 02.07.2026: Юст.ОНА1 09:00, Юст.ОНА2 11:00
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, data_zap)
    VALUES (24, 1525, '2026-07-02', 81, '2026-07-02', 2, '2026-07-03 00:00:00'::timestamp)
    RETURNING id
)
INSERT INTO id06_ona (id_main, dn, dk, n_ona, dlit)
SELECT id, '2026-07-02 09:00:00'::timestamp, '2026-07-02 09:25:00'::timestamp, 1, 1500 FROM ins
UNION ALL
SELECT id, '2026-07-02 11:00:00'::timestamp, '2026-07-02 11:25:00'::timestamp, 2, 1500 FROM ins;

-- ИД06 — 03.07.2026: Техн.съёмка МСУ1 штатная, КВД, ТНП
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, data_zap)
    VALUES (25, 1525, '2026-07-03', 82, '2026-07-03', 6, '2026-07-04 00:00:00'::timestamp)
    RETURNING id
),
ins_ts AS (
    INSERT INTO id06_ts (id_main, dn, dk, tip, reg,
        pr_msu1, pr_vd_msu1, pr_ik_msu1,
        pr_vd1_1, pr_vd2_1, pr_vd3_1,
        pr_ik4_1, pr_ik5_1, pr_ik6_1, pr_ik7_1, pr_ik8_1, pr_ik9_1, pr_ik10_1,
        pr_msu2, pr_vd_msu2, pr_ik_msu2,
        pr_otkl_zg)
    SELECT id,
        '2026-07-03 00:00:00'::timestamp, '2026-07-03 23:59:59'::timestamp,
        1, 1,
        1, 1, 1,
        1, 1, 1,
        1, 1, 1, 1, 1, 1, 1,
        0, 0, 0,
        0
    FROM ins
    RETURNING id_main
),
ins_kvd AS (
    INSERT INTO id06_kvd (id_main, dn, dk, pr_msu, pr_bssd, pr_zg)
    SELECT id, '2026-07-03 09:00:00'::timestamp, '2026-07-03 09:07:00'::timestamp, 1, 0, 0 FROM ins
    UNION ALL
    SELECT id, '2026-07-03 09:30:00'::timestamp, '2026-07-03 09:37:00'::timestamp, 1, 0, 0 FROM ins
    UNION ALL
    SELECT id, '2026-07-03 10:00:00'::timestamp, '2026-07-03 10:07:00'::timestamp, 1, 0, 0 FROM ins
    RETURNING id_main
)
INSERT INTO id06_tnp (id_main, dn, dk, dlit)
SELECT id, '2026-07-03 07:00:00'::timestamp, '2026-07-03 07:08:36'::timestamp, 516 FROM ins
UNION ALL
SELECT id, '2026-07-03 09:00:00'::timestamp, '2026-07-03 09:08:36'::timestamp, 516 FROM ins;

-- ИД06 — 04.07.2026: Техн.съёмка МСУ2 учащённая
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, data_zap)
    VALUES (26, 1525, '2026-07-04', 83, '2026-07-04', 1, '2026-07-05 00:00:00'::timestamp)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg,
    pr_msu1, pr_vd_msu1, pr_ik_msu1,
    pr_msu2, pr_vd_msu2, pr_ik_msu2,
    pr_vd1_2, pr_vd2_2, pr_vd3_2,
    pr_ik4_2, pr_ik5_2, pr_ik6_2, pr_ik7_2, pr_ik8_2, pr_ik9_2, pr_ik10_2,
    pr_otkl_zg)
SELECT id,
    '2026-07-04 00:00:00'::timestamp, '2026-07-04 23:58:59'::timestamp,
    2, 1,
    0, 0, 0,
    1, 1, 1,
    1, 1, 1,
    1, 1, 1, 1, 1, 1, 1,
    0
FROM ins;

-- ВКИ1 — 04.07.2026 15:00
WITH ins AS (
    INSERT INTO kr01_main (rnf, n_ka, dsf, n_bc, n_zad, k_imp, dt_zap, n_form_id, used)
    VALUES (28, 1525, '2026-07-04 15:00:00'::timestamp, 1, 1002, 1, '2026-07-04 15:05:00'::timestamp, 101, 0)
    RETURNING id
)
INSERT INTO kr01 (id_main, n_vit, date_im, dlit, pr_or, ugl_v, massa, n_du, pr_var)
SELECT id, 1300, '2026-07-04 15:00:00'::timestamp, 120, 1, 0.5, 1500.0, 3, 1 FROM ins;

-- ИД06 — 05.07.2026: Техн.съёмка ВД МСУ1 + ИК МСУ2 учащённая
WITH ins AS (
    INSERT INTO id06 (rnf, n_ka, d_np, n_sp, dsf, k_zajv, data_zap)
    VALUES (27, 1525, '2026-07-05', 84, '2026-07-05', 1, '2026-07-06 00:00:00'::timestamp)
    RETURNING id
)
INSERT INTO id06_ts (id_main, dn, dk, tip, reg,
    pr_msu1, pr_vd_msu1, pr_ik_msu1,
    pr_vd1_1, pr_vd2_1, pr_vd3_1,
    pr_ik4_1, pr_ik5_1, pr_ik6_1, pr_ik7_1, pr_ik8_1, pr_ik9_1, pr_ik10_1,
    pr_msu2, pr_vd_msu2, pr_ik_msu2,
    pr_vd1_2, pr_vd2_2, pr_vd3_2,
    pr_ik4_2, pr_ik5_2, pr_ik6_2, pr_ik7_2, pr_ik8_2, pr_ik9_2, pr_ik10_2,
    pr_otkl_zg)
SELECT id,
    '2026-07-05 00:00:00'::timestamp, '2026-07-05 23:55:59'::timestamp,
    2, 1,
    1, 1, 0,
    1, 1, 1,
    0, 0, 0, 0, 0, 0, 0,
    1, 0, 1,
    0, 0, 0,
    1, 1, 1, 1, 1, 1, 1,
    0
FROM ins;

-- РО02 — сезонный разворот
INSERT INTO ro_02 (rnf, dsf, n_ka, data_n, data_razv, data_k, n_form_id)
VALUES (29, '2026-07-03 00:00:00'::timestamp, 1525,
        '2026-07-03 00:00:00'::timestamp,
        '2026-07-05 11:00:00'::timestamp,
        '2026-07-08 00:00:00'::timestamp,
        102);

-- Тени и засветки 01-05.07.2026

WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-07-01 00:00:00'::timestamp, '2026-07-01 23:59:59'::timestamp, 1525, 1001)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-07-01 21:00:00'::timestamp, '2026-07-01 21:30:00'::timestamp, '30 minutes'::interval FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-07-01 20:30:00'::timestamp, '2026-07-01 22:00:00'::timestamp, '90 minutes'::interval FROM ins;

WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-07-02 00:00:00'::timestamp, '2026-07-02 23:59:59'::timestamp, 1525, 1002)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-07-02 21:00:00'::timestamp, '2026-07-02 21:30:00'::timestamp, '30 minutes'::interval FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-07-02 20:30:00'::timestamp, '2026-07-02 22:00:00'::timestamp, '90 minutes'::interval FROM ins;

WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-07-03 00:00:00'::timestamp, '2026-07-03 23:59:59'::timestamp, 1525, 1003)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-07-03 21:00:00'::timestamp, '2026-07-03 21:30:00'::timestamp, '30 minutes'::interval FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-07-03 20:30:00'::timestamp, '2026-07-03 22:00:00'::timestamp, '90 minutes'::interval FROM ins;

WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-07-04 00:00:00'::timestamp, '2026-07-04 23:59:59'::timestamp, 1525, 1004)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-07-04 21:00:00'::timestamp, '2026-07-04 21:30:00'::timestamp, '30 minutes'::interval FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-07-04 20:30:00'::timestamp, '2026-07-04 22:00:00'::timestamp, '90 minutes'::interval FROM ins;

WITH ins AS (
    INSERT INTO t_forecast (dn, dk, n_ka, n_init)
    VALUES ('2026-07-05 00:00:00'::timestamp, '2026-07-05 23:59:59'::timestamp, 1525, 1005)
    RETURNING id
),
ins_shadow AS (
    INSERT INTO t_shadow (n_rec, d_t_in, d_t_out, duration)
    SELECT id, '2026-07-05 21:00:00'::timestamp, '2026-07-05 21:30:00'::timestamp, '30 minutes'::interval FROM ins
)
INSERT INTO t_zasvetka (n_rec, d_t_in, d_t_out, duration)
SELECT id, '2026-07-05 20:30:00'::timestamp, '2026-07-05 22:00:00'::timestamp, '90 minutes'::interval FROM ins;
