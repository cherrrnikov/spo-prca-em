package ru.laspace.backend.repository.input;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.input.id02.Id02Dto;

@Repository
@Slf4j
@RequiredArgsConstructor
public class Id02Repository {

    private final JdbcTemplate jdbcTemplate;

    public Id02Dto findLatestByDate(LocalDate date) {
        String sql = """
                SELECT id, rnf, n_ka, d_np, n_sp, dsf, data_zap,
                       i_msu1, i_vd_1, i_ik_1,
                       vd1_1, vd2_1, vd3_1,
                       ik4_1, ik5_1, ik6_1, ik7_1, ik8_1, ik9_1, ik10_1,
                       i_msu2, i_vd_2, i_ik_2,
                       vd1_2, vd2_2, vd3_2,
                       ik4_2, ik5_2, ik6_2, ik7_2, ik8_2, ik9_2, ik10_2,
                       pr_bssd, bssd1, bssd2, pr_zg, pr_otkl_zg
                FROM id02
                WHERE d_np = ?
                ORDER BY data_zap DESC
                LIMIT 1
                """;

        try {
            return jdbcTemplate.queryForObject(sql, new Id02RowMapper(), date);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Запись ИД02 не найдена для даты: {}", date);
            return null;
        }
    }

    private static class Id02RowMapper implements RowMapper<Id02Dto> {
        @Override
        public Id02Dto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id02Dto.builder()
                    .id(rs.getLong("id"))
                    .rnf(rs.getInt("rnf"))
                    .nKa(rs.getInt("n_ka"))
                    .dNp(rs.getDate("d_np").toLocalDate())
                    .nSp(rs.getInt("n_sp"))
                    .dsf(rs.getDate("dsf").toLocalDate())
                    .dataZap(rs.getTimestamp("data_zap").toLocalDateTime())
                    .i_msu1(rs.getInt("i_msu1"))
                    .i_vd_1(rs.getInt("i_vd_1"))
                    .i_ik_1(rs.getInt("i_ik_1"))
                    .vd1_1(rs.getInt("vd1_1"))
                    .vd2_1(rs.getInt("vd2_1"))
                    .vd3_1(rs.getInt("vd3_1"))
                    .ik4_1(rs.getInt("ik4_1"))
                    .ik5_1(rs.getInt("ik5_1"))
                    .ik6_1(rs.getInt("ik6_1"))
                    .ik7_1(rs.getInt("ik7_1"))
                    .ik8_1(rs.getInt("ik8_1"))
                    .ik9_1(rs.getInt("ik9_1"))
                    .ik10_1(rs.getInt("ik10_1"))
                    .i_msu2(rs.getInt("i_msu2"))
                    .i_vd_2(rs.getInt("i_vd_2"))
                    .i_ik_2(rs.getInt("i_ik_2"))
                    .vd1_2(rs.getInt("vd1_2"))
                    .vd2_2(rs.getInt("vd2_2"))
                    .vd3_2(rs.getInt("vd3_2"))
                    .ik4_2(rs.getInt("ik4_2"))
                    .ik5_2(rs.getInt("ik5_2"))
                    .ik6_2(rs.getInt("ik6_2"))
                    .ik7_2(rs.getInt("ik7_2"))
                    .ik8_2(rs.getInt("ik8_2"))
                    .ik9_2(rs.getInt("ik9_2"))
                    .ik10_2(rs.getInt("ik10_2"))
                    .pr_bssd(rs.getInt("pr_bssd"))
                    .bssd1(rs.getInt("bssd1"))
                    .bssd2(rs.getInt("bssd2"))
                    .pr_zg(rs.getInt("pr_zg"))
                    .pr_otkl_zg(rs.getInt("pr_otkl_zg"))
                    .build();
        }
    }
}