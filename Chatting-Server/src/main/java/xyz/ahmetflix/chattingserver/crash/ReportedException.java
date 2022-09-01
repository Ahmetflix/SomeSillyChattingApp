package xyz.ahmetflix.chattingserver.crash;

public class ReportedException extends RuntimeException
{
    private final CrashReport theReportedExceptionCrashReport;

    public ReportedException(CrashReport report)
    {
        this.theReportedExceptionCrashReport = report;
    }

    public CrashReport getCrashReport()
    {
        return this.theReportedExceptionCrashReport;
    }

    public Throwable getCause()
    {
        return this.theReportedExceptionCrashReport.getCause();
    }

    public String getMessage()
    {
        return this.theReportedExceptionCrashReport.getDescription();
    }
}
