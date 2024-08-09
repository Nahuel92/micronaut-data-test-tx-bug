package io.github.nahuel92;

import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.test.annotation.Sql;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.TransactionOperations;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Sql(phase = Sql.Phase.BEFORE_EACH, scripts = {"classpath:db/ddl.sql", "classpath:db/data.sql"})
class MicronautDataTestTxBugTest {
    @Inject
    private Service subject;

    @Inject
    private JdbcOperations jdbcOperations;

    @Inject
    private TransactionOperations<Connection> transactionOperations;

    @Nested
    @DisplayName("When repository code runs in same thread, it can see transaction's changes made on test code")
    class AnotherThreadTest {
        @Test
        @DisplayName("When attempting to update data before executing a subject's method, single-threaded subject sees the update")
        void successOnUpdatingDataBeforeExecutingSubjectMethod() {
            // given
            updateTestData();

            // when
            final var results = subject.getEnabledGood();

            // then
            assertTrue(results.contains(2L));
        }

        @Test
        @DisplayName("When commiting transactions to update data using TransactionOperations, single-threaded subject sees the update")
        void failureOnUpdatingDataBeforeExecutingSubjectMethodUsingTransactionOperations() {
            // given
            transactionOperations.executeWrite(s -> {
                updateTestData();
                return null;
            });

            // when
            final var results = subject.getEnabledGood();

            // then
            assertTrue(results.contains(2L));
        }

        @Test
        @DisplayName("When explicitly commiting transactions to update data, single-threaded subject sees the update")
        void successOnUpdatingDataBeforeExecutingSubjectMethodUsingManualCommit() throws SQLException {
            // given
            updateTestData();
            jdbcOperations.getConnection().commit();

            // when
            final var results = subject.getEnabledGood();

            // then
            assertTrue(results.contains(2L));
        }
    }

    @Nested
    @DisplayName("When repository code runs in another thread, it can't see transaction's changes made on test code")
    class SameThreadTest {
        @Test
        @DisplayName("When attempting to update data before executing a subject's method, subject fails to see the update")
        void failureOnUpdatingDataBeforeExecutingSubjectMethod() {
            // given
            updateTestData();

            // when
            final var results = subject.getEnabledBugged();

            // then
            assertTrue(results.contains(2L));
        }

        @Test
        @DisplayName("When commiting transactions to update data using TransactionOperations, subject fails to see the update")
        void failureOnUpdatingDataBeforeExecutingSubjectMethodUsingTransactionOperations() {
            // given
            transactionOperations.executeWrite(s -> {
                updateTestData();
                return null;
            });

            // when
            final var results = subject.getEnabledBugged();

            // then
            assertTrue(results.contains(2L));
        }

        @Test
        @DisplayName("When explicitly commiting transactions to update data, subject should see the update")
        void successOnUpdatingDataBeforeExecutingSubjectMethodUsingManualCommit() throws SQLException {
            // given
            updateTestData();
            jdbcOperations.getConnection().commit();

            // when
            final var results = subject.getEnabledBugged();

            // then
            assertTrue(results.contains(2L));
        }
    }

    private void updateTestData() {
        jdbcOperations.prepareStatement(
                "UPDATE my_entity SET enabled = TRUE WHERE id = 2",
                PreparedStatement::executeUpdate
        );
    }
}