package ru.laspace.backend.repository.input;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.input.id06.Id06KvdDto;
import ru.laspace.backend.dto.input.id06.Id06MainDto;
import ru.laspace.backend.dto.input.id06.Id06OnaDto;
import ru.laspace.backend.dto.input.id06.Id06TnpDto;
import ru.laspace.backend.dto.input.id06.Id06TsDto;

@Repository
@Slf4j
@RequiredArgsConstructor
public class Id06Repository {

    private final JdbcTemplate jdbcTemplate;

    public Id06MainDto findLatestByDate(LocalDate date) {
        String sql = """
                SELECT id, rnf, n_ka, d_np, n_sp, dsf, k_zajv,
                       data_zap, n_form_id, used
                FROM id06
                WHERE d_np = ?
                ORDER BY data_zap DESC
                LIMIT 1
                """;

        try {
            return jdbcTemplate.queryForObject(sql, new Id06MainRowMapper(), date);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Запись ИД06 не найдена для даты: {}", date);
            return null;
        }
    }

    public List<Id06KvdDto> findKvdByMainId(Long idMain) {
        String sql = """
                SELECT id, id_main, dn, dk, pr_msu, pr_bssd, pr_zg
                FROM id06_kvd
                WHERE id_main = ?
                ORDER BY dn
                """;

        return jdbcTemplate.query(sql, new Id06KvdRowMapper(), idMain);
    }

    public List<Id06TnpDto> findTnpByMainId(Long idMain) {
        String sql = """
                SELECT id, id_main, dn, dk, dlit
                FROM id06_tnp
                WHERE id_main = ?
                ORDER BY dn
                """;

        return jdbcTemplate.query(sql, new Id06TnpRowMapper(), idMain);
    }

    public List<Id06TsDto> findTsByMainId(Long idMain) {
        String sql = """
                SELECT id, id_main, dn, dk, tip, reg,
                       pr_msu1, pr_vd_msu1, pr_ik_msu1,
                       pr_vd1_1, pr_vd2_1, pr_vd3_1,
                       pr_ik4_1, pr_ik5_1, pr_ik6_1, pr_ik7_1,
                       pr_ik8_1, pr_ik9_1, pr_ik10_1,
                       pr_msu2, pr_vd_msu2, pr_ik_msu2,
                       pr_vd1_2, pr_vd2_2, pr_vd3_2,
                       pr_ik4_2, pr_ik5_2, pr_ik6_2, pr_ik7_2,
                       pr_ik8_2, pr_ik9_2, pr_ik10_2,
                       pr_otkl_zg
                FROM id06_ts
                WHERE id_main = ?
                ORDER BY dn
                """;

        return jdbcTemplate.query(sql, new Id06TsRowMapper(), idMain);
    }

    public List<Id06OnaDto> findOnaByMainId(Long idMain) {
        String sql = """
                SELECT id, id_main, dn, dk, n_ona, dlit
                FROM id06_ona
                WHERE id_main = ?
                ORDER BY dn
                """;

        return jdbcTemplate.query(sql, new Id06OnaRowMapper(), idMain);
    }

    private static class Id06MainRowMapper implements RowMapper<Id06MainDto> {
        @Override
        public Id06MainDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id06MainDto.builder()
                    .id(rs.getLong("id"))
                    .rnf(rs.getInt("rnf"))
                    .nKa(rs.getInt("n_ka"))
                    .dNp(rs.getDate("d_np").toLocalDate())
                    .nSp(rs.getInt("n_sp"))
                    .dsf(rs.getDate("dsf").toLocalDate())
                    .kZajv(rs.getInt("k_zajv"))
                    .dataZap(rs.getTimestamp("data_zap").toLocalDateTime())
                    .nFormId(rs.getInt("n_form_id"))
                    .used(rs.getInt("used"))
                    .build();
        }
    }

    private static class Id06KvdRowMapper implements RowMapper<Id06KvdDto> {
        @Override
        public Id06KvdDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id06KvdDto.builder()
                    .id(rs.getLong("id"))
                    .idMain(rs.getLong("id_main"))
                    .dn(rs.getTimestamp("dn").toLocalDateTime())
                    .dk(rs.getTimestamp("dk").toLocalDateTime())
                    .prMsu(rs.getInt("pr_msu"))
                    .prBssd(rs.getInt("pr_bssd"))
                    .prZg(rs.getInt("pr_zg"))
                    .build();
        }
    }

    private static class Id06TnpRowMapper implements RowMapper<Id06TnpDto> {
        @Override
        public Id06TnpDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id06TnpDto.builder()
                    .id(rs.getLong("id"))
                    .idMain(rs.getLong("id_main"))
                    .dn(rs.getTimestamp("dn").toLocalDateTime())
                    .dk(rs.getTimestamp("dk").toLocalDateTime())
                    .dlit(rs.getInt("dlit"))
                    .build();
        }
    }

    private static class Id06TsRowMapper implements RowMapper<Id06TsDto> {
        @Override
        public Id06TsDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id06TsDto.builder()
                    .id(rs.getLong("id"))
                    .idMain(rs.getLong("id_main"))
                    .dn(rs.getTimestamp("dn").toLocalDateTime())
                    .dk(rs.getTimestamp("dk").toLocalDateTime())
                    .tip(rs.getInt("tip"))
                    .reg(rs.getInt("reg"))
                    .prMsu1(rs.getInt("pr_msu1"))
                    .prVdMsu1(rs.getInt("pr_vd_msu1"))
                    .prIkMsu1(rs.getInt("pr_ik_msu1"))
                    .prVd1_1(rs.getInt("pr_vd1_1"))
                    .prVd2_1(rs.getInt("pr_vd2_1"))
                    .prVd3_1(rs.getInt("pr_vd3_1"))
                    .prIk4_1(rs.getInt("pr_ik4_1"))
                    .prIk5_1(rs.getInt("pr_ik5_1"))
                    .prIk6_1(rs.getInt("pr_ik6_1"))
                    .prIk7_1(rs.getInt("pr_ik7_1"))
                    .prIk8_1(rs.getInt("pr_ik8_1"))
                    .prIk9_1(rs.getInt("pr_ik9_1"))
                    .prIk10_1(rs.getInt("pr_ik10_1"))
                    .prMsu2(rs.getInt("pr_msu2"))
                    .prVdMsu2(rs.getInt("pr_vd_msu2"))
                    .prIkMsu2(rs.getInt("pr_ik_msu2"))
                    .prVd1_2(rs.getInt("pr_vd1_2"))
                    .prVd2_2(rs.getInt("pr_vd2_2"))
                    .prVd3_2(rs.getInt("pr_vd3_2"))
                    .prIk4_2(rs.getInt("pr_ik4_2"))
                    .prIk5_2(rs.getInt("pr_ik5_2"))
                    .prIk6_2(rs.getInt("pr_ik6_2"))
                    .prIk7_2(rs.getInt("pr_ik7_2"))
                    .prIk8_2(rs.getInt("pr_ik8_2"))
                    .prIk9_2(rs.getInt("pr_ik9_2"))
                    .prIk10_2(rs.getInt("pr_ik10_2"))
                    .prOtklZg(rs.getInt("pr_otkl_zg"))
                    .build();
        }
    }

    private static class Id06OnaRowMapper implements RowMapper<Id06OnaDto> {
        @Override
        public Id06OnaDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Id06OnaDto.builder()
                    .id(rs.getLong("id"))
                    .idMain(rs.getLong("id_main"))
                    .dn(rs.getTimestamp("dn").toLocalDateTime())
                    .dk(rs.getTimestamp("dk").toLocalDateTime())
                    .nOna(rs.getInt("n_ona"))
                    .dlit(rs.getInt("dlit"))
                    .build();
        }
    }
}