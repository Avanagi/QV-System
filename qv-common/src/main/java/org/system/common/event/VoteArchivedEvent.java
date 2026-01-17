package org.system.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteArchivedEvent implements Serializable {
    private Long voteId;
    private Long userId;
    private Long projectId;
    private Integer voteCount;
    private BigDecimal cost;
    private String txHash;
    private LocalDateTime timestamp;
}