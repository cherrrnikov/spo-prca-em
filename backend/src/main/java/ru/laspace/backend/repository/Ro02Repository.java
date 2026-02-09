package ru.laspace.backend.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.laspace.backend.dto.ro02.Ro02Dto;

@Repository
@Slf4j
@RequiredArgsConstructor
public class Ro02Repository {
    private final JdbcTemplate jdbcTemplate;

    public List<Ro02Dto> findByDateInRange(LocalDate date) {
        String sql = """
                SELECT id, rnf, n_ka, dsf, data_n, data_razv, data_k, n_form_id
                FROM ro_02
                WHERE data_n <= ?
                    AND (? <= COALESCE(data_k, data_razv))
                    ORDER BY data_razv
                """;
        return jdbcTemplate.query(sql, new Ro02RowMapper(), date, date);
    }

    private static class Ro02RowMapper implements RowMapper<Ro02Dto> {
        @Override
        public Ro02Dto mapRow(ResultSet rs, int rowNum) throws SQLException {
            Ro02Dto.Ro02DtoBuilder builder = Ro02Dto.builder()
                    .id(rs.getLong("id"))
                    .rnf(rs.getInt("rnf"))
                    .nKa(rs.getInt("n_ka"))
                    .dsf(rs.getDate("dsf").toLocalDate())
                    .dataN(rs.getDate("data_n").toLocalDate())
                    .dataRazv(rs.getDate("data_razv").toLocalDate())
                    .nFormId(rs.getInt("n_form_id"));

            Date dataK = rs.getDate("data_k");
            if (dataK != null) {
                builder.dataK(dataK.toLocalDate());
            }

            return builder.build();
        }
    }
}
