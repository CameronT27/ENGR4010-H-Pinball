public final class Ball {
    private Vec2 position;
    private Vec2 previousPosition;
    private Vec2 velocity;
    private final float radius;

    public Ball(Vec2 position, float radius) {
        this.position = position;
        this.previousPosition = position;
        this.velocity = new Vec2(0.0f, 0.0f);
        this.radius = radius;
    }

    public void integrate(float deltaSeconds, Vec2 acceleration) {
        previousPosition = position;
        velocity = velocity.add(acceleration.multiply(deltaSeconds));
        velocity = velocity.multiply((float) Math.pow(0.999f, deltaSeconds * 60.0f));
        position = position.add(velocity.multiply(deltaSeconds));
    }

    public Vec2 position() {
        return position;
    }

    public Vec2 velocity() {
        return velocity;
    }

    public Vec2 previousPosition() {
        return previousPosition;
    }

    public float radius() {
        return radius;
    }

    public void setPosition(Vec2 position) {
        this.position = position;
    }

    public void setVelocity(Vec2 velocity) {
        this.velocity = velocity;
    }
}