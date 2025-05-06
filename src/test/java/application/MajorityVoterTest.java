package application;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MajorityVoterTest {

    @Test
    void testGetMajorityLines() {
        MajorityVoter voter = new MajorityVoter(3);
        List<String> lines = Arrays.asList(
                "1 Texte message1",
                "1 Texte message1",
                "1 Texte message1",
                "2 Texte message2",
                "2 Texte message2",
                "3 Texte message3"
        );

        List<String> majorityLines = voter.getMajorityLines(lines);

        assertEquals(1, majorityLines.size());
        assertTrue(majorityLines.contains("1 Texte message1"));
    }
}