package com.ctux.ae2craftingtime.mc1201;

import appeng.api.config.LockCraftingMode;
import com.ctux.ae2craftingtime.core.ProviderDispatchTracker.AttemptResult;

public final class ProviderDispatchContext {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    public static Scope begin(Object provider) {
        var previous = CURRENT.get();
        var frame = new Frame(provider);
        CURRENT.set(frame);
        return new Scope(previous, frame);
    }

    public static void lock(Object provider, LockCraftingMode reason) {
        var frame = current(provider);
        if (frame != null) {
            frame.checked = true;
            frame.locked = reason != LockCraftingMode.NONE;
        }
    }

    public static void dedicated(Object provider, boolean acceptsPlans) {
        var frame = current(provider);
        if (frame != null && acceptsPlans) {
            frame.dedicatedRoute = true;
        }
    }

    public static void dedicatedResult(Object provider, boolean accepted) {
        var frame = current(provider);
        if (frame != null && !accepted) {
            frame.dedicatedRejected = true;
        }
    }

    public static void target(Object provider, boolean present) {
        var frame = current(provider);
        if (frame != null && present) {
            frame.targets++;
        }
    }

    public static void blocked(boolean blocked) {
        var frame = CURRENT.get();
        if (frame != null && blocked) {
            frame.rejections++;
        }
    }

    public static void acceptsInputs(Object provider, boolean accepts) {
        var frame = current(provider);
        if (frame != null && !accepts) {
            frame.rejections++;
        }
    }

    public static void externalPush(Object provider, boolean supported) {
        var frame = current(provider);
        if (frame != null && !supported) {
            frame.unsupported = true;
        }
    }

    private static Frame current(Object provider) {
        var frame = CURRENT.get();
        return frame != null && frame.provider == provider ? frame : null;
    }

    private static final class Frame {
        private final Object provider;
        private boolean checked;
        private boolean locked;
        private boolean dedicatedRoute;
        private boolean dedicatedRejected;
        private boolean unsupported;
        private int targets;
        private int rejections;

        private Frame(Object provider) {
            this.provider = provider;
        }

        private AttemptResult finish(boolean success) {
            if (success) {
                return AttemptResult.SUCCESS;
            }
            if (locked) {
                return AttemptResult.LOCKED;
            }
            if (!checked || dedicatedRoute || dedicatedRejected || unsupported) {
                return AttemptResult.UNKNOWN;
            }
            if (targets == 0) {
                return AttemptResult.NO_TARGET;
            }
            return rejections == targets ? AttemptResult.INPUT_BLOCKED : AttemptResult.UNKNOWN;
        }
    }

    public static final class Scope implements AutoCloseable {
        private final Frame previous;
        private final Frame frame;
        private boolean closed;

        private Scope(Frame previous, Frame frame) {
            this.previous = previous;
            this.frame = frame;
        }

        public AttemptResult finish(boolean success) {
            return frame.finish(success);
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    private ProviderDispatchContext() {
    }
}
