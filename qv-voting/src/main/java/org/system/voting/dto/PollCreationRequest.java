package org.system.voting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.system.voting.entity.VoteType;
import java.util.List;

@Data
public class PollCreationRequest {
    private String title;
    private String description;
    private Long creatorId;
    private List<String> options;
    private VoteType voteType;

    @JsonProperty("isPublic")
    private boolean isPublic;

}