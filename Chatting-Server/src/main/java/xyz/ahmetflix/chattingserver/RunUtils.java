package xyz.ahmetflix.chattingserver;

import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class RunUtils {

    public static <V> V run(FutureTask<V> task, Logger logger) {
        try {
            task.run();
            return task.get();
        } catch (ExecutionException | InterruptedException exception) {
            logger.fatal("Error executing task", exception);
        }

        return null;
    }

}
