package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CpuTtcResolver {
    public static <T> List<CpuTtcCache.Entry> resolve(List<Integer> requested, Map<T, Integer> serials, Collection<T> live,
            Predicate<T> busy, Function<T, OptionalLong> estimate) {
        return requested.stream().map(serial -> {
            var cpu = live.stream().filter(candidate -> serial.equals(serials.get(candidate))).findFirst();
            return new CpuTtcCache.Entry(serial,
                    cpu.filter(busy).map(estimate).orElseGet(OptionalLong::empty));
        }).toList();
    }

    private CpuTtcResolver() { }
}
