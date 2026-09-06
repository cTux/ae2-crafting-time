package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import appeng.api.config.LockCraftingMode;
import com.ctux.ae2craftingtime.core.ProviderDispatchTracker.AttemptResult;
import org.junit.jupiter.api.Test;

class ProviderDispatchContextTest {
    @Test
    void classifiesOnlyDirectCompleteProviderEvidence() {
        var provider = new Object();
        try (var scope = ProviderDispatchContext.begin(provider)) {
            assertEquals(AttemptResult.UNKNOWN, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            assertEquals(AttemptResult.NO_TARGET, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            ProviderDispatchContext.target(provider, true);
            ProviderDispatchContext.blocked(true);
            assertEquals(AttemptResult.INPUT_BLOCKED, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            ProviderDispatchContext.target(provider, true);
            ProviderDispatchContext.acceptsInputs(provider, false);
            assertEquals(AttemptResult.INPUT_BLOCKED, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.LOCK_WHILE_HIGH);
            assertEquals(AttemptResult.LOCKED, scope.finish(false));
            assertEquals(AttemptResult.SUCCESS, scope.finish(true));
        }
    }

    @Test
    void dedicatedUnknownPartialAcceptanceMismatchesAndNestedScopesStayUnknown() {
        var provider = new Object();
        var other = new Object();
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            ProviderDispatchContext.dedicated(provider, true);
            ProviderDispatchContext.dedicatedResult(provider, false);
            assertEquals(AttemptResult.UNKNOWN, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            ProviderDispatchContext.target(provider, true);
            ProviderDispatchContext.acceptsInputs(provider, true);
            assertEquals(AttemptResult.UNKNOWN, scope.finish(false));
        }
        try (var scope = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            ProviderDispatchContext.externalPush(provider, false);
            assertEquals(AttemptResult.UNKNOWN, scope.finish(false));
        }
        try (var outer = ProviderDispatchContext.begin(provider)) {
            ProviderDispatchContext.lock(other, LockCraftingMode.LOCK_WHILE_LOW);
            ProviderDispatchContext.target(provider, false);
            try (var inner = ProviderDispatchContext.begin(other)) {
                ProviderDispatchContext.lock(other, LockCraftingMode.LOCK_WHILE_LOW);
                assertEquals(AttemptResult.LOCKED, inner.finish(false));
            }
            ProviderDispatchContext.lock(provider, LockCraftingMode.NONE);
            assertEquals(AttemptResult.NO_TARGET, outer.finish(false));
            outer.close();
        }
    }
}
