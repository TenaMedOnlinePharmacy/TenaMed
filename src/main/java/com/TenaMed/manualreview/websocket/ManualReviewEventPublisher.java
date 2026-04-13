package com.TenaMed.manualreview.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ManualReviewEventPublisher {

    private static final String TASK_TOPIC = "/topic/tasks";

    private final SimpMessagingTemplate messagingTemplate;

    public void sendTaskCreated(UUID taskId) {
        messagingTemplate.convertAndSend(TASK_TOPIC, Map.of(
                "type", "TASK_CREATED",
                "taskId", taskId
        ));
    }

    public void sendTaskClaimed(UUID taskId, UUID userId) {
        messagingTemplate.convertAndSend(TASK_TOPIC, Map.of(
                "type", "TASK_CLAIMED",
                "taskId", taskId,
                "userId", userId
        ));
    }

    public void sendTaskCompleted(UUID taskId) {
        messagingTemplate.convertAndSend(TASK_TOPIC, Map.of(
                "type", "TASK_COMPLETED",
                "taskId", taskId
        ));
    }
}
