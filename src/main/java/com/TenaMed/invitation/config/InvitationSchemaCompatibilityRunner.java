package com.TenaMed.invitation.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class InvitationSchemaCompatibilityRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public InvitationSchemaCompatibilityRunner(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String dbName;
        try (Connection connection = dataSource.getConnection()) {
            dbName = connection.getMetaData().getDatabaseProductName();
        }

        if (dbName == null || !dbName.toLowerCase().contains("postgres")) {
            return;
        }

        // Keep invitation schema compatible while moving to generic institute fields.
        executeIgnoreErrors("ALTER TABLE invitations ADD COLUMN IF NOT EXISTS institute_id uuid");
        executeIgnoreErrors("ALTER TABLE invitations ADD COLUMN IF NOT EXISTS institute_type varchar(32)");
        executeIgnoreErrors("ALTER TABLE invitations ALTER COLUMN hospital_id DROP NOT NULL");

        executeIgnoreErrors("""
                UPDATE invitations
                SET institute_id = COALESCE(institute_id, hospital_id, pharmacy_id)
                WHERE institute_id IS NULL
                """);

        executeIgnoreErrors("""
                UPDATE invitations
                SET institute_type = CASE
                    WHEN institute_type IS NOT NULL THEN institute_type
                    WHEN pharmacy_id IS NOT NULL THEN 'PHARMACY'
                    WHEN hospital_id IS NOT NULL THEN 'HOSPITAL'
                    ELSE institute_type
                END
                WHERE institute_type IS NULL
                """);
    }

    private void executeIgnoreErrors(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ignored) {
            // Ignore to keep startup resilient across partially-migrated environments.
        }
    }
}
