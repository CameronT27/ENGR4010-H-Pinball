public final class GameSimulation {
    private static final float TABLE_WIDTH = 720.0f;
    private static final float TABLE_HEIGHT = 1200.0f;
    private static final float WALL_LEFT = 28.0f;
    private static final float WALL_RIGHT = TABLE_WIDTH - 28.0f;
    private static final float WALL_BOTTOM = 28.0f;
    private static final float WALL_TOP = TABLE_HEIGHT - 28.0f;
    private static final float LAUNCHER_X = 660.0f;
    private static final float LAUNCHER_BOTTOM = 62.0f;
    private static final float MAX_LAUNCH_POWER = 2100.0f;
    private static final float LAUNCH_CHARGE_TIME = 1.5f;
    private static final float GRAVITY = -520.0f;
    private static final float RESTITUTION = 0.82f;
    private static final float DRAIN_LEFT = 170.0f;
    private static final float DRAIN_RIGHT = 550.0f;
    private static final float NUDGE_COOLDOWN = 0.35f;
    private static final float STATIONARY_SPEED = 8.0f;
    private static final float STATIONARY_TIME_LIMIT = 1.25f;
    private static final int STARTING_BALLS = 3;

        private final Ball ball = new Ball(new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM), 14.0f);
            private final Flipper leftFlipper = new Flipper(new Vec2(245.0f, 115.0f), 105.0f,
                (float) Math.toRadians(-20.0), (float) Math.toRadians(25.0), 8.0f, 12.0f);
            private final Flipper rightFlipper = new Flipper(new Vec2(475.0f, 115.0f), 105.0f,
                (float) Math.toRadians(200.0), (float) Math.toRadians(155.0), 8.0f, 12.0f);
    private final Vec2[] bumpers = {
            new Vec2(230.0f, 730.0f), new Vec2(360.0f, 825.0f), new Vec2(490.0f, 730.0f)
    };
    private boolean ballInLauncher = true;
    private boolean launchWasPressed;
    private boolean nudgeLeftWasPressed;
    private boolean nudgeRightWasPressed;
    private boolean launcherPathActive;
    private float launcherPower;
    private float launcherPathTime;
    private float launcherPathDuration;
    private float nudgeCooldown;
    private float stationaryTime;
    private int score;
    private int ballsRemaining = STARTING_BALLS;
    private boolean gameOver;

    public void update(float deltaSeconds, InputState input) {
        float delta = Math.min(deltaSeconds, 0.033f);
        leftFlipper.update(delta, input.leftFlipper());
        rightFlipper.update(delta, input.rightFlipper());
        updateNudge(delta, input);
        updateLauncher(delta, input.launchButton());
        if (ballInLauncher || gameOver) {
            return;
        }
        if (launcherPathActive) {
            updateLauncherPath(delta);
            return;
        }
        ball.integrate(delta, new Vec2(0.0f, GRAVITY));
        if (isDrained() || isOutOfBounds()) {
            drainBall();
            return;
        }
        if (ball.velocity().length() < STATIONARY_SPEED) {
            stationaryTime += delta;
            if (stationaryTime >= STATIONARY_TIME_LIMIT) {
                drainBall();
                return;
            }
        } else {
            stationaryTime = 0.0f;
        }
        collideWithWalls();
        collideWithBumpers();
        leftFlipper.collide(ball);
        rightFlipper.collide(ball);
    }

    public void reset() {
        ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM));
        ball.setVelocity(new Vec2(0.0f, 0.0f));
        leftFlipper.reset();
        rightFlipper.reset();
        ballInLauncher = true;
        launchWasPressed = false;
        nudgeLeftWasPressed = false;
        nudgeRightWasPressed = false;
        launcherPathActive = false;
        launcherPower = 0.0f;
        launcherPathTime = 0.0f;
        launcherPathDuration = 0.0f;
        nudgeCooldown = 0.0f;
        stationaryTime = 0.0f;
        score = 0;
        ballsRemaining = STARTING_BALLS;
        gameOver = false;
    }

    private void updateNudge(float deltaSeconds, InputState input) {
        nudgeCooldown = Math.max(0.0f, nudgeCooldown - deltaSeconds);
        boolean leftPressed = input.nudgeLeft();
        boolean rightPressed = input.nudgeRight();
        boolean newNudge = (leftPressed && !nudgeLeftWasPressed) || (rightPressed && !nudgeRightWasPressed);
        if (!ballInLauncher && !gameOver && nudgeCooldown == 0.0f && newNudge) {
            float direction = leftPressed ? -1.0f : 1.0f;
            ball.setVelocity(ball.velocity().add(new Vec2(direction * 180.0f, 90.0f)));
            nudgeCooldown = NUDGE_COOLDOWN;
        }
        nudgeLeftWasPressed = leftPressed;
        nudgeRightWasPressed = rightPressed;
    }

    private void updateLauncher(float deltaSeconds, boolean launchPressed) {
        boolean released = launchWasPressed && !launchPressed;
        launchWasPressed = launchPressed;
        if (!ballInLauncher) {
            return;
        }

        if (launchPressed) {
            launcherPower = Math.min(1.0f, launcherPower + deltaSeconds / LAUNCH_CHARGE_TIME);
            ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM));
            ball.setVelocity(new Vec2(0.0f, 0.0f));
        } else if (released && launcherPower > 0.0f && !gameOver) {
            ballInLauncher = false;
            launcherPathActive = true;
            launcherPathTime = 0.0f;
            launcherPathDuration = 1.4f - launcherPower * 0.55f;
            launcherPower = 0.0f;
        }
    }

    private void updateLauncherPath(float deltaSeconds) {
        launcherPathTime = Math.min(launcherPathDuration, launcherPathTime + deltaSeconds);
        float progress = launcherPathTime / launcherPathDuration;
        Vec2 start = new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM);
        Vec2 control = new Vec2(LAUNCHER_X, 760.0f);
        Vec2 end = new Vec2(520.0f, 1080.0f);
        float firstWeight = 1.0f - progress;
        Vec2 position = start.multiply(firstWeight * firstWeight)
                .add(control.multiply(2.0f * firstWeight * progress))
                .add(end.multiply(progress * progress));
        Vec2 tangent = control.subtract(start).multiply(2.0f * firstWeight)
                .add(end.subtract(control).multiply(2.0f * progress));
        ball.setPosition(position);
        ball.setVelocity(tangent.multiply(1.0f / launcherPathDuration));
        if (launcherPathTime >= launcherPathDuration) {
            launcherPathActive = false;
        }
    }

    private boolean isDrained() {
        Vec2 position = ball.position();
        return position.y() < WALL_BOTTOM - ball.radius()
                && position.x() > DRAIN_LEFT && position.x() < DRAIN_RIGHT;
    }

    private boolean isOutOfBounds() {
        Vec2 position = ball.position();
        float radius = ball.radius();
        return position.x() < WALL_LEFT - radius
                || position.x() > WALL_RIGHT + radius
                || position.y() < WALL_BOTTOM - radius
                || position.y() > WALL_TOP + radius;
    }

    private void drainBall() {
        ballsRemaining--;
        launcherPower = 0.0f;
        stationaryTime = 0.0f;
        launcherPathActive = false;
        if (ballsRemaining <= 0) {
            gameOver = true;
            ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM));
            ball.setVelocity(new Vec2(0.0f, 0.0f));
            return;
        }
        ballInLauncher = true;
        ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_BOTTOM));
        ball.setVelocity(new Vec2(0.0f, 0.0f));
    }

    private void collideWithWalls() {
        Vec2 position = ball.position();
        Vec2 velocity = ball.velocity();
        float radius = ball.radius();
        if (position.x() < WALL_LEFT + radius) {
            position = new Vec2(WALL_LEFT + radius, position.y());
            velocity = new Vec2(Math.abs(velocity.x()) * RESTITUTION, velocity.y());
        } else if (position.x() > WALL_RIGHT - radius) {
            position = new Vec2(WALL_RIGHT - radius, position.y());
            velocity = new Vec2(-Math.abs(velocity.x()) * RESTITUTION, velocity.y());
        }
        if (position.y() > WALL_TOP - radius) {
            position = new Vec2(position.x(), WALL_TOP - radius);
            velocity = new Vec2(velocity.x(), -Math.abs(velocity.y()) * RESTITUTION);
        } else if (position.y() < WALL_BOTTOM + radius) {
            position = new Vec2(position.x(), WALL_BOTTOM + radius);
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
                score += 10;
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

    public boolean ballInLauncher() {
        return ballInLauncher;
    }

    public float launcherPower() {
        return launcherPower;
    }

    public int score() {
        return score;
    }

    public int ballsRemaining() {
        return ballsRemaining;
    }

    public boolean gameOver() {
        return gameOver;
    }
}