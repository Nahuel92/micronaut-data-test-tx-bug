package io.github.nahuel92;

import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Singleton
public class Service {
    private final EntityRepository entityRepository;

    Service(final EntityRepository entityRepository) {
        this.entityRepository = entityRepository;
    }

    public static List<Long> execute(final Callable<List<Long>> callable) {
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var submit = executor.submit(callable);
            return submit.get(1, TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException | TimeoutException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Long> getEnabledBugged() {
        return Stream.of((Callable<List<Long>>) entityRepository::getEnabled)
                .map(Service::execute)          // Run IO-bound task on another thread
                .flatMap(Collection::stream)
                .toList();
    }

    public List<Long> getEnabledGood() {
        return entityRepository.getEnabled();
    }
}
