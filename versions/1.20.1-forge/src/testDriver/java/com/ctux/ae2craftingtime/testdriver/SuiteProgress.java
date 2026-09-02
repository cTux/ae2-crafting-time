package com.ctux.ae2craftingtime.testdriver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class SuiteProgress {
    record CaseResult(String scenario, String world, String result, String startedAt, String finishedAt) { }
    record Result(int schema, boolean complete, String result, long processId, List<CaseResult> cases) { }
    private final List<CaseResult> cases = new ArrayList<>();
    private int index;

    SuiteProgress(List<DriverOptions> options) {
        options.forEach(option -> cases.add(new CaseResult(option.scenario(), option.world(), "NOT_RUN", null, null)));
    }

    void start(Instant now) {
        var current = cases.get(index);
        cases.set(index, new CaseResult(current.scenario(), current.world(), "RUNNING", now.toString(), null));
    }

    boolean finish(boolean passed, Instant now) {
        var current = cases.get(index);
        cases.set(index, new CaseResult(current.scenario(), current.world(), passed ? "PASS" : "FAIL",
                current.startedAt(), now.toString()));
        index++;
        return passed && index < cases.size();
    }

    Result snapshot(long processId) {
        boolean passed = cases.stream().allMatch(item -> item.result().equals("PASS"));
        boolean failed = cases.stream().anyMatch(item -> item.result().equals("FAIL"));
        return new Result(1, passed, passed ? "PASS" : failed ? "FAIL" : "RUNNING", processId, List.copyOf(cases));
    }
}
