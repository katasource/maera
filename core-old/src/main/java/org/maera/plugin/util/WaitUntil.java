package org.maera.plugin.util;

import com.atlassian.util.concurrent.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Utility methods for synchronising on asynchronous processes
 */
public class WaitUntil {
    private static final Logger log = LoggerFactory.getLogger(WaitUntil.class);

    /**
     * Default {@link #STARTUP_WAIT}
     */
    private static final int DEFAULT_STARTUP_WAIT = 60;

    /**
     * Number of seconds to wait before a plugin starts up. Configurable through
     * system property "org.maera.plugin.startup.wait". Defaults to
     * {@link #DEFAULT_STARTUP_WAIT}
     */
    private static final int STARTUP_WAIT = Integer.getInteger("org.maera.plugin.startup.wait", DEFAULT_STARTUP_WAIT);

    private WaitUntil() {
    }

    /**
     * Invokes the wait condition, trying every second for 10 seconds
     *
     * @param waitCondition The condition that determines when to stop waiting
     * @return True if the condition returned true
     */
    public static boolean invoke(final WaitCondition waitCondition) {
        return invoke(waitCondition, STARTUP_WAIT);
    }

    /**
     * Invokes the wait condition, trying every second for the configured
     * seconds
     *
     * @param waitCondition The condition that determines when to stop waiting
     * @param tries         The number of tries to attempt
     * @return True if the condition returned true
     */
    public static boolean invoke(final WaitCondition waitCondition, final int tries) {
        final int secondMillis = 1000;
        return invoke(waitCondition, tries * secondMillis, TimeUnit.MILLISECONDS, secondMillis);
    }

    /**
     * Invokes the wait condition, trying every second for the configured
     * seconds
     *
     * @param waitCondition The condition that determines when to stop waiting
     * @param time          the amount of time to wait
     * @param unit          the time unit time is specified in
     * @param retryInterval how often to re-check the condition (specified in
     *                      the supplied TimeUnit)
     * @return True if the condition returned true
     */
    public static boolean invoke(final WaitCondition waitCondition, final int time, final TimeUnit unit, final int retryInterval) {
        final Timeout timeout = Timeout.getMillisTimeout(time, unit);
        boolean successful = false;
        while (!timeout.isExpired()) {
            if (waitCondition.isFinished()) {
                successful = true;
                break;
            }

            if (log.isInfoEnabled()) {
                log.info(waitCondition.getWaitMessage() + ", " + TimeUnit.SECONDS.convert(timeout.getTime(), timeout.getUnit()) + " seconds remaining");
            }
            try {
                sleep(unit.toMillis(retryInterval));
            }
            catch (final InterruptedException e) {
                break;
            }
        }
        return successful;
    }

    /**
     * The condition to determine when to stop waiting
     */
    public interface WaitCondition {
        /**
         * If the condition has been finished
         *
         * @return True if finished and should stop waiting
         */
        boolean isFinished();

        /**
         * Gets the wait message to log for each try
         *
         * @return The string to print describing why the code is waiting
         */
        String getWaitMessage();
    }
}
