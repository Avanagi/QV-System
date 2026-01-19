package org.system.voting.dto;

import lombok.Data;
import java.util.List;

@Data
public class PollCreationRequest {
    private String title;
    private String description;
    private Long creatorId;
    private List<String> options;
}