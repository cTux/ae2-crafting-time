package com.ctux.ae2craftingtime.mc1201;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.KeyCounter;
import com.ctux.ae2craftingtime.core.ProviderDispatchTracker.Evaluation;
import java.util.Iterator;

public final class ProviderDispatchObserver {
    private final String networkId;
    private final Object scope;
    private final IPatternDetails pattern;
    private final long tick;
    private final Evaluation evaluation = new Evaluation();
    private boolean presenceObserved;
    private boolean completed;

    public ProviderDispatchObserver(String networkId, Object scope, IPatternDetails pattern, long tick) {
        this.networkId = networkId;
        this.scope = scope;
        this.pattern = pattern;
        this.tick = tick;
    }

    public Iterator<ICraftingProvider> iterator(Iterable<ICraftingProvider> providers) {
        var delegate = providers.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                var hasNext = delegate.hasNext();
                if (hasNext) {
                    observePresence(true);
                } else {
                    observePresence(false);
                    evaluation.exhausted();
                    complete(evaluation.result());
                }
                return hasNext;
            }

            @Override
            public ICraftingProvider next() {
                var provider = delegate.next();
                observePresence(true);
                evaluation.candidate();
                return provider;
            }

            @Override
            public void remove() {
                delegate.remove();
            }
        };
    }

    public boolean busy(ICraftingProvider provider) {
        var busy = provider.isBusy();
        evaluation.busy(busy);
        return busy;
    }

    public boolean push(ICraftingProvider provider, IPatternDetails dispatchedPattern, KeyCounter[] input) {
        try (var context = ProviderDispatchContext.begin(provider)) {
            var accepted = provider.pushPattern(dispatchedPattern, input);
            evaluation.attempt(context.finish(accepted));
            if (evaluation.succeeded()) {
                complete(null);
            }
            return accepted;
        }
    }

    public void finish() {
        complete(null);
    }

    private void observePresence(boolean hasProvider) {
        if (!presenceObserved) {
            presenceObserved = true;
            ProfilerBridge.observeProviders(networkId, scope, pattern, hasProvider);
        }
    }

    private void complete(com.ctux.ae2craftingtime.core.CraftingBlockReason reason) {
        if (completed) {
            return;
        }
        completed = true;
        ProfilerBridge.observeProviderDispatch(networkId, scope, pattern, reason, tick);
    }
}
