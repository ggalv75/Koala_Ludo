package Model;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Loads, validates and selects questions without exposing correct answers to the UI. */
public final class QuestionRepository {
    private static final String RESOURCE = "/questions.properties";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String CIPHER_NAME = "AES/GCM/NoPadding";

    private final Map<Integer, Question> questions;
    private final Map<Integer, Integer> correctOptionIndexes;
    private final Map<QuestionLevel, List<Question>> byLevel;
    private final Random random;

    /** Keep an immutable repository with an injectable random source for repeatable tests. */
    private QuestionRepository(Map<Integer, Question> questions, Map<Integer, Integer> answers,
                               Random random) {
        this.questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
        this.correctOptionIndexes = Map.copyOf(answers);
        this.random = Objects.requireNonNull(random, "random");
        EnumMap<QuestionLevel, List<Question>> grouped = new EnumMap<>(QuestionLevel.class);
        for (QuestionLevel level : QuestionLevel.values()) grouped.put(level, new ArrayList<>());
        for (Question question : questions.values()) grouped.get(question.level()).add(question);
        grouped.replaceAll((level, list) -> List.copyOf(list));
        this.byLevel = Collections.unmodifiableMap(grouped);
    }

    /** Load the question bank from a bundled classpath resource (also works inside a JAR). */
    public static QuestionRepository fromResources(SecretKey key, Random random) throws IOException {
        try (InputStream in = QuestionRepository.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IOException("Missing classpath resource: " + RESOURCE);
            return read(in, key, random);
        }
    }

    /** Convenience overload using a secure random source. */
    public static QuestionRepository fromResources(SecretKey key) throws IOException {
        return fromResources(key, new SecureRandom());
    }

    /** Load the question bank from a file during development or automated tests. */
    public static QuestionRepository fromFile(Path file, SecretKey key, Random random) throws IOException {
        Objects.requireNonNull(file, "file");
        try (InputStream in = Files.newInputStream(file)) {
            return read(in, key, random);
        }
    }

    /** Read a locally stored AES key; never put this key in source control or inside a public JAR. */
    public static SecretKey readKey(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        try {
            byte[] raw = Base64.getDecoder().decode(Files.readString(file, StandardCharsets.UTF_8).strip());
            if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
                throw new IOException("Invalid AES key length");
            }
            return new SecretKeySpec(raw, "AES");
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid Base64 key file", error);
        }
    }

    /** Parse 30 or more validated questions and decrypt their correct option indexes. */
    private static QuestionRepository read(InputStream stream, SecretKey key, Random random) throws IOException {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(random, "random");
        Properties properties = new Properties();
        properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        int size;
        try {
            size = Integer.parseInt(required(properties, "questions.count"));
        } catch (NumberFormatException error) {
            throw new IOException("Invalid questions.count", error);
        }
        if (size < 1) throw new IOException("Question bank is empty");

        Map<Integer, Question> loaded = new LinkedHashMap<>();
        Map<Integer, Integer> answers = new HashMap<>();
        for (int id = 1; id <= size; id++) {
            String prefix = "question." + id + ".";
            try {
                QuestionLevel level = QuestionLevel.valueOf(required(properties, prefix + "level"));
                String topic = required(properties, prefix + "topic");
                String text = required(properties, prefix + "text");
                List<String> options = List.of(
                        required(properties, prefix + "option.0"),
                        required(properties, prefix + "option.1"),
                        required(properties, prefix + "option.2"),
                        required(properties, prefix + "option.3"));
                Question question = new Question(id, topic, text, options, level);
                int correctIndex = decryptAnswer(required(properties, prefix + "answer"), id, key);
                if (correctIndex < 0 || correctIndex > 3) {
                    throw new IOException("Invalid correct answer index for question " + id);
                }
                loaded.put(id, question);
                answers.put(id, correctIndex);
            } catch (IllegalArgumentException error) {
                throw new IOException("Invalid question " + id + ": " + error.getMessage(), error);
            }
        }
        return new QuestionRepository(loaded, answers, random);
    }

    /** Require every field to exist and contain a nonblank value. */
    private static String required(Properties properties, String name) throws IOException {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) throw new IOException("Missing property: " + name);
        return value;
    }

    /** AES-GCM decrypt an answer index bound to its question id. */
    private static int decryptAnswer(String encrypted, int questionId, SecretKey key) throws IOException {
        try {
            byte[] input = Base64.getDecoder().decode(encrypted);
            if (input.length < IV_BYTES + TAG_BITS / 8 + 1) throw new IOException("Short answer payload");
            byte[] nonce = java.util.Arrays.copyOfRange(input, 0, IV_BYTES);
            byte[] cipherText = java.util.Arrays.copyOfRange(input, IV_BYTES, input.length);
            Cipher cipher = Cipher.getInstance(CIPHER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(("KoalaLudo:question:" + questionId).getBytes(StandardCharsets.UTF_8));
            String answer = new String(cipher.doFinal(cipherText), StandardCharsets.US_ASCII);
            return Integer.parseInt(answer);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new IOException("Cannot decrypt answer for question " + questionId, error);
        }
    }

    /** Return all public question data in original id order. */
    public List<Question> findAll() {
        return List.copyOf(questions.values());
    }

    /** Return an existing question without revealing its correct answer. */
    public Optional<Question> findById(int id) {
        return Optional.ofNullable(questions.get(id));
    }

    /** Pick a uniformly random question from the required question level (FR-34). */
    public Optional<Question> findRandom(QuestionLevel level) {
        List<Question> candidates = byLevel.get(Objects.requireNonNull(level, "level"));
        if (candidates.isEmpty()) return Optional.empty();
        return Optional.of(candidates.get(random.nextInt(candidates.size())));
    }

    /** Check the selected 0-based answer index without publishing the correct one (FR-36). */
    public boolean isCorrect(int questionId, int selectedIndex) {
        if (selectedIndex < 0 || selectedIndex > 3) throw new IllegalArgumentException("Answer index must be 0..3");
        Integer correctIndex = correctOptionIndexes.get(questionId);
        if (correctIndex == null) throw new IllegalArgumentException("Unknown question: " + questionId);
        return correctIndex == selectedIndex;
    }

    /** Return the total number of questions loaded. */
    public int size() {
        return questions.size();
    }

    /** Return the number of questions at a specific level. */
    public int count(QuestionLevel level) {
        return byLevel.get(Objects.requireNonNull(level, "level")).size();
    }
}
