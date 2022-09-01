package xyz.ahmetflix.chattingserver.crash;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.Callable;

public class CrashReportSystemDetails {
    private final CrashReport crashReport;
    private final String name;
    private final List<CrashReportDetail> children = Lists.newArrayList();
    private StackTraceElement[] stackTrace = new StackTraceElement[0];

    public CrashReportSystemDetails(CrashReport report, String name) {
        this.crashReport = report;
        this.name = name;
    }

    public void addCrashSectionCallable(String sectionName, Callable<String> callable) {
        try {
            this.addCrashSection(sectionName, callable.call());
        } catch (Throwable throwable) {
            this.addCrashSectionThrowable(sectionName, throwable);
        }

    }

    public void addCrashSection(String sectionName, Object value) {
        this.children.add(new CrashReportDetail(sectionName, value));
    }

    public void addCrashSectionThrowable(String sectionName, Throwable throwable) {
        this.addCrashSection(sectionName, throwable);
    }

    public int getPrunedStackTrace(int var1) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length <= 0) {
            return 0;
        } else {
            this.stackTrace = new StackTraceElement[stackTraceElements.length - 3 - var1];
            System.arraycopy(stackTraceElements, 3 + var1, this.stackTrace, 0, this.stackTrace.length);
            return this.stackTrace.length;
        }
    }

    public boolean firstTwoElementsOfStackTraceMatch(StackTraceElement var1, StackTraceElement var2) {
        if (this.stackTrace.length != 0 && var1 != null) {
            StackTraceElement var3 = this.stackTrace[0];
            if (var3.isNativeMethod() == var1.isNativeMethod() && var3.getClassName().equals(var1.getClassName()) && var3.getFileName().equals(var1.getFileName()) && var3.getMethodName().equals(var1.getMethodName())) {
                if (var2 != null != this.stackTrace.length > 1) {
                    return false;
                } else if (var2 != null && !this.stackTrace[1].equals(var2)) {
                    return false;
                } else {
                    this.stackTrace[0] = var1;
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void trimStackTraceEntriesFromBottom(int amount) {
        StackTraceElement[] stackTraceElements = new StackTraceElement[this.stackTrace.length - amount];
        System.arraycopy(this.stackTrace, 0, stackTraceElements, 0, stackTraceElements.length);
        this.stackTrace = stackTraceElements;
    }

    public void appendToStringBuilder(StringBuilder builder) {
        builder.append("-- ").append(this.name).append(" --\n");
        builder.append("Details:");

        for (CrashReportDetail detail : this.children) {
            builder.append("\n\t");
            builder.append(detail.getKey());
            builder.append(": ");
            builder.append(detail.getValue());
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            builder.append("\nStacktrace:");

            for (StackTraceElement stackTraceElement : this.stackTrace) {
                builder.append("\n\tat ");
                builder.append(stackTraceElement.toString());
            }
        }

    }

    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    static class CrashReportDetail {
        private final String key;
        private final String value;

        public CrashReportDetail(String key, Object value) {
            this.key = key;
            if (value == null) {
                this.value = "~~NULL~~";
            } else if (value instanceof Throwable) {
                Throwable var3 = (Throwable)value;
                this.value = "~~ERROR~~ " + var3.getClass().getSimpleName() + ": " + var3.getMessage();
            } else {
                this.value = value.toString();
            }

        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }
}
