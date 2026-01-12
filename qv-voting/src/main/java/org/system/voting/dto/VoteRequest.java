package org.system.voting.dto;

import lombok.Data;

@Data
public class VoteRequest {
    private Long userId;
    private Long projectId;
    private Integer voteCount;
}