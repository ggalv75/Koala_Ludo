package Model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/** Loads the JSON question bank, validates it, and selects level-matched questions. */
public final class QuestionRepository {
    private static final String RESOURCE = "/questions.json";
    private final Map<Integer, Question> questions;
    private final Map<Integer, Integer> correctOptionIndexes;
    private final Map<QuestionLevel, List<Question>> byLevel;
    private final Random random;

    /** Keep validated questions in insertion order and group them by difficulty. */
    private QuestionRepository(Map<Integer, Question> questions, Map<Integer, Integer> answers, Random random) {
        this.questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
        this.correctOptionIndexes = Map.copyOf(answers);
        this.random = Objects.requireNonNull(random, "random");
        EnumMap<QuestionLevel, List<Question>> grouped = new EnumMap<>(QuestionLevel.class);
        for (QuestionLevel level : QuestionLevel.values()) grouped.put(level, new ArrayList<>());
        for (Question question : questions.values()) grouped.get(question.level()).add(question);
        grouped.replaceAll((level, list) -> List.copyOf(list));
        this.byLevel = Collections.unmodifiableMap(grouped);
    }

    /** Read the bundled questions.json from the classpath, including when running inside a JAR. */
    public static QuestionRepository fromResources(Random random) throws IOException {
        try (InputStream stream = QuestionRepository.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IOException("Missing classpath resource: " + RESOURCE);
            return read(stream.readAllBytes(), random);
        }
    }

    /** Load the bundled bank with a securely seeded random source. */
    public static QuestionRepository fromResources() throws IOException {
        return fromResources(new SecureRandom());
    }

    /** Load a JSON bank from disk for tests and development without a decryption key. */
    public static QuestionRepository fromFile(Path path, Random random) throws IOException {
        Objects.requireNonNull(path, "path");
        return read(Files.readAllBytes(path), random);
    }

    /** Parse and validate the complete question bank before exposing any questions. */
    private static QuestionRepository read(byte[] bytes, Random random) throws IOException {
        Objects.requireNonNull(random, "random");
        Object parsed = new JsonParser(new String(bytes, StandardCharsets.UTF_8)).parse();
        Map<?, ?> root = requireObject(parsed, "root");
        Object rawQuestions = root.get("questions");
        if (!(rawQuestions instanceof List<?> items)) {
            throw new IOException("JSON root must contain a questions array");
        }
        if (items.size() < 30) throw new IOException("FR-60 requires at least 30 questions; found " + items.size());

        Map<Integer, Question> loaded = new LinkedHashMap<>();
        Map<Integer, Integer> answers = new LinkedHashMap<>();
        for (Object raw : items) {
            Map<?, ?> q = requireObject(raw, "question entry");
            int id = requireInt(q, "id");
            if (loaded.containsKey(id)) throw new IOException("Duplicate question id: " + id);
            String text = requireString(q, "text");
            String topic = requireString(q, "topic");
            String levelValue = requireString(q, "level");
            final QuestionLevel level;
            try {
                level = QuestionLevel.valueOf(levelValue);
            } catch (IllegalArgumentException error) {
                throw new IOException("Invalid level for question " + id + ": " + levelValue, error);
            }
            Object rawOptions = q.get("options");
            if (!(rawOptions instanceof List<?> optionArray) || optionArray.size() != 4) {
                throw new IOException("Question " + id + " must contain exactly four options");
            }
            List<String> options = new ArrayList<>();
            for (Object option : optionArray) {
                if (!(option instanceof String answer) || answer.isBlank()) {
                    throw new IOException("Question " + id + " has an invalid answer option");
                }
                options.add(answer);
            }
            int correctIndex = requireInt(q, "correctOptionIndex");
            if (correctIndex < 0 || correctIndex >= 4) {
                throw new IOException("Invalid correctOptionIndex for question " + id);
            }
            try {
                loaded.put(id, new Question(id, topic, text, options, level));
            } catch (IllegalArgumentException | NullPointerException error) {
                throw new IOException("Invalid question " + id + ": " + error.getMessage(), error);
            }
            answers.put(id, correctIndex);
        }
        for (QuestionLevel level : QuestionLevel.values()) {
            if (loaded.values().stream().noneMatch(question -> question.level() == level)) {
                throw new IOException("No questions found for level " + level);
            }
        }
        return new QuestionRepository(loaded, answers, random);
    }

    /** Require a JSON object instead of accepting malformed question structures. */
    private static Map<?, ?> requireObject(Object object, String field) throws IOException {
        if (!(object instanceof Map<?, ?> map)) throw new IOException("Expected object for " + field);
        return map;
    }

    /** Require a nonblank JSON string for a question field. */
    private static String requireString(Map<?, ?> object, String field) throws IOException {
        Object value = object.get(field);
        if (!(value instanceof String text) || text.isBlank()) throw new IOException("Missing string: " + field);
        return text;
    }

    /** Require an integer JSON number for a question field. */
    private static int requireInt(Map<?, ?> object, String field) throws IOException {
        Object value = object.get(field);
        if (!(value instanceof Integer number)) throw new IOException("Missing integer: " + field);
        return number;
    }

    /** Return the public information for every question without answer indexes. */
    public List<Question> findAll() { return List.copyOf(questions.values()); }

    /** Look up one question without revealing its answer index. */
    public Optional<Question> findById(int id) { return Optional.ofNullable(questions.get(id)); }

    /** Select one uniformly random question from the requested difficulty (FR-34). */
    public Optional<Question> findRandom(QuestionLevel level) {
        List<Question> candidates = byLevel.get(Objects.requireNonNull(level, "level"));
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    /** Compare one selected answer with its index stored in the JSON question bank (FR-36). */
    public boolean isCorrect(int questionId, int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex > 3) throw new IllegalArgumentException("Answer index must be 0..3");
        Integer correctIndex = correctOptionIndexes.get(questionId);
        if (correctIndex == null) throw new IllegalArgumentException("Unknown question: " + questionId);
        return correctIndex == selectedIndex;
    }

    /** Return the number of validated questions. */
    public int size() { return questions.size(); }

    /** Return the number of questions at a requested difficulty. */
    public int count(QuestionLevel level) { return byLevel.get(Objects.requireNonNull(level, "level")).size(); }

    /** Parse the JSON subset required by this bank without any third-party dependencies. */
    private static final class JsonParser {
        private final String json;
        private int index;

        /** Start a parser for a single JSON document. */
        private JsonParser(String json) { this.json = Objects.requireNonNull(json, "json"); }

        /** Parse exactly one complete JSON value and reject trailing content. */
        private Object parse() throws IOException {
            Object result = readValue();
            skipSpace();
            if (index != json.length()) throw error("Unexpected trailing JSON");
            return result;
        }

        /** Dispatch to the appropriate JSON value parser. */
        private Object readValue() throws IOException {
            skipSpace();
            if (index >= json.length()) throw error("Unexpected end of JSON");
            char ch = json.charAt(index);
            if (ch == '{') return readObject();
            if (ch == '[') return readArray();
            if (ch == '"') return readString();
            if (ch == '-' || (ch >= '0' && ch <= '9')) return readInteger();
            if (json.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
            if (json.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
            if (json.startsWith("null", index)) { index += 4; return null; }
            throw error("Invalid JSON value");
        }

        /** Read a JSON object while rejecting repeated field names. */
        private Map<String, Object> readObject() throws IOException {
            expect('{');
            Map<String, Object> values = new LinkedHashMap<>();
            skipSpace();
            if (consume('}')) return values;
            do {
                skipSpace();
                String name = readString();
                expect(':');
                Object value = readValue();
                if (values.containsKey(name)) throw error("Duplicate JSON field: " + name);
                values.put(name, value);
                skipSpace();
                if (consume('}')) return values;
                expect(',');
            } while (true);
        }

        /** Read an ordered JSON array. */
        private List<Object> readArray() throws IOException {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipSpace();
            if (consume(']')) return values;
            do {
                values.add(readValue());
                skipSpace();
                if (consume(']')) return values;
                expect(',');
            } while (true);
        }

        /** Decode quoted JSON text, backslash escapes, and Unicode escape sequences. */
        private String readString() throws IOException {
            skipSpace();
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < json.length()) {
                char ch = json.charAt(index++);
                if (ch == '"') return result.toString();
                if (ch == '\\') {
                    if (index >= json.length()) throw error("Incomplete escape");
                    char esc = json.charAt(index++);
                    switch (esc) {
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        case '/' -> result.append('/');
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> {
                            if (index + 4 > json.length()) throw error("Incomplete Unicode escape");
                            try {
                                result.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
                            } catch (NumberFormatException error) {
                                throw error("Invalid Unicode escape");
                            }
                            index += 4;
                        }
                        default -> throw error("Invalid JSON escape");
                    }
                } else {
                    if (ch < 0x20) throw error("Unescaped control character");
                    result.append(ch);
                }
            }
            throw error("Unterminated JSON string");
        }

        /** Read an integer; question bank fields do not use decimals. */
        private Integer readInteger() throws IOException {
            int start = index;
            if (json.charAt(index) == '-') index++;
            if (index == json.length() || !Character.isDigit(json.charAt(index))) throw error("Invalid number");
            if (json.charAt(index) == '0') index++;
            else while (index < json.length() && json.charAt(index) >= '0' && json.charAt(index) <= '9') index++;
            if (index < json.length() && "0123456789.eE+-".indexOf(json.charAt(index)) >= 0) {
                throw error("Only integer JSON numbers are supported");
            }
            try {
                return Integer.parseInt(json.substring(start, index));
            } catch (NumberFormatException error) {
                throw error("Integer out of range");
            }
        }

        /** Require one punctuation character after whitespace. */
        private void expect(char expected) throws IOException {
            skipSpace();
            if (!consume(expected)) throw error("Expected '" + expected + "'");
        }

        /** Consume one punctuation character if it is next. */
        private boolean consume(char expected) {
            if (index < json.length() && json.charAt(index) == expected) { index++; return true; }
            return false;
        }

        /** Skip legal JSON whitespace characters. */
        private void skipSpace() {
            while (index < json.length() && " \t\r\n".indexOf(json.charAt(index)) >= 0) index++;
        }

        /** Build a useful parsing error with the approximate character offset. */
        private IOException error(String reason) { return new IOException(reason + " at character " + index); }
    }
}
