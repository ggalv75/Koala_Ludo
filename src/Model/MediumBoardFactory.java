package Model;

/**
 * Concrete Creator for the Medium board layout.
 */
final class MediumBoardFactory extends BoardFactory {
    @Override
    protected BoardLayout createLayout() {
        return new MediumBoardLayout();
    }
}