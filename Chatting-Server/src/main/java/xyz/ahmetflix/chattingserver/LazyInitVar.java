package xyz.ahmetflix.chattingserver;

public abstract class LazyInitVar<T> {
    private T var;
    private boolean cached = false;

    public LazyInitVar() {
    }

    public T get() {
        if (!this.cached) {
            this.cached = true;
            this.var = this.init();
        }

        return this.var;
    }

    protected abstract T init();
}