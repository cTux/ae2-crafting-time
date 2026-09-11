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

    static SuiteProgress resume(List<DriverOptions> options, java.nio.file.Path path) throws java.io.IOException {
        var saved = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(path), Result.class);
        var progress = new SuiteProgress(options);
        if (saved == null || saved.schema() != 1 || saved.complete() || !"RUNNING".equals(saved.result())
                || saved.cases() == null || saved.cases().size() != options.size()) {
            throw new IllegalArgumentException("invalid interrupted suite progress");
        }
        progress.cases.clear();
        progress.cases.addAll(saved.cases());
        while (progress.index < progress.cases.size()
                && progress.cases.get(progress.index).result().equals("PASS")) progress.index++;
        if (progress.index >= progress.cases.size()
                || !progress.cases.get(progress.index).result().equals("RUNNING")) {
            throw new IllegalArgumentException("suite progress has no interrupted case");
        }
        for (var i = 0; i < options.size(); i++) {
            var item = progress.cases.get(i);
            var option = options.get(i);
            if (!item.scenario().equals(option.scenario()) || !item.world().equals(option.world())
                    || (i < progress.index && !item.result().equals("PASS"))
                    || (i > progress.index && !item.result().equals("NOT_RUN"))) {
                throw new IllegalArgumentException("suite progress does not match its plan");
            }
        }
        return progress;
    }

    int index() { return index; }

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
