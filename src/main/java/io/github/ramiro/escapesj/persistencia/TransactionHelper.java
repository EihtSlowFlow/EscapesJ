package io.github.ramiro.escapesj.persistencia;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionHelper {

    @FunctionalInterface
    public interface TransactionalAction<T> {
        T execute(Connection txConn) throws Exception;
    }

    /**
     * Executes the given action within a database transaction.
     * Disables autoCommit, executes, and commits on success or rolls back on error.
     */
    public static <T> T runInTransaction(TransactionalAction<T> action) throws Exception {
        try (Connection conn = DatabaseService.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                T result = action.execute(conn);
                conn.commit();
                return result;
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        }
    }
}
