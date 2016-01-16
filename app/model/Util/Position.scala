package model.Util

case class Position(var x: Double, var y: Double) {
    def multiply(mx: Double, my: Double): Position = {
        return new Position(x * mx, y * my);
    }

    def divide(dx: Double, dy: Double): Position = {
        return new Position(x / dx, y / dy);
    }

    def add(ax: Double, ay: Double): Position = {
        return new Position(x + ax, y + ay);
    }

    def subtract(sx: Double, sy: Double): Position = {
        return new Position(x - sx, y - sy);
    }

    def distanceSquared(position: Position): Double = {
        return math.pow(position.x - x, 2) + math.pow(position.y - y, 2);
    }

    def distance(position: Position): Double = {
        return math.sqrt(distanceSquared(position));
    }

    def distanceSquared(ox: Double, oy: Double): Double = {
        return math.pow(ox - x, 2) + math.pow(oy - y, 2);
    }

    def distance(ox: Double, oy: Double): Double = {
        return math.sqrt(distanceSquared(ox, oy));
    }
}