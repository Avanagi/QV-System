package org.system.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundsReservedEvent implements Serializable {
    private Long voteId;
    private Long userId;
}