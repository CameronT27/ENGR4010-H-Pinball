public final class GameSimulation {
    private static final float TABLE_WIDTH = 1280.0f;
    private static final float TABLE_HEIGHT = 720.0f;
    private static final float GRAVITY = -520.0f;
    private static final float RESTITUTION = 0.82f;

    private final Ball ball = new Ball(new Vec2(640.0f, 560.0f), 14.0f);
    private final Flipper leftFlipper = new Flipper(new Vec2(485.0f, 115.0f), 125.0f,
            (float) Math.toRadians(18.0), (float) Math.toRadians(54.0), 8.0f, 12.0f);
    private final Flipper rightFlipper = new Flipper(new Vec2(795.0f, 115.0f), 125.0f,
            (float) Math.toRadians(162.0), (float) Math.toRadians(126.0), 8.0f, 12.0f);
    private final Vec2[] bumpers = {
            new Vec2(465.0f, 425.0f), new Vec2(640.0f, 485.0f), new Vec2(815.0f, 425.0f)
    };

    public void update(float deltaSeconds, InputState input) {
        float delta = Math.min(deltaSeconds, 0.033f);
        leftFlipper.update(delta, input.leftFlipper());
        rightFlipper.update(delta, input.rightFlipper());
        ball.integrate(delta, new Vec2(0.0f, GRAVITY));
        collideWithWalls();
        collideWithBumpers();
        leftFlipper.collide(ball);
        rightFlipper.collide(ball);
    }

    private void collideWithWalls() {
        Vec2 position = ball.position();
        Vec2 velocity = ball.velocity();
        float radius = ball.radius();
        if (position.x() < radius) {
            position = new Vec2(radius, position.y());
            velocity = new Vec2(Math.abs(velocity.x()) * RESTITUTION, velocity.y());
        } else if (position.x() > TABLE_WIDTH - radius) {
            position = new Vec2(TABLE_WIDTH - radius, position.y());
            velocity = new Vec2(-Math.abs(velocity.x()) * RESTITUTION, velocity.y());
        }
        if (position.y() > TABLE_HEIGHT - radius) {
            position = new Vec2(position.x(), TABLE_HEIGHT - radius);
            velocity = new Vec2(velocity.x(), -Math.abs(velocity.y()) * RESTITUTION);
        } else if (position.y() < radius) {
            position = new Vec2(position.x(), radius);
            velocity = new Vec2(velocity.x(), Math.abs(velocity.y()) * RESTITUTION);
        }
        ball.setPosition(position);
        ball.setVelocity(velocity);
    }

    private void collideWithBumpers() {
        for (Vec2 bumper : bumpers) {
            Vec2 separation = ball.position().subtract(bumper);
            float distance = separation.length();
            float minimumDistance = ball.radius() + 28.0f;
            if (distance >= minimumDistance) {
                continue;
            }
            Vec2 normal = distance > 0.0001f ? separation.multiply(1.0f / distance) : new Vec2(0.0f, 1.0f);
            ball.setPosition(bumper.add(normal.multiply(minimumDistance)));
            float incomingSpeed = ball.velocity().dot(normal);
            if (incomingSpeed < 0.0f) {
                ball.setVelocity(ball.velocity().subtract(normal.multiply(2.0f * incomingSpeed)).multiply(1.12f));
            }
        }
    }

    public Ball ball() {
        return ball;
    }

    public Flipper leftFlipper() {
        return leftFlipper;
    }

    public Flipper rightFlipper() {
        return rightFlipper;
    }

    public Vec2[] bumpers() {
        return bumpers.clone();
    }
}