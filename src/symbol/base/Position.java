package symbol.base;

public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Position) {
            Position another = (Position) obj;
            return ((this.x == another.getX()) && (this.y == another.getY()));
        } else {
            System.err.println("Type error");
            return false;
        }
    }
}
