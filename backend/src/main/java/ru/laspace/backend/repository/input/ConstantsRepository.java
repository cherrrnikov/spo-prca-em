package ru.laspace.backend.repository.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ConstantsRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, Integer> MODE_TO_CID = Map.of(
            "astr", 6,
            "s", 90,
            "omi", 92,
            "tnp", 88,
            "kvd", 87,
            "ts", 89,
            "ona", 77);

    public Map<String, Integer> getModeDurationsMap() {
        List<Integer> cIds = new ArrayList<>(MODE_TO_CID.values());

        String placeholders = String.join(",",
                Collections.nCopies(cIds.size(), "?"));

        String sql = String.format(
                "SELECT c_id, c_value_sec FROM t_constants WHERE c_id IN (%s)",
                placeholders);

        Map<Integer, Integer> idToValue = new HashMap<>();
        jdbcTemplate.query(sql, cIds.toArray(), (rs) -> {
            idToValue.put(rs.getInt("c_id"), rs.getInt("c_value_sec"));
        });

        Map<String, Integer> result = new HashMap<>();
        MODE_TO_CID.forEach((mode, cId) -> {
            Integer value = idToValue.get(cId);
            if (value != null) {
                result.put(mode, value);
            }
        });

        return result;
    }
}
