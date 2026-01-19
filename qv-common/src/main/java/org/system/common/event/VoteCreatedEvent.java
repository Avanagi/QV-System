package org.system.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteCreatedEvent implements Serializable {
    private Long voteId;
    private Long userId;
    private Long optionId;
    private BigDecimal cost;
    private Integer voteCount;
    private String pollTitle;
    private String optionText;
    private Long pollId;
}