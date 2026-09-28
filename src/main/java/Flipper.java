public final class Flipper {
    private final Vec2 pivot;
    private final float length;
    private final float restAngle;
    private final float activeAngle;
    private final float angularSpeed;
    private final float radius;
    private float angle;
    private float angularVelocity;

    public Flipper(Vec2 pivot, float length, float restAngle, float activeAngle, float angularSpeed, float radius) {
        this.pivot = pivot;
        this.length = length;
        this.restAngle = restAngle;
        this.activeAngle = activeAngle;
        this.angularSpeed = angularSpeed;
        this.radius = radius;
        this.angle = restAngle;
    }

    public void update(float deltaSeconds, boolean active) {
        float target = active ? activeAngle : restAngle;
        float maximumStep = angularSpeed * deltaSeconds;
        float difference = target - angle;
        float step = Math.max(-maximumStep, Math.min(maximumStep, difference));
        angle += step;
        angularVelocity = deltaSeconds > 0.0f ? step / deltaSeconds : 0.0f;
    }

    public void collide(Ball ball) {
        Vec2 direction = new Vec2((float) Math.cos(angle), (float) Math.sin(angle));
        Vec2 pivotToBall = ball.position().subtract(pivot);
        float projection = Math.max(0.0f, Math.min(1.0f, pivotToBall.dot(direction) / length));
        Vec2 closest = pivot.add(direction.multiply(projection));
        Vec2 separation = ball.position().subtract(closest);
        float distance = separation.length();
        float minimumDistance = ball.radius() + radius;
        if (distance >= minimumDistance) {
            return;
        }

        Vec2 normal = distance > 0.0001f ? separation.multiply(1.0f / distance) : direction;
        ball.setPosition(closest.add(normal.multiply(minimumDistance)));
        Vec2 contactOffset = closest.subtract(pivot);
        Vec2 surfaceVelocity = new Vec2(-contactOffset.y(), contactOffset.x()).multiply(angularVelocity);
        Vec2 relativeVelocity = ball.velocity().subtract(surfaceVelocity);
        float normalSpeed = relativeVelocity.dot(normal);
        if (normalSpeed < 0.0f) {
            Vec2 reflected = relativeVelocity.subtract(normal.multiply(1.8f * normalSpeed));
            ball.setVelocity(reflected.add(surfaceVelocity));
        }
    }

    public Vec2 pivot() {
        return pivot;
    }

    public Vec2 endpoint() {
        return pivot.add(new Vec2((float) Math.cos(angle), (float) Math.sin(angle)).multiply(length));
    }
}