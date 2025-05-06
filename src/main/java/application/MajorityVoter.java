package application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MajorityVoter {
    private static final Logger logger = LoggerFactory.getLogger(MajorityVoter.class);
    private final int majorityThreshold;

    public MajorityVoter(int totalReplicas) {
        this.majorityThreshold = totalReplicas / 2 + 1;
    }

    public List<String> getMajorityLines(List<String> allLines) {
        Map<String, Integer> lineCount = new HashMap<>();
        for (String line : allLines) {
            lineCount.merge(line, 1, Integer::sum);
        }

        List<String> majorityLines = new ArrayList<>();
        lineCount.forEach((line, count) -> {
            if (count >= majorityThreshold) {
                majorityLines.add(line);
            }
        });

        logger.info("Found {} majority lines with threshold {}", majorityLines.size(), majorityThreshold);
        return majorityLines;
    }
}