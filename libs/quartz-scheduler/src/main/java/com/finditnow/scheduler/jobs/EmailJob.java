package com.finditnow.scheduler.jobs;

import com.finditnow.mail.MailService;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

public class EmailJob implements Job {
    @Override
    public void execute(JobExecutionContext ctx) {
        JobDataMap data = ctx.getMergedJobDataMap();

        MailService.getInstance().sendMail(
                data.getString("email"),
                data.getString("subject"),
                data.getString("body")
        );
    }
}
