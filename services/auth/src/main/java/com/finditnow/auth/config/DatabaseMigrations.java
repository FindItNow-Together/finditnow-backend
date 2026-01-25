package com.finditnow.auth.config;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public class DatabaseMigrations {
    public static void migrate(DataSource ds) {
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("filesystem:./db/migrations")
<<<<<<< HEAD
=======
                .defaultSchema("auth_service_schema")
>>>>>>> da72e1a (Update delivery system implementation)
                .load();

        flyway.migrate();
    }
}
