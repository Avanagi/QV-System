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

    // Для сложных объектов (История пользователя - JSON)
    private final RedisTemplate<String, Object> redisTemplate;
    // Для простых счетчиков и множеств (Статистика - Строки/Числа)
    private final StringRedisTemplate stringRedisTemplate;

    public void saveToHistory(VoteArchivedEvent event) {
        // --- 1. СОХРАНЕНИЕ ЛИЧНОЙ ИСТОРИИ (JSON) ---
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

        // Используем Hash, чтобы перезаписывать дубликаты (Идемпотентность)
        redisTemplate.opsForHash().put(userKey, hashKey, record);
        redisTemplate.expire(userKey, Duration.ofDays(30));

        // --- 2. ОБНОВЛЕНИЕ СТАТИСТИКИ ОПРОСА ---
        String projectKey = "project_stats:" + event.getPollId();
        String participantsKey = "project_participants:" + event.getPollId();

        // А. Увеличиваем общую сумму квадратичных голосов
        stringRedisTemplate.opsForHash().increment(projectKey, "totalVotes", event.getVoteCount());

        // Б. Увеличиваем счетчик конкретной опции (например, "option:55")
        // Важно: используем OptionId, а не ProjectId
        stringRedisTemplate.opsForHash().increment(projectKey, "option:" + event.getProjectId(), event.getVoteCount());

        // В. Добавляем ID пользователя в Множество (Set)
        // Set хранит только уникальные значения. Если юзер уже голосовал, дубля не будет.
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

        // 1. Получаем все счетчики голосов (Total + Options)
        Map<Object, Object> allStats = stringRedisTemplate.opsForHash().entries(projectKey);

        // 2. Получаем количество УНИКАЛЬНЫХ участников (размер множества)
        Long uniqueParticipants = stringRedisTemplate.opsForSet().size(participantsKey);

        ProjectStats stats = new ProjectStats();
        stats.setProjectId(pollId);
        // Безопасно преобразуем Long в Integer (для DTO)
        stats.setParticipants(uniqueParticipants != null ? uniqueParticipants.intValue() : 0);

        Map<String, Integer> optionStats = new HashMap<>();
        int totalVotes = 0;

        // Парсим данные из Redis Hash
        for (Map.Entry<Object, Object> entry : allStats.entrySet()) {
            String field = entry.getKey().toString();
            int value = Integer.parseInt(entry.getValue().toString());

            if (field.equals("totalVotes")) {
                totalVotes = value;
            } else if (field.startsWith("option:")) {
                // Извлекаем ID опции из ключа "option:123"
                String optionId = field.split(":")[1];
                optionStats.put(optionId, value);
            }
        }

        stats.setTotalVotes(totalVotes);
        stats.setOptionStats(optionStats);

        return stats;
    }
}