package org.system.history.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStats {
    private Long projectId;
    private Integer totalVotes;
    private Integer participants;
    private Map<String, Integer> optionStats;
}