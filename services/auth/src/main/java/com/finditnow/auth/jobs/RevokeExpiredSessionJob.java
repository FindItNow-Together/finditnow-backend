package com.finditnow.auth.jobs;

import com.finditnow.auth.AuthApp;
import com.finditnow.auth.dao.AuthDao;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class RevokeExpiredSessionJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(RevokeExpiredSessionJob.class);


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            logger.info("Starting expired session revocation job");

            // Get services from AuthApp
            AuthDao authDao = AuthApp.getAuthDao();

            // Execute the cleanup
            try (Connection conn = authDao.getDataSource().getConnection()) {
                int revokedCount = authDao.sessionDao.revokeExpiredSessions(conn);
                logger.info("Revoked {} expired sessions", revokedCount);
            }

        } catch (Exception e) {
            logger.error("Failed to revoke expired sessions", e);
            throw new JobExecutionException("Failed to revoke expired sessions", e);
        }
    }

    public static void runJob(){
        try {
            // Define the job
            JobDetail job = JobBuilder.newJob(RevokeExpiredSessionJob.class)
                    .withIdentity("revokeExpiredSessions", "auth")
                    .build();

            // Trigger every hour (adjust as needed)
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("revokeExpiredSessionsTrigger", "auth")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(30)
                            .repeatForever())
                    .build();

            com.finditnow.scheduler.Scheduler.get().scheduleJob(job, trigger);
            logger.info("Scheduled RevokeExpiredSessionJob to run every 30 minutes");
        } catch (SchedulerException e) {
            logger.error("Failed to initialize scheduler: {}; continuing without it", e.getMessage(), e);
        }
    }
}
