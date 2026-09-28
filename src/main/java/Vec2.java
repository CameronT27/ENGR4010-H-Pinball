public record Vec2(float x, float y) {
    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 multiply(float scalar) {
        return new Vec2(x * scalar, y * scalar);
    }

    public float dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    public float lengthSquared() {
        return dot(this);
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public Vec2 normalized() {
        float length = length();
        return length > 0.0001f ? multiply(1.0f / length) : new Vec2(0.0f, 0.0f);
    }
}