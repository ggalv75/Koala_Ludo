package Model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Reject malformed JSON question banks before a Medium game can start. */
class QuestionRepositoryIntegrityTest {

    /** Test a JSON bank without replacing the real project resource. */
    private static void assertInvalid(String content) throws IOException {
        Path temp = Files.createTempFile("koala-invalid-questions", ".json");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            assertThrows(IOException.class, () -> QuestionRepository.fromFile(temp, new Random(1)));
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /** FR-60: reject a bank with fewer than thirty questions. */
    @Test void rejectsTooSmallQuestionBank() throws IOException {
        String json = Files.readString(Path.of("src", "questions.json"), StandardCharsets.UTF_8);
        int last = json.lastIndexOf(",\n    {");
        // Drop the final question by replacing its object with whitespace.
        int lastEnd = json.lastIndexOf("    }");
        assertTrue(last > 0 && lastEnd > last);
        assertInvalid(json.substring(0, last) + json.substring(lastEnd + 5));
    }

    /** FR-34: reject a bank that has no questions at HARD difficulty. */
    @Test void rejectsMissingDifficultyLevel() throws IOException {
        String json = Files.readString(Path.of("src", "questions.json"), StandardCharsets.UTF_8);
        assertInvalid(json.replace("\"level\": \"HARD\"", "\"level\": \"EASY\""));
    }

    /** Malformed JSON must fail before creating a repository. */
    @Test void rejectsInvalidJsonSyntax() throws IOException {
        assertInvalid("{invalid json}");
    }

    /** A duplicate question ID must not silently replace an existing question. */
    @Test void rejectsDuplicateIdentifiers() throws IOException {
        String json = Files.readString(Path.of("src", "questions.json"), StandardCharsets.UTF_8);
        assertInvalid(json.replaceFirst("\"id\": 2", "\"id\": 1"));
    }

    /** Every answer index must select one of the four available options. */
    @Test void rejectsInvalidCorrectOptionIndex() throws IOException {
        String json = Files.readString(Path.of("src", "questions.json"), StandardCharsets.UTF_8);
        assertInvalid(json.replaceFirst("\"correctOptionIndex\": [0-3]", "\"correctOptionIndex\": 4"));
    }
}
