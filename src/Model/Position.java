package Model;

/** A physical square on the board grid. Two routes that pass the same square share the same Position. */
public record Position(int row, int col) { }
