package org.system.voting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.system.voting.dto.PollCreationRequest;
import org.system.voting.entity.Poll;
import org.system.voting.service.PollService;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    public Poll createPoll(@RequestBody PollCreationRequest request) {
        return pollService.createPoll(request);
    }

    @PostMapping("/unlock")
    public Poll unlockPoll(@RequestParam Long userId, @RequestParam String code) {
        return pollService.unlockPoll(userId, code);
    }

    @GetMapping("/access/{userId}")
    public List<Poll> getMyAccess(@PathVariable Long userId) {
        return pollService.getMyUnlockedPolls(userId);
    }

    @GetMapping("/my/{creatorId}")
    public List<Poll> getCreatedByMe(@PathVariable Long creatorId) {
        return pollService.getCreatedByMe(creatorId);
    }
}