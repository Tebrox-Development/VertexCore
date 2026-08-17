package de.tebrox.vertexCore.database.backend;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcDatabaseBackendStorageIdentityTest {

    @Test
    void equalConfiguredJdbcUrlsAreSameStorageAcrossBackendInstances() {
        HikariDataSource leftDataSource = dataSource("jdbc:mysql://db.example:3306/vertex");
        HikariDataSource rightDataSource = dataSource("jdbc:mysql://db.example:3306/vertex");
        try {
            JdbcDatabaseBackend left = new JdbcDatabaseBackend(leftDataSource, leftDataSource.getJdbcUrl());
            JdbcDatabaseBackend right = new JdbcDatabaseBackend(rightDataSource, rightDataSource.getJdbcUrl());

            assertTrue(left.sameStorageAs(right));
            assertTrue(right.sameStorageAs(left));
        } finally {
            leftDataSource.close();
            rightDataSource.close();
        }
    }

    @Test
    void differentConfiguredJdbcUrlsAreNotSameStorage() {
        HikariDataSource leftDataSource = dataSource("jdbc:mysql://db.example:3306/vertex_a");
        HikariDataSource rightDataSource = dataSource("jdbc:mysql://db.example:3306/vertex_b");
        try {
            JdbcDatabaseBackend left = new JdbcDatabaseBackend(leftDataSource, leftDataSource.getJdbcUrl());
            JdbcDatabaseBackend right = new JdbcDatabaseBackend(rightDataSource, rightDataSource.getJdbcUrl());

            assertFalse(left.sameStorageAs(right));
            assertFalse(right.sameStorageAs(left));
        } finally {
            leftDataSource.close();
            rightDataSource.close();
        }
    }

    private static HikariDataSource dataSource(String jdbcUrl) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        return dataSource;
    }
}
