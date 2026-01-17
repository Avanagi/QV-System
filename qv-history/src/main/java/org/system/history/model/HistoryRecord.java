package org.system.history.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryRecord implements Serializable {
    private Long voteId;
    private Long projectId;
    private Integer voteCount;
    private BigDecimal cost;
    private String txHash;
    private String timestamp;
}