package Model;

/**
 * Concrete Creator for the Easy board layout.
 */
final class EasyBoardFactory extends BoardFactory {
    @Override
    protected BoardLayout createLayout() {
        return new EasyBoardLayout();
    }
}