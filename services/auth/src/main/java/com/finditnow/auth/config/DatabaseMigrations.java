package com.finditnow.auth.config;

import com.finditnow.config.Config;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class DatabaseMigrations {
    public static void migrate(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("filesystem:./db/migrations")
                .load();

        flyway.migrate();
    }
}
