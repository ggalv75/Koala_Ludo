package Model;

import java.util.*;

/**
 * Validates new-game settings before the game starts (FR-01, FR-03, FR-05).
 * Lives in the Model so the View only collects input and shows the message (SDP 5.3.4, Done #5).
 */
public final class SetupValidator {
    private SetupValidator() { }

    /** Returns a message describing the first problem, or empty when the settings are valid. */
    public static Optional<String> validate(Difficulty difficulty, List<String> names, List<PlayerColor> colors) {
        if (difficulty == null) return Optional.of("יש לבחור רמת קושי.");
        if (names == null || colors == null || names.size() != colors.size()) {
            return Optional.of("הגדרות השחקנים אינן שלמות.");
        }
        int count = names.size();
        if (count < GameRules.MIN_PLAYERS || count > GameRules.MAX_PLAYERS) {
            return Optional.of("מספר השחקנים חייב להיות בין " + GameRules.MIN_PLAYERS
                    + " ל-" + GameRules.MAX_PLAYERS + ".");
        }
        Set<String> seenNames = new HashSet<>();
        Set<PlayerColor> seenColors = EnumSet.noneOf(PlayerColor.class);
        for (int i = 0; i < count; i++) {
            String name = names.get(i) == null ? "" : names.get(i).trim();
            int player = i + 1;
            if (name.isEmpty()) {
                return Optional.of("יש להזין שם או כינוי לשחקן " + player + ".");
            }
            if (name.length() > GameRules.MAX_NAME_LENGTH) {
                return Optional.of("השם של שחקן " + player + " ארוך מדי (עד "
                        + GameRules.MAX_NAME_LENGTH + " תווים).");
            }
            if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
                return Optional.of("השם \"" + name + "\" כבר נבחר. לכל שחקן צריך להיות שם אחר.");
            }
            PlayerColor color = colors.get(i);
            if (color == null) {
                return Optional.of("יש לבחור צבע לשחקן " + player + ".");
            }
            if (!seenColors.add(color)) {
                return Optional.of("הצבע " + color.displayName() + " כבר נבחר. לכל שחקן צריך להיות צבע ייחודי.");
            }
        }
        return Optional.empty();
    }
}
