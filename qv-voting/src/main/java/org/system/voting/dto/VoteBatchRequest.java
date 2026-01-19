package org.system.voting.dto;

import lombok.Data;

import java.util.Map;

@Data
public class VoteBatchRequest {
    private Long userId;
    private Long pollId;
    private Map<Long, Integer> votes;
}