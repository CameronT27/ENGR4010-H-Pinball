/**
 * A static "capsule" obstacle: a line segment with a thickness. Used for the inlane guides,
 * the launcher lane divider, the top-corner chamfers and the slingshot edges.
 * A kick other than 1.0 marks the rail as a guide channel. Guide channels
 * preserve the bounce direction while applying a small speed gain.
 */
public record Rail(Vec2 a, Vec2 b, float radius, float kick) {
    private static final float GUIDE_CHANNEL_ROLL_ACCELERATION = 55.0f;

    public Rail(Vec2 a, Vec2 b, float radius) {
        this(a, b, radius, 1.0f);
    }

    public boolean isGuideChannel() {
        return kick != 1.0f;
    }

    public Vec2 closestPoint(Vec2 point) {
        Vec2 segment = b.subtract(a);
        float lengthSquared = segment.lengthSquared();
        float t = lengthSquared > 0.0f
                ? Math.max(0.0f, Math.min(1.0f, point.subtract(a).dot(segment) / lengthSquared))
                : 0.0f;
        return a.add(segment.multiply(t));
    }

    /** Pushes the ball out of the rail and bounces it. Returns true if the ball struck the rail. */
    public boolean collide(Ball ball, float restitution) {
        Vec2 closest = closestPoint(ball.position());
        Vec2 separation = ball.position().subtract(closest);
        float distance = separation.length();
        float minimumDistance = ball.radius() + radius;
        if (distance >= minimumDistance) {
            return false;
        }

        Vec2 normal;
        if (distance > 0.0001f) {
            normal = separation.multiply(1.0f / distance);
        } else {
            Vec2 along = b.subtract(a).normalized();
            normal = new Vec2(-along.y(), along.x());
        }
        ball.setPosition(closest.add(normal.multiply(minimumDistance)));

        float normalSpeed = ball.velocity().dot(normal);
        if (normalSpeed >= 0.0f) {
            return false;
        }
        Vec2 bounced = ball.velocity().subtract(normal.multiply((1.0f + restitution) * normalSpeed));
        if (isGuideChannel()) {
            Vec2 along = b.subtract(a).normalized();
            if (along.y() > 0.0f) {
                along = along.multiply(-1.0f);
            }
            ball.setVelocity(bounced.multiply(kick).add(along.multiply(GUIDE_CHANNEL_ROLL_ACCELERATION)));
        } else {
            ball.setVelocity(bounced);
        }
        return true;
    }
}
