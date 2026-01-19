package org.system.voting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.system.voting.dto.PollCreationRequest;
import org.system.voting.entity.Poll;
import org.system.voting.service.PollService;

import java.util.List;

@RestController
@RequestMapping("/api/projects") // Оставим старый путь для совместимости с Gateway
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    // Создать опрос
    @PostMapping
    public Poll createPoll(@RequestBody PollCreationRequest request) {
        return pollService.createPoll(request);
    }

    // Разблокировать опрос по коду (ввод кода)
    @PostMapping("/unlock")
    public Poll unlockPoll(@RequestParam Long userId, @RequestParam String code) {
        return pollService.unlockPoll(userId, code);
    }

    // Получить мои открытые опросы (доступные)
    @GetMapping("/access/{userId}")
    public List<Poll> getMyAccess(@PathVariable Long userId) {
        return pollService.getMyUnlockedPolls(userId);
    }

    // Получить опросы, созданные мной (Creator)
    @GetMapping("/my/{creatorId}")
    public List<Poll> getCreatedByMe(@PathVariable Long creatorId) {
        return pollService.getCreatedByMe(creatorId);
    }
}