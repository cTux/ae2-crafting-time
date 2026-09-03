package com.ctux.ae2craftingtime.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Checks only explicitly named bytecode members, without loading their classes. */
public final class IntegrationContract {
    private IntegrationContract() {}

    /** Empty member means class presence; otherwise use field:name or method:name. */
    public record Member(String owner, String member, String descriptorPattern) {}

    public record ClassInfo(String superName, Map<String, List<String>> members) {
        public ClassInfo {
            members = members.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, e -> List.copyOf(e.getValue())));
        }
    }

    public static String check(List<Member> contract, Function<String, ClassInfo> bytecode) {
        for (var member : contract) {
            if (!matches(member, bytecode)) {
                return "missing:" + member.owner() + "#" + member.member();
            }
        }
        return null;
    }

    private static boolean matches(Member member, Function<String, ClassInfo> bytecode) {
        var visited = new HashSet<String>();
        var owner = member.owner();
        while (owner != null && !owner.equals("java/lang/Object")) {
            if (!visited.add(owner)) {
                throw new IllegalStateException("Cyclic adapter class hierarchy: " + owner);
            }
            var info = bytecode.apply(owner);
            if (info == null) {
                return false;
            }
            if (member.member().isEmpty()) {
                return true;
            }
            if (info.members().getOrDefault(member.member(), List.of()).stream()
                    .anyMatch(d -> d.matches(member.descriptorPattern()))) {
                return true;
            }
            owner = info.superName();
        }
        return false;
    }
}
