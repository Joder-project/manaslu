package org.manaslu.cache.core;

import lombok.extern.slf4j.Slf4j;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 10)
@Fork(1)
@Slf4j
public class BenchmarkEnhance {

    @Benchmark
    public void raw(Wrapper state) {
        state.raw.update("Hello");
        state.raw.dumpStrategy.update(new UpdateInfo(state.raw, Set.of("name")));
    }

    @Benchmark
    public void proxy(Wrapper state) {
        state.proxy.update("Hello");
    }

    @State(Scope.Benchmark)
    public static class Wrapper {
        UserEntity raw;
        UserEntity proxy;

        @Setup
        public void setup() {
            raw = new UserEntity();
            var dumpStrategy = new NoDumpStrategy<Integer, UserEntity>();
            raw.initialize(null, dumpStrategy);
            var entityTypeManager = new EntityTypeManager();
            entityTypeManager.registerTypes(List.of(UserEntity.class));
            proxy = entityTypeManager.newEnhance(1, raw);
            proxy.initialize(null, dumpStrategy);
        }
    }
}
