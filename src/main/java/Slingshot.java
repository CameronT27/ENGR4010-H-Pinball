/**
 * Triangular kicker (slingshot) that kicks the ball sideways when entered.
 * Classic pinball element placed near flippers.
 */
public record Slingshot(Vec2 a, Vec2 b, Vec2 c, float kickPower, int points) {

    /**
     * Checks if ball enters the slingshot triangle and kicks it.
     * @param ball The ball to check
     * @param restitution Bounce restitution (unused here, kick overrides)
     * @param simTime Current simulation time (for cooldown)
     * @return true if ball was kicked
     */
    public boolean collide(Ball ball, float restitution, float simTime) {
        if (!pointInTriangle(ball.position(), a, b, c)) {
            return false;
        }

        // Kick ball away from triangle center
        Vec2 center = new Vec2((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f);
        Vec2 dir = ball.position().subtract(center);
        float dirLenSq = dir.lengthSquared();
        if (dirLenSq < 0.0001f) {
            dir = new Vec2(1, 0);
        } else {
            dir = dir.multiply(1f / (float) Math.sqrt(dirLenSq));
        }

        // Apply kick: cancel inward velocity, add outward kick
        float incoming = ball.velocity().dot(dir);
        Vec2 newVel = ball.velocity()
                .subtract(dir.multiply((1f + restitution) * Math.min(0f, incoming)))
                .add(dir.multiply(kickPower));
        ball.setVelocity(newVel);
        return true;
    }

    /** Barycentric point-in-triangle test. */
    private static boolean pointInTriangle(Vec2 p, Vec2 a, Vec2 b, Vec2 c) {
        float v0x = c.x() - a.x();
        float v0y = c.y() - a.y();
        float v1x = b.x() - a.x();
        float v1y = b.y() - a.y();
        float v2x = p.x() - a.x();
        float v2y = p.y() - a.y();

        float dot00 = v0x * v0x + v0y * v0y;
        float dot01 = v0x * v1x + v0y * v1y;
        float dot02 = v0x * v2x + v0y * v2y;
        float dot11 = v1x * v1x + v1y * v1y;
        float dot12 = v1x * v2x + v1y * v2y;

        float invDenom = 1f / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return u >= 0f && v >= 0f && (u + v) < 1f;
    }
}