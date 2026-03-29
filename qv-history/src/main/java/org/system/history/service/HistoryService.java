package org.system.history.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.system.common.event.VoteArchivedEvent;
import org.system.history.model.HistoryRecord;
import org.system.history.model.ProjectStats;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                event.getPollId(),
                event.getPollTitle(),
                event.getOptionText(),
                event.getTimestamp().toString()
        );

        redisTemplate.opsForHash().put(userKey, hashKey, record);
        redisTemplate.expire(userKey, Duration.ofDays(30));

        String projectKey = "project_stats:" + event.getPollId();
        String participantsKey = "project_participants:" + event.getPollId();

        stringRedisTemplate.opsForHash().increment(projectKey, "totalVotes", event.getVoteCount());

        stringRedisTemplate.opsForHash().increment(projectKey, "option:" + event.getProjectId(), event.getVoteCount());

        stringRedisTemplate.opsForSet().add(participantsKey, event.getUserId().toString());

        log.info("📊 Статистика обновлена для Poll {}: Option {} (+{}), User {}",
                event.getPollId(), event.getProjectId(), event.getVoteCount(), event.getUserId());
    }

    public List<Object> getUserHistory(Long userId) {
        String key = "history:" + userId;
        return redisTemplate.opsForHash().values(key);
    }

    public ProjectStats getProjectStats(Long pollId) {
        String projectKey = "project_stats:" + pollId;
        String participantsKey = "project_participants:" + pollId;

        Map<Object, Object> allStats = stringRedisTemplate.opsForHash().entries(projectKey);

        Long uniqueParticipants = stringRedisTemplate.opsForSet().size(participantsKey);

        ProjectStats stats = new ProjectStats();
        stats.setProjectId(pollId);
        stats.setParticipants(uniqueParticipants != null ? uniqueParticipants.intValue() : 0);

        Map<String, Integer> optionStats = new HashMap<>();
        int totalVotes = 0;

        for (Map.Entry<Object, Object> entry : allStats.entrySet()) {
            String field = entry.getKey().toString();
            int value = Integer.parseInt(entry.getValue().toString());

            if (field.equals("totalVotes")) {
                totalVotes = value;
            } else if (field.startsWith("option:")) {
                String optionId = field.split(":")[1];
                optionStats.put(optionId, value);
            }
        }

        stats.setTotalVotes(totalVotes);
        stats.setOptionStats(optionStats);

        return stats;
    }
}