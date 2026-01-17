package org.system.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.system.common.event.VoteArchivedEvent;
import org.system.history.model.HistoryRecord;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveToHistory(VoteArchivedEvent event) {
        String key = "history:" + event.getUserId();

        String hashKey = event.getVoteId().toString();

        HistoryRecord record = new HistoryRecord(
                event.getVoteId(),
                event.getProjectId(),
                event.getVoteCount(),
                event.getCost(),
                event.getTxHash(),
                event.getTimestamp().toString()
        );

        redisTemplate.opsForHash().put(key, hashKey, record);

        redisTemplate.expire(key, Duration.ofDays(30));

        log.info("📜 Записано (или обновлено) в Redis [User {}]: Vote {}", event.getUserId(), event.getVoteId());
    }

    public List<Object> getUserHistory(Long userId) {
        String key = "history:" + userId;
        return redisTemplate.opsForHash().values(key);
    }
}