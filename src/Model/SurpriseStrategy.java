package Model;

/**
 * Strategy contract for one surprise effect (FR-38 to FR-44).
 * Strategies calculate a movement or schedule a turn effect.
 * The engine commits movements, resolves landings and publishes events.
 */
public interface SurpriseStrategy {

    SurpriseResolution apply(GameState state, int pieceId);
}