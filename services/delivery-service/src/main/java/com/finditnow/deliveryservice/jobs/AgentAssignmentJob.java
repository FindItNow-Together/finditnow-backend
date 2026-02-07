package com.finditnow.deliveryservice.jobs;

import com.finditnow.deliveryservice.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentAssignmentJob {
    private final AssignmentService assignmentService;

    @Scheduled(cron = "*/30 * * * * *")
    public void runAssignmentJob() {
        log.info("Executing agent assignment job");
        assignmentService.attemptAssignment();
    }
}
