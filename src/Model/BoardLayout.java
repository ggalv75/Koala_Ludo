package Model;
import java.util.List;
import java.util.Map;

public interface BoardLayout {
    Map<Position, Cell> fixedCells();
    Map<PlayerColor, List<Position>> routes();
    Map<PlayerColor, Position> castleCorners();
    Position centerCorner();
}