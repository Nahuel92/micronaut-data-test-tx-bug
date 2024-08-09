package io.github.nahuel92;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional
public abstract class EntityRepository {
    private final JdbcOperations jdbcOperations;

    EntityRepository(final JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    public List<Long> getEnabled() {
        return jdbcOperations.prepareStatement(
                "SELECT id AS id FROM my_entity WHERE enabled IS TRUE",
                statement -> {
                    final var resultSet = statement.executeQuery();
                    final var enabled = new ArrayList<Long>();
                    while (resultSet.next()) {
                        enabled.add(resultSet.getLong("id"));
                    }
                    return enabled;
                }
        );
    }
}
