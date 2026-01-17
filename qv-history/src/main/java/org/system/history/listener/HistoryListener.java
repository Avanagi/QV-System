package org.system.history.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;
import org.system.common.event.VoteArchivedEvent;
import org.system.history.service.HistoryService;

@Component
@RequiredArgsConstructor
public class HistoryListener {

    private final HistoryService historyService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("history-queue"), // Своя очередь
            exchange = @Exchange(value = "saga-exchange", type = ExchangeTypes.TOPIC),
            key = "vote.archived"
    ))
    public void handleArchivedEvent(VoteArchivedEvent event) {
        historyService.saveToHistory(event);
    }
}