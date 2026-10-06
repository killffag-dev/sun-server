package naryn.sun.utility.math.pool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Высокопроизводительный пул объектов для устранения GC-аллокаций в циклах рендера и тиков.
 * Потокобезопасен через ThreadLocal или прямое использование на конкретном потоке.
 *
 * @param <T> тип пулируемого объекта
 */
public class ObjectPool<T> {

    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxCapacity;

    private final ThreadLocal<Deque<T>> pool = ThreadLocal.withInitial(ArrayDeque::new);

    public ObjectPool(Supplier<T> factory, Consumer<T> resetter, int maxCapacity) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxCapacity = maxCapacity;
    }

    public ObjectPool(Supplier<T> factory, Consumer<T> resetter) {
        this(factory, resetter, 64);
    }

    public T obtain() {
        Deque<T> localQueue = this.pool.get();
        if (localQueue.isEmpty()) {
            return this.factory.get();
        }
        return localQueue.pop();
    }

    public void release(T object) {
        if (object == null) {
            return;
        }
        if (this.resetter != null) {
            this.resetter.accept(object);
        }
        Deque<T> localQueue = this.pool.get();
        if (localQueue.size() < this.maxCapacity) {
            localQueue.push(object);
        }
    }
}
