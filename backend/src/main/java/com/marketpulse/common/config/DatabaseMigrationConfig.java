package com.marketpulse.common.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    public DatabaseSchemaMigrator databaseSchemaMigrator(DataSource dataSource) {
        return new DatabaseSchemaMigrator(dataSource);
    }

    @Bean
    public static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("databaseSchemaMigrator");
    }

    public static class DatabaseSchemaMigrator {
        public DatabaseSchemaMigrator(DataSource dataSource) {
            migrate(dataSource);
        }

        private void migrate(DataSource dataSource) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                log.info("Checking database schema compatibility...");

                // Check if 'users' table exists in the current schema
                boolean usersTableExists = false;
                try {
                    stmt.execute("SELECT 1 FROM users WHERE 1 = 0");
                    usersTableExists = true;
                } catch (Exception e) {
                    // Table does not exist yet (e.g. fresh database before Hibernate ddl-auto runs)
                    usersTableExists = false;
                }

                if (usersTableExists) {
                    boolean thresholdColExists = false;
                    try {
                        stmt.execute("SELECT threshold_percent FROM users WHERE 1 = 0");
                        thresholdColExists = true;
                    } catch (Exception e) {
                        thresholdColExists = false;
                    }

                    if (!thresholdColExists) {
                        log.info("Executing schema migration: adding 'threshold_percent' column to 'users' table...");
                        stmt.execute("ALTER TABLE users ADD COLUMN threshold_percent DOUBLE PRECISION DEFAULT 3.0");
                        stmt.execute("UPDATE users SET threshold_percent = 3.0 WHERE threshold_percent IS NULL");
                        log.info("Schema migration completed: 'threshold_percent' added with default 3.0.");
                    } else {
                        // Ensure all existing rows have default 3.0 if null
                        stmt.execute("UPDATE users SET threshold_percent = 3.0 WHERE threshold_percent IS NULL");
                    }
                }

                // Race condition prevention: Ensure unique index on watchlist_stocks(watchlist_id, symbol)
                try {
                    stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_watchlist_stocks_wl_symbol ON watchlist_stocks(watchlist_id, symbol)");
                } catch (Exception e) {
                    log.debug("Notice on unique index creation: {}", e.getMessage());
                }
            } catch (Exception ex) {
                log.warn("Database schema migration notice: {}", ex.getMessage());
            }
        }
    }
}
