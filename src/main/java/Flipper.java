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

    public void reset() {
        angle = restAngle;
        angularVelocity = 0.0f;
    }

    public void collide(Ball ball) {
        Vec2 direction = new Vec2((float) Math.cos(angle), (float) Math.sin(angle));
        Vec2 collisionPosition = ball.position();
        Vec2 closest = closestPoint(collisionPosition, direction);
        float distance = collisionPosition.subtract(closest).length();
        float minimumDistance = ball.radius() + radius + 2.0f;

        if (distance >= minimumDistance) {
            Vec2 travel = ball.position().subtract(ball.previousPosition());
            for (int sampleIndex = 1; sampleIndex <= 8; sampleIndex++) {
                float progress = sampleIndex / 8.0f;
                Vec2 sample = ball.previousPosition().add(travel.multiply(progress));
                Vec2 sampleClosest = closestPoint(sample, direction);
                float sampleDistance = sample.subtract(sampleClosest).length();
                if (sampleDistance < minimumDistance) {
                    collisionPosition = sample;
                    closest = sampleClosest;
                    distance = sampleDistance;
                    break;
                }
            }
        }

        if (distance >= minimumDistance) {
            return;
        }

        Vec2 separation = collisionPosition.subtract(closest);
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

    private Vec2 closestPoint(Vec2 point, Vec2 direction) {
        Vec2 pivotToPoint = point.subtract(pivot);
        float projection = Math.max(0.0f, Math.min(length, pivotToPoint.dot(direction)));
        return pivot.add(direction.multiply(projection));
    }

    public Vec2 pivot() {
        return pivot;
    }

    public Vec2 endpoint() {
        return pivot.add(new Vec2((float) Math.cos(angle), (float) Math.sin(angle)).multiply(length));
    }
}