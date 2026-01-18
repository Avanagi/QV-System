package org.system.history.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.system.history.model.ProjectStats;
import org.system.history.service.HistoryService;

import java.util.List;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
@Slf4j
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<Object>> getHistory(@PathVariable Long userId) {
        log.info("API: Запрос истории для User {}", userId);
        return ResponseEntity.ok(historyService.getUserHistory(userId));
    }

    @GetMapping("/stats/{projectId}")
    public ResponseEntity<ProjectStats> getStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(historyService.getProjectStats(projectId));
    }
}