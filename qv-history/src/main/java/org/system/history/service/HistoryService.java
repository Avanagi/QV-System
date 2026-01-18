package org.system.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate; // <-- Импорт
import org.springframework.stereotype.Service;
import org.system.common.event.VoteArchivedEvent;
import org.system.history.model.HistoryRecord;
import org.system.history.model.ProjectStats;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    public void saveToHistory(VoteArchivedEvent event) {
        String userKey = "history:" + event.getUserId();
        String hashKey = event.getVoteId().toString();

        HistoryRecord record = new HistoryRecord(
                event.getVoteId(),
                event.getProjectId(),
                event.getVoteCount(),
                event.getCost(),
                event.getTxHash(),
                event.getTimestamp().toString()
        );

        redisTemplate.opsForHash().put(userKey, hashKey, record);
        redisTemplate.expire(userKey, Duration.ofDays(30));

        String projectKey = "project_stats:" + event.getProjectId();

        stringRedisTemplate.opsForHash().increment(projectKey, "totalVotes", event.getVoteCount());
        stringRedisTemplate.opsForHash().increment(projectKey, "participants", 1);

        log.info("📜 Записано в Redis [User {}] и обновлена статистика [Project {}]",
                event.getUserId(), event.getProjectId());
    }

    public List<Object> getUserHistory(Long userId) {
        String key = "history:" + userId;
        return redisTemplate.opsForHash().values(key);
    }

    public ProjectStats getProjectStats(Long projectId) {
        String key = "project_stats:" + projectId;

        Object rawVotes = stringRedisTemplate.opsForHash().get(key, "totalVotes");
        Object rawParticipants = stringRedisTemplate.opsForHash().get(key, "participants");

        int totalVotes = rawVotes != null ? Integer.parseInt(rawVotes.toString()) : 0;
        int participants = rawParticipants != null ? Integer.parseInt(rawParticipants.toString()) : 0;

        return new ProjectStats(projectId, totalVotes, participants);
    }
}