package com.finditnow.dispatcher;
import com.finditnow.mail.MailService;
import com.finditnow.scheduler.jobs.EmailJob;
import org.quartz.*;

import java.time.Instant;
import java.util.Date;

public class EmailDispatcher {
    private final MailService mailService;

    public EmailDispatcher() {
        this.mailService = MailService.getInstance();
    }

    public void send(String to, String subject, String body, boolean async) {
        if (!async) {
            mailService.sendMail(to, subject, body);
            return;
        }
        schedule(to, subject, body);
    }

    private void schedule(String to, String subject, String body) {
        try {
            JobDataMap map = new JobDataMap();
            map.put("email", to);
            map.put("subject", subject);
            map.put("body", body);
            map.put("service", mailService);

            JobDetail job = JobBuilder.newJob(EmailJob.class)
                    .withIdentity("emailJob:" + Instant.now().toEpochMilli())
                    .setJobData(map)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .startAt(Date.from(Instant.now().plusSeconds(3)))
                    .build();

            com.finditnow.scheduler.Scheduler.get().scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
