package com.finditnow.auth.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database transactions with automatic rollback on failure
 */
public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes a transactional operation
     *
     * @param operation The operation to execute within a transaction
     * @return The result of the operation
     * @throws Exception if the operation fails
     */
    public <T> T executeInTransaction(TransactionalOperation<T> operation) throws Exception {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            T result = operation.execute(conn);

            conn.commit();
            return result;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back due to error: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection", e);
                }
            }
        }
    }

    /**
     * Executes a read-only operation (no transaction needed)
     */
    public <T> T executeReadOnly(TransactionalOperation<T> operation) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return operation.execute(conn);
        }
    }

    @FunctionalInterface
    public interface TransactionalOperation<T> {
        T execute(Connection conn) throws Exception;
    }
}
