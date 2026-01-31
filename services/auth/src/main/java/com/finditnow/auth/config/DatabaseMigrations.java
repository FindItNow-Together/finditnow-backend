package com.finditnow.auth.config;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class DatabaseMigrations {
    public static void migrate(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
    }
}
