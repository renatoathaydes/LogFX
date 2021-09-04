package com.athaydes.logfx.concurrency;

public final class IdentifiableRunnable implements Runnable {
    private final Object id;
    private final Runnable runnable;

    public IdentifiableRunnable( Object id, Runnable runnable ) {
        this.id = id;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        runnable.run();
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        IdentifiableRunnable that = ( IdentifiableRunnable ) o;

        return id.equals( that.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
