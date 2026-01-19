package org.system.voting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.system.voting.dto.VoteBatchRequest;
import org.system.voting.service.VoteService;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/batch")
    public ResponseEntity<String> castBatchVote(@RequestBody VoteBatchRequest request) {
        try {
            voteService.submitBatchVote(request);
            return ResponseEntity.ok("Голоса приняты");
        } catch (RuntimeException e) {
            // Возвращаем 500 с текстом ошибки (фронтенд это покажет)
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}