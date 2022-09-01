package xyz.ahmetflix.chattingserver.crash;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.ahmetflix.chattingserver.Server;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

public class CrashReport {

    private static final Logger LOGGER = LogManager.getLogger();
    private final String description;
    private final Throwable cause;
    private final CrashReportSystemDetails systemDetails = new CrashReportSystemDetails(this, "System Details");
    private final List<CrashReportSystemDetails> reportSections = Lists.newArrayList();
    private File reportFile;
    private boolean firstCategoryInCrashReport = true;
    private StackTraceElement[] stackTrace = new StackTraceElement[0];

    public CrashReport(String s, Throwable throwable) {
        this.description = s;
        this.cause = throwable;
        this.populateEnvironment();
    }

    private void populateEnvironment() {
        this.systemDetails.addCrashSectionCallable("Server Version", (Callable<String>) Server::getVersion);
        this.systemDetails.addCrashSectionCallable("Operating System", (Callable<String>) () -> System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version"));
        this.systemDetails.addCrashSectionCallable("Java Version", (Callable<String>) () -> System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        this.systemDetails.addCrashSectionCallable("Java VM Version", (Callable<String>) () -> System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor"));
        this.systemDetails.addCrashSectionCallable("Memory", (Callable<String>) () -> {
            Runtime runtime = Runtime.getRuntime();
            long i = runtime.maxMemory();
            long j = runtime.totalMemory();
            long k = runtime.freeMemory();
            long l = i / 1024L / 1024L;
            long i1 = j / 1024L / 1024L;
            long j1 = k / 1024L / 1024L;

            return k + " bytes (" + j1 + " MB) / " + j + " bytes (" + i1 + " MB) up to " + i + " bytes (" + l + " MB)";
        });
        this.systemDetails.addCrashSectionCallable("JVM Flags", (Callable<String>) () -> {
            RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
            List<String> list = runtimemxbean.getInputArguments();
            int i = 0;
            StringBuilder stringbuilder = new StringBuilder();

            for (String arg : list) {
                if (arg.startsWith("-X")) {
                    if (i++ > 0) {
                        stringbuilder.append(" ");
                    }

                    stringbuilder.append(arg);
                }
            }

            return String.format("%d total; %s", i, stringbuilder);
        });
    }

    public String getDescription() {
        return description;
    }

    public Throwable getCause() {
        return cause;
    }

    public void getSectionsInStringBuilder(StringBuilder stringbuilder) {
        if ((this.stackTrace == null || this.stackTrace.length <= 0) && this.reportSections.size() > 0) {
            this.stackTrace = ArrayUtils.subarray(this.reportSections.get(0).getStackTrace(), 0, 1);
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            stringbuilder.append("-- Head --\n");
            stringbuilder.append("Stacktrace:\n");

            for (StackTraceElement stacktraceelement : this.stackTrace) {
                stringbuilder.append("\t").append("at ").append(stacktraceelement.toString());
                stringbuilder.append("\n");
            }

            stringbuilder.append("\n");
        }

        for (CrashReportSystemDetails crashreportsystemdetails : this.reportSections) {
            crashreportsystemdetails.appendToStringBuilder(stringbuilder);
            stringbuilder.append("\n\n");
        }

        this.systemDetails.appendToStringBuilder(stringbuilder);
    }

    public String getCauseStackTraceOrString() {
        StringWriter stringwriter = null;
        PrintWriter printwriter = null;
        Throwable throwable = this.cause;

        if (this.cause.getMessage() == null) {
            if (this.cause instanceof NullPointerException) {
                throwable = new NullPointerException(this.description);
            } else if (this.cause instanceof StackOverflowError) {
                throwable = new StackOverflowError(this.description);
            } else if (this.cause instanceof OutOfMemoryError) {
                throwable = new OutOfMemoryError(this.description);
            }

            throwable.setStackTrace(this.cause.getStackTrace());
        }

        String s = throwable.toString();

        try {
            stringwriter = new StringWriter();
            printwriter = new PrintWriter(stringwriter);
            throwable.printStackTrace(printwriter);
            s = stringwriter.toString();
        } finally {
            IOUtils.closeQuietly(stringwriter);
            IOUtils.closeQuietly(printwriter);
        }

        return s;
    }

    public String getCompleteReport() {
        StringBuilder stringbuilder = new StringBuilder();

        stringbuilder.append("---- Minecraft Crash Report ----\n");
        stringbuilder.append("// ");
        stringbuilder.append(wittyComment());
        stringbuilder.append("\n\n");
        stringbuilder.append("Time: ");
        stringbuilder.append((new SimpleDateFormat()).format(new Date()));
        stringbuilder.append("\n");
        stringbuilder.append("Description: ");
        stringbuilder.append(this.description);
        stringbuilder.append("\n\n");
        stringbuilder.append(this.getCauseStackTraceOrString());
        stringbuilder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

        for (int i = 0; i < 87; ++i) {
            stringbuilder.append("-");
        }

        stringbuilder.append("\n\n");
        this.getSectionsInStringBuilder(stringbuilder);
        return stringbuilder.toString();
    }

    public boolean saveToFile(File file) {
        if (this.reportFile != null) {
            return false;
        } else {
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            try {
                FileWriter filewriter = new FileWriter(file);

                filewriter.write(this.getCompleteReport());
                filewriter.close();
                this.reportFile = file;
                return true;
            } catch (Throwable throwable) {
                CrashReport.LOGGER.error("Could not save crash report to " + file, throwable);
                return false;
            }
        }
    }

    public CrashReportSystemDetails getSystemDetails() {
        return this.systemDetails;
    }

    public CrashReportSystemDetails makeDetails(String s) {
        return this.makeCategoryDepth(s, 1);
    }

    public CrashReportSystemDetails makeCategoryDepth(String s, int i) {
        CrashReportSystemDetails crashreportsystemdetails = new CrashReportSystemDetails(this, s);

        if (this.firstCategoryInCrashReport) {
            int j = crashreportsystemdetails.getPrunedStackTrace(i);
            StackTraceElement[] astacktraceelement = this.cause.getStackTrace();
            StackTraceElement stacktraceelement = null;
            StackTraceElement stacktraceelement1 = null;
            int k = astacktraceelement.length - j;

            if (k < 0) {
                System.out.println("Negative index in crash report handler (" + astacktraceelement.length + "/" + j + ")");
            }

            if (astacktraceelement != null && 0 <= k && k < astacktraceelement.length) {
                stacktraceelement = astacktraceelement[k];
                if (astacktraceelement.length + 1 - j < astacktraceelement.length) {
                    stacktraceelement1 = astacktraceelement[astacktraceelement.length + 1 - j];
                }
            }

            this.firstCategoryInCrashReport = crashreportsystemdetails.firstTwoElementsOfStackTraceMatch(stacktraceelement, stacktraceelement1);
            if (j > 0 && !this.reportSections.isEmpty()) {
                CrashReportSystemDetails crashreportsystemdetails1 = (CrashReportSystemDetails) this.reportSections.get(this.reportSections.size() - 1);

                crashreportsystemdetails1.trimStackTraceEntriesFromBottom(j);
            } else if (astacktraceelement != null && astacktraceelement.length >= j && 0 <= k && k < astacktraceelement.length) {
                this.stackTrace = new StackTraceElement[k];
                System.arraycopy(astacktraceelement, 0, this.stackTrace, 0, this.stackTrace.length);
            } else {
                this.firstCategoryInCrashReport = false;
            }
        }

        this.reportSections.add(crashreportsystemdetails);
        return crashreportsystemdetails;
    }

    private static String wittyComment() {
        String[] astring = new String[] { "Who set us up the TNT?", "Everything\'s going to plan. No, really, that was supposed to happen.", "Uh... Did I do that?", "Oops.", "Why did you do that?", "I feel sad now :(", "My bad.", "I\'m sorry, Dave.", "I let you down. Sorry :(", "On the bright side, I bought you a teddy bear!", "Daisy, daisy...", "Oh - I know what I did wrong!", "Hey, that tickles! Hehehe!", "I blame Dinnerbone.", "You should try our sister game, Minceraft!", "Don\'t be sad. I\'ll do better next time, I promise!", "Don\'t be sad, have a hug! <3", "I just don\'t know what went wrong :(", "Shall we play a game?", "Quite honestly, I wouldn\'t worry myself about that.", "I bet Cylons wouldn\'t have this problem.", "Sorry :(", "Surprise! Haha. Well, this is awkward.", "Would you like a cupcake?", "Hi. I\'m Minecraft, and I\'m a crashaholic.", "Ooh. Shiny.", "This doesn\'t make any sense!", "Why is it breaking :(", "Don\'t do that.", "Ouch. That hurt :(", "You\'re mean.", "This is a token for 1 free hug. Redeem at your nearest Mojangsta: [~~HUG~~]", "There are four lights!", "But it works on my machine."};

        try {
            return astring[(int) (System.nanoTime() % (long) astring.length)];
        } catch (Throwable throwable) {
            return "Witty comment unavailable :(";
        }
    }

    public static CrashReport makeCrashReport(Throwable throwable, String descriptionIn) {
        CrashReport crashreport;

        if (throwable instanceof ReportedException) {
            crashreport = ((ReportedException) throwable).getCrashReport();
        } else {
            crashreport = new CrashReport(descriptionIn, throwable);
        }

        return crashreport;
    }
}
