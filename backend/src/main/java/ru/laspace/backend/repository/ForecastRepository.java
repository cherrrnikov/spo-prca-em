package ru.laspace.backend.repository;

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
import ru.laspace.backend.dto.forecast.ForecastDto;
import ru.laspace.backend.dto.forecast.ShadowDto;
import ru.laspace.backend.dto.forecast.ZasvetkaDto;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ForecastRepository {
    private final JdbcTemplate jdbcTemplate;

    public ForecastDto findLatestByDate(LocalDate date) {
        String sql = """
                    SELECT id, dn, dk, n_ka, n_init
                    FROM t_forecast
                    WHERE dn = ?
                    ORDER BY id DESC
                    LIMIT 1
                """;

        try {
            return jdbcTemplate.queryForObject(sql, new ForecastRowMapper(), date);
        } catch (EmptyResultDataAccessException e) {
            log.debug("Прогноз не найден для даты: {}", date);
            return null;
        }
    }

    public List<ShadowDto> findShadowsByForecastId(Long forecastId) {
        String sql = """
                SELECT id, n_rec, d_t_in, d_t_out,
                    EXTRACT(EPOCH FROM duration) as duration_seconds
                FROM t_shadow
                WHERE n_rec = ?
                ORDER BY d_t_in
                """;

        return jdbcTemplate.query(sql, new ShadowRowMapper(), forecastId);
    }

    public List<ZasvetkaDto> findZasvetkiByForecastId(Long forecastId) {
        String sql = """
                SELECT id, n_rec, d_t_in, d_t_out,
                EXTRACT (EPOCH FROM duration) as duration_seconds
                FROM t_zasvetki
                WHERE n_rec = ?
                ORDER BY d_t_in
                """;

        return jdbcTemplate.query(sql, new ZasvetkaRowMapper(), forecastId);
    }

    private static class ForecastRowMapper implements RowMapper<ForecastDto> {
        @Override
        public ForecastDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ForecastDto.builder()
                    .id(rs.getLong("id"))
                    .dn(rs.getDate("dn").toLocalDate())
                    .dk(rs.getDate("dk").toLocalDate())
                    .nKa(rs.getInt("n_ka"))
                    .nInit(rs.getInt("n_init"))
                    .build();
        }
    }

    private static class ShadowRowMapper implements RowMapper<ShadowDto> {
        @Override
        public ShadowDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ShadowDto.builder()
                    .id(rs.getLong("id"))
                    .nRec(rs.getLong("n_rec"))
                    .dTIn(rs.getTimestamp("d_t_in").toLocalDateTime())
                    .dTOut(rs.getTimestamp("d_t_out").toLocalDateTime())
                    .duration(rs.getInt("duration_seconds"))
                    .build();
        }
    }

    private static class ZasvetkaRowMapper implements RowMapper<ZasvetkaDto> {
        @Override
        public ZasvetkaDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ZasvetkaDto.builder()
                    .id(rs.getLong("id"))
                    .nRec(rs.getLong("n_rec"))
                    .dTIn(rs.getTimestamp("d_t_in").toLocalDateTime())
                    .dTOut(rs.getTimestamp("d_t_out").toLocalDateTime())
                    .duration(rs.getInt("duration_seconds"))
                    .build();
        }
    }
}
