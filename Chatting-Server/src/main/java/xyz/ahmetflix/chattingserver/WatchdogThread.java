package xyz.ahmetflix.chattingserver;

import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

public class WatchdogThread extends Thread
{

    private static WatchdogThread instance;
    private final long timeoutTime;
    private volatile long lastTick;
    private volatile boolean stopping;

    private WatchdogThread(long timeoutTime)
    {
        super( "Watchdog Thread" );
        this.timeoutTime = timeoutTime;
    }

    public static void doStart(int timeoutTime)
    {
        if ( instance == null )
        {
            instance = new WatchdogThread( timeoutTime * 1000L);
            instance.start();
        }
    }

    public static void tick()
    {
        instance.lastTick = System.currentTimeMillis();
    }

    public static void doStop()
    {
        if ( instance != null )
        {
            instance.stopping = true;
        }
    }

    @Override
    public void run()
    {
        while ( !stopping )
        {
            if ( lastTick != 0 && System.currentTimeMillis() > lastTick + timeoutTime )
            {
                Logger log = Server.LOGGER;
                log.fatal("The server has stopped responding!");
                log.fatal("------------------------------");
                log.fatal("Server thread dump:");
                dumpThread(ManagementFactory.getThreadMXBean().getThreadInfo(Server.getInstance().primaryThread.getId(), Integer.MAX_VALUE), log);
                log.fatal("------------------------------");
                log.fatal("Entire Thread Dump:");
                ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true,true);
                for ( ThreadInfo thread : threads )
                {
                    dumpThread( thread, log );
                }
                log.fatal("------------------------------" );
                break;
            }

            try
            {
                sleep( 10000 );
            } catch ( InterruptedException ex )
            {
                interrupt();
            }
        }
    }

    private static void dumpThread(ThreadInfo thread, Logger log)
    {
        log.fatal("------------------------------");
        log.fatal("Current Thread: " + thread.getThreadName());
        log.fatal("\tPID: " + thread.getThreadId()
                + " | Suspended: " + thread.isSuspended()
                + " | Native: " + thread.isInNative()
                + " | State: " + thread.getThreadState());
        if ( thread.getLockedMonitors().length != 0 )
        {
            log.fatal("\tThread is waiting on monitor(s):");
            for ( MonitorInfo monitor : thread.getLockedMonitors())
            {
                log.fatal("\t\tLocked on:" + monitor.getLockedStackFrame());
            }
        }
        log.fatal("\tStack:");
        for ( StackTraceElement stack : thread.getStackTrace() )
        {
            log.fatal("\t\t" + stack );
        }
    }
}
