package com.researchassistant.operations.backup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class FlywaySchemaVersionService {

    private final JdbcTemplate jdbcTemplate;

    public FlywaySchemaVersionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String currentVersion() {
        return jdbcTemplate.query(
                """
                select version
                from flyway_schema_history
                where success = true and version is not null
                order by installed_rank desc
                limit 1
                """,
                resultSet -> resultSet.next()
                        ? resultSet.getString("version")
                        : null
        );
    }
}
