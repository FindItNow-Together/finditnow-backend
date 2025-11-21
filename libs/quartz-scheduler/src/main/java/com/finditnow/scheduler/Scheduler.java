package com.finditnow.scheduler;

import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

public class Scheduler {
    private static org.quartz.Scheduler scheduler;

    public static void init() throws Exception {
        if (scheduler == null) {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        }
    }

    public static org.quartz.Scheduler get() {
        return scheduler;
    }

    public static void shutdown() throws SchedulerException {
        scheduler.shutdown();
    }
}
