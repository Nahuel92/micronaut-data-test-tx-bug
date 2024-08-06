package io.github.nahuel92;

import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.test.annotation.Sql;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.TransactionOperations;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Sql(phase = Sql.Phase.BEFORE_ALL, scripts = {"classpath:db/ddl.sql", "classpath:db/data.sql"})
class MicronautDataTestTxBugTest {
    @Inject
    private EntityRepository subject;

    @Inject
    private JdbcOperations jdbcOperations;

    @Inject
    private TransactionOperations<Connection> transactionOperations;

    @Test
    @DisplayName("When attempting to update data before executing a subject's method, subject fails to see the update")
    void failureToUpdateDataBeforeExecutingSubjectMethod() {
        // given
        updateEnabledFlag();

        // when
        final var results = subject.getEnabled();

        // then
        assertAll(
                () -> assertTrue(results.contains(2L)),
                () -> assertTrue(getUpdatedEnabledFlag())
        );
    }

    @Test
    @DisplayName("When explicitly commiting transactions to update data, subject should see the update")
    void successOnUpdatingDataBeforeExecutingSubjectMethod() throws SQLException {
        // given
        updateEnabledFlag();
        jdbcOperations.getConnection().commit();

        // when
        final var results = subject.getEnabled();

        // then
        assertAll(
                () -> assertTrue(results.contains(2L)),
                () -> assertTrue(getUpdatedEnabledFlag())
        );
    }

    @Test
    @DisplayName("When commiting transactions to update data using TransactionOperations, subject should see the update")
    void successOnUpdatingDataBeforeExecutingSubjectMethodAlternative() {
        // given
        transactionOperations.executeWrite(s -> {
            updateEnabledFlag();
            return null;
        });

        // when
        final var results = subject.getEnabled();

        // then
        assertAll(
                () -> assertTrue(results.contains(2L)),
                () -> assertTrue(getUpdatedEnabledFlag())
        );
    }

    private void updateEnabledFlag() {
        jdbcOperations.prepareStatement(
                "UPDATE my_entity SET enabled = TRUE WHERE id = 2",
                PreparedStatement::executeUpdate
        );
    }

    private boolean getUpdatedEnabledFlag() {
        return jdbcOperations.prepareStatement(
                "SELECT enabled AS e FROM my_entity WHERE id = 2",
                statement -> {
                    final var result = statement.executeQuery();
                    if (result.next()) {
                        return result.getBoolean("e");
                    }
                    throw new RuntimeException("Expected a value!");
                }
        );
    }
}
