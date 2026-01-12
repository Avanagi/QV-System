package org.system.voting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.system.voting.dto.VoteRequest;
import org.system.voting.entity.Vote;
import org.system.voting.service.VoteService;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<Vote> castVote(@RequestBody VoteRequest request) {
        Vote vote = voteService.createVote(request);
        return ResponseEntity.ok(vote);
    }
}