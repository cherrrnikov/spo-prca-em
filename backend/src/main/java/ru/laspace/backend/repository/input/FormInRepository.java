package ru.laspace.backend.repository.input;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class FormInRepository {
    private final JdbcTemplate jdbcTemplate;

    public String findLatestContentByIdentN(Integer identN) {
        String sql = """
                SELECT content FROM form_in
                WHERE ident_n = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try {
            return jdbcTemplate.queryForObject(sql, String.class, identN);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Запись form_in не найдена для ident_n={}", identN);
            return null;
        }
    }
}
