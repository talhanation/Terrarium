package net.gegy1000.terrarium.server.world.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.gegy1000.terrarium.server.util.FutureUtil;
import net.gegy1000.terrarium.server.util.tuple.Tuple2;
import net.gegy1000.terrarium.server.util.tuple.Tuple3;
import net.gegy1000.terrarium.server.world.data.op.DataFunction;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class DataOp<T> {
    private final DataFunction<T> function;

    private Cache<DataView, CompletableFuture<Optional<T>>> cache;
    private Function<T, T> copy;

    private DataOp(DataFunction<T> function) {
        this.function = function;
    }

    public static <T> DataOp<T> of(DataFunction<T> function) {
        return new DataOp<>(function);
    }

    public static <T> DataOp<T> ofSync(Function<DataView, T> function) {
        return new DataOp<>(view -> {
            T result = function.apply(view);
            return CompletableFuture.completedFuture(Optional.of(result));
        });
    }

    public static <T> DataOp<T> completed(Optional<T> result) {
        return new DataOp<>(view -> CompletableFuture.completedFuture(result));
    }

    public DataOp<T> cached(Function<T, T> copy) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(4)
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .build();
        this.copy = copy;
        return this;
    }

    public CompletableFuture<Optional<T>> apply(DataView view) {
        if (this.cache == null) return this.function.apply(view);

        try {
            return this.cache.get(view, () -> this.function.apply(view))
                    .thenApply(opt -> opt.map(v -> this.copy.apply(v)));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public <U> DataOp<U> map(BiFunction<T, DataView, U> map) {
        return DataOp.of(view -> {
            CompletableFuture<Optional<T>> future = this.apply(view);
            return future.thenApply(opt -> opt.map(result -> map.apply(result, view)));
        });
    }

    public <U> DataOp<U> flatMap(BiFunction<T, DataView, Optional<U>> map) {
        return DataOp.of(view -> {
            CompletableFuture<Optional<T>> future = this.apply(view);
            return future.thenApply(opt -> opt.flatMap(result -> map.apply(result, view)));
        });
    }

    public static <A, B> DataOp<Tuple2<A, B>> join2(DataOp<A> a, DataOp<B> b) {
        return DataOp.of(view -> {
            return FutureUtil.join2(a.apply(view), b.apply(view))
                    .thenApply(tup -> {
                        if (tup.a.isPresent() && tup.b.isPresent()) {
                            return Optional.of(new Tuple2<>(tup.a.get(), tup.b.get()));
                        }
                        return Optional.empty();
                    });
        });
    }

    public static <A, B, C> DataOp<Tuple3<A, B, C>> join3(DataOp<A> a, DataOp<B> b, DataOp<C> c) {
        return DataOp.of(view -> {
            return FutureUtil.join3(a.apply(view), b.apply(view), c.apply(view))
                    .thenApply(tup -> {
                        if (tup.a.isPresent() && tup.b.isPresent() && tup.c.isPresent()) {
                            return Optional.of(new Tuple3<>(tup.a.get(), tup.b.get(), tup.c.get()));
                        }
                        return Optional.empty();
                    });
        });
    }
}
