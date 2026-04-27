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
import ru.laspace.backend.dto.input.kr01.Kr01ImpulseDto;
import ru.laspace.backend.dto.input.kr01.Kr01MainDto;

@Repository
@Slf4j
@RequiredArgsConstructor
public class Kr01Repository {
    private final JdbcTemplate jdbcTemplate;

    public Kr01MainDto findLatestByDate(LocalDate date) {
        String sql = """
                SELECT km.id, km.rnf, km.n_ka, km.dsf, km.n_bc, km.n_zad, km.k_imp,
                       km.dt_zap, km.n_form_id, km.used
                FROM kr01_main km
                INNER JOIN kr01 k ON km.id = k.id_main
                WHERE DATE(k.date_im) = DATE(?)
                ORDER BY k.date_im DESC
                LIMIT 1
                """;

        try {
            return jdbcTemplate.queryForObject(sql, new Kr01MainRowMapper(), date);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Запись ВКИ не найдена для даты: {}", date);
            return null;
        }
    }

    public List<Kr01ImpulseDto> findKr01ImpulseByMainId(Long idMain) {
        String sql = """
                SELECT id, id_main, n_vit, date_im, dlit,
                    pr_or, ugl_v, massa, n_du, pr_var
                FROM kr01
                WHERE id_main = ?
                ORDER BY date_im
                """;

        return jdbcTemplate.query(sql, new Kr01ImpulseRowMapper(), idMain);
    }

    private static class Kr01MainRowMapper implements RowMapper<Kr01MainDto> {
        @Override
        public Kr01MainDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Kr01MainDto.builder()
                    .id(rs.getLong("id"))
                    .rnf(rs.getInt("rnf"))
                    .nKa(rs.getInt("n_ka"))
                    .dsf(rs.getTimestamp("dsf").toLocalDateTime())
                    .nBc(rs.getInt("n_bc"))
                    .nZad(rs.getInt("n_zad"))
                    .kImp(rs.getInt("k_imp"))
                    .dtZap(rs.getTimestamp("dt_zap").toLocalDateTime())
                    .nFormId(rs.getInt("n_form_id"))
                    .used(rs.getInt("used"))
                    .build();
        }
    }

    private static class Kr01ImpulseRowMapper implements RowMapper<Kr01ImpulseDto> {
        @Override
        public Kr01ImpulseDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Kr01ImpulseDto.builder()
                    .id(rs.getLong("id"))
                    .idMain(rs.getLong("id_main"))
                    .nVit(rs.getInt("n_vit"))
                    .dateIm(rs.getTimestamp("date_im").toLocalDateTime())
                    .dlit(rs.getInt("dlit"))
                    .prOr(rs.getShort("pr_or"))
                    .uglV(rs.getDouble("ugl_v"))
                    .massa(rs.getDouble("massa"))
                    .nDu(rs.getInt("n_du"))
                    .prVar(rs.getShort("pr_var"))
                    .build();
        }
    }
}
