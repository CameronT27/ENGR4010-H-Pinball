import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GameSimulation {
    // ---- Table layout (shared with Main so the drawing always matches the physics) ----
    public static final float TABLE_WIDTH = 720.0f;
    public static final float TABLE_HEIGHT = 1200.0f;
    public static final float WALL_LEFT = 28.0f;
    public static final float WALL_RIGHT = TABLE_WIDTH - 28.0f;
    /** Leaves a dedicated HUD band above the playfield. */
    public static final float WALL_TOP = 1020.0f;
    /** The wall between the playfield and the launcher lane. */
    public static final float LANE_DIVIDER_X = 628.0f;
    public static final float PLAYFIELD_CENTER_X = (WALL_LEFT + LANE_DIVIDER_X) / 2.0f;
    public static final float LAUNCHER_X = 660.0f;
    public static final float LAUNCHER_REST_Y = 80.0f;
    public static final float PLUNGER_TRAVEL = 30.0f;

    private static final float LANE_FLOOR_Y = 28.0f;
    public static final float LANE_DIVIDER_TOP = 820.0f;
    private static final float RAIL_RADIUS = 4.0f;
    private static final float FLIPPER_PIVOT_OFFSET = 125.0f;
    private static final float FLIPPER_PIVOT_Y = 115.0f;

    // ---- Tuning ----
    private static final float LAUNCH_MIN_SPEED = 1250.0f;
    private static final float LAUNCH_MAX_SPEED = 1700.0f;
    private static final float LAUNCH_CHARGE_TIME = 1.5f;
    private static final float GRAVITY = -820.0f;
    private static final float RESTITUTION = 0.68f;
    private static final float MAX_BALL_SPEED = 2000.0f;
    private static final float MAX_STEP = 1.0f / 240.0f;
    private static final float NUDGE_COOLDOWN = 0.35f;
    private static final float STATIONARY_SPEED = 8.0f;
    private static final float STATIONARY_TIME_LIMIT = 1.25f;
    private static final float BUMPER_RADIUS = 28.0f;
    private static final int STARTING_BALLS = 3;
    private static final int STARTING_LEVEL = 1;
    private static final int BASE_LEVEL_TARGET = 500;

    private final Ball ball = new Ball(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y), 14.0f);
    private final Flipper leftFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X - FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(-28.0), (float) Math.toRadians(28.0), 10.0f, 12.0f);
    private final Flipper rightFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X + FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(208.0), (float) Math.toRadians(152.0), 10.0f, 12.0f);
    private Vec2[] bumpers;
    private int[] bumperValues;
    private float[] bumperGlow;
    private final List<Rail> rails = buildRails();
    private List<ScoringRail> obstacles;
    private final Rail launcherGate = new Rail(new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP),
            new Vec2(LANE_DIVIDER_X, WALL_TOP), RAIL_RADIUS);

    private boolean ballInLauncher = true;
    private boolean launcherGateClosed;
    private boolean launchWasPressed;
    private boolean launchLocked;
    private boolean nudgeLeftWasPressed;
    private boolean nudgeRightWasPressed;
    private float launcherPower;
    private float nudgeCooldown;
    private float stationaryTime;
    private int score;
    private int level = STARTING_LEVEL;
    private int nextLevelScore = BASE_LEVEL_TARGET;
    private int ballsRemaining = STARTING_BALLS;
    private boolean gameOver;
    private boolean launchEvent;
    private boolean bumperEvent;
    private boolean kickerEvent;
    private boolean drainEvent;
    private boolean levelUpEvent;

    public GameSimulation() {
        generateLevelLayout();
    }

    private static float mirrorX(float x) {
        return 2.0f * PLAYFIELD_CENTER_X - x;
    }

    private void collideWithObstacles() {
        for (ScoringRail obstacle : obstacles) {
            if (obstacle.rail().collide(ball, RESTITUTION)) {
                score += obstacle.points();
                kickerEvent = true;
            }
        }
    }

    private List<Rail> buildRails() {
        List<Rail> list = new ArrayList<>();
        Vec2 leftPivot = leftFlipper.pivot();
        Vec2 rightPivot = rightFlipper.pivot();

        // Top corner chamfers: turn the ball coming up the lane toward the playfield.
        list.add(new Rail(new Vec2(WALL_LEFT, 900.0f), new Vec2(WALL_LEFT + 120.0f, WALL_TOP), RAIL_RADIUS));
        list.add(new Rail(new Vec2(WALL_RIGHT, 900.0f), new Vec2(WALL_RIGHT - 120.0f, WALL_TOP), RAIL_RADIUS));

        // Inlane guides: funnel every ball onto a flipper so the only exit is the center drain.
        list.add(new Rail(new Vec2(WALL_LEFT, 300.0f), leftPivot, RAIL_RADIUS + 1.0f));
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, 300.0f), rightPivot, RAIL_RADIUS + 1.0f));

        // Launcher lane: divider wall and floor.
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, LANE_FLOOR_Y), new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP), RAIL_RADIUS));
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, LANE_FLOOR_Y), new Vec2(WALL_RIGHT, LANE_FLOOR_Y), RAIL_RADIUS));

        // Slingshots (triangle islands). Edges are listed in triples so Main can fill each triangle.
        float[][] triangle = {{131.0f, 310.0f}, {251.0f, 355.0f}, {221.0f, 250.0f}};
        for (int side = 0; side < 2; side++) {
            Vec2[] points = new Vec2[3];
            for (int index = 0; index < 3; index++) {
                float x = side == 0 ? triangle[index][0] : mirrorX(triangle[index][0]);
                points[index] = new Vec2(x, triangle[index][1]);
            }
            for (int index = 0; index < 3; index++) {
                list.add(new Rail(points[index], points[(index + 1) % 3], RAIL_RADIUS, 1.05f));
            }
        }
        return Collections.unmodifiableList(list);
    }

    private void generateLevelLayout() {
        Random random = new Random();
        int bumperCount = Math.min(3 + level - 1, 6);
        bumpers = new Vec2[bumperCount];
        bumperValues = new int[bumperCount];
        bumperGlow = new float[bumperCount];

        for (int index = 0; index < bumperCount; index++) {
            bumpers[index] = randomBumperPosition(random, index);
            bumperValues[index] = 15 + random.nextInt(4) * 5;
        }

        List<ScoringRail> generated = new ArrayList<>();
        int obstacleCount = Math.min(level - 1, 4);
        for (int index = 0; index < obstacleCount; index++) {
            float x = 170.0f + random.nextInt(300);
            float y = 410.0f + random.nextInt(270);
            float length = 55.0f + random.nextInt(35);
            float angle = random.nextBoolean() ? 0.35f : -0.35f;
            Vec2 start = new Vec2(x - length * 0.5f, y - (float) Math.sin(angle) * length * 0.5f);
            Vec2 end = new Vec2(x + length * 0.5f, y + (float) Math.sin(angle) * length * 0.5f);
            generated.add(new ScoringRail(new Rail(start, end, 5.0f), 20 + level * 5));
        }
        obstacles = Collections.unmodifiableList(generated);
    }

    private Vec2 randomBumperPosition(Random random, int bumperIndex) {
        for (int attempt = 0; attempt < 100; attempt++) {
            float x = 120.0f + random.nextFloat() * 380.0f;
            float y = 510.0f + random.nextFloat() * 330.0f;
            Vec2 candidate = new Vec2(x, y);
            boolean tooClose = false;
            for (int index = 0; index < bumperIndex; index++) {
                if (candidate.subtract(bumpers[index]).lengthSquared() < 80.0f * 80.0f) {
                    tooClose = true;
                    break;
                }
            }
            if (!tooClose) {
                return candidate;
            }
        }
        return new Vec2(160.0f + bumperIndex * 100.0f, 560.0f + (bumperIndex % 2) * 180.0f);
    }

    public void update(float deltaSeconds, InputState input) {
        float delta = Math.min(Math.max(deltaSeconds, 0.0f), 0.033f);
        updateNudge(delta, input);
        updateLauncher(delta, input.launchButton());
        for (int index = 0; index < bumperGlow.length; index++) {
            bumperGlow[index] = Math.max(0.0f, bumperGlow[index] - delta * 4.0f);
        }

        // Substep so a fast ball can never skip through a rail, bumper or flipper.
        int steps = Math.max(1, (int) Math.ceil(delta / MAX_STEP));
        float step = delta / steps;
        for (int index = 0; index < steps; index++) {
            simulateStep(step, input);
        }
    }

    private void simulateStep(float step, InputState input) {
        leftFlipper.update(step, input.leftFlipper());
        rightFlipper.update(step, input.rightFlipper());
        if (ballInLauncher || gameOver) {
            return;
        }

        ball.integrate(step, new Vec2(0.0f, GRAVITY));
        limitSpeed();
        if (isDrained() || isOutOfBounds()) {
            drainBall();
            return;
        }
        if (isBackInLauncherLane()) {
            returnToLauncher();
            return;
        }
        if (!launcherGateClosed && ball.position().x() < LANE_DIVIDER_X
                && ball.position().y() > LANE_DIVIDER_TOP) {
            launcherGateClosed = true;
        }

        // A ball cradled on a held flipper is the player's choice, so only count truly stuck balls.
        boolean flipperHeld = input.leftFlipper() || input.rightFlipper();
        if (!flipperHeld && ball.velocity().length() < STATIONARY_SPEED) {
            stationaryTime += step;
            if (stationaryTime >= STATIONARY_TIME_LIMIT) {
                drainBall();
                return;
            }
        } else {
            stationaryTime = 0.0f;
        }

        collideWithWalls();
        collideWithRails();
        collideWithObstacles();
        if (launcherGateClosed) {
            launcherGate.collide(ball, RESTITUTION);
        }
        collideWithBumpers();
        leftFlipper.collide(ball);
        rightFlipper.collide(ball);
        limitSpeed();
    }

    private void limitSpeed() {
        float speed = ball.velocity().length();
        if (speed > MAX_BALL_SPEED) {
            ball.setVelocity(ball.velocity().multiply(MAX_BALL_SPEED / speed));
        }
    }

    public void reset() {
        ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y));
        ball.setVelocity(new Vec2(0.0f, 0.0f));
        leftFlipper.reset();
        rightFlipper.reset();
        ballInLauncher = true;
        launcherGateClosed = false;
        launchWasPressed = false;
        launchLocked = false;
        nudgeLeftWasPressed = false;
        nudgeRightWasPressed = false;
        launcherPower = 0.0f;
        nudgeCooldown = 0.0f;
        stationaryTime = 0.0f;
        score = 0;
        level = STARTING_LEVEL;
        nextLevelScore = BASE_LEVEL_TARGET;
        ballsRemaining = STARTING_BALLS;
        gameOver = false;
        levelUpEvent = false;
        generateLevelLayout();
    }

    private void updateNudge(float deltaSeconds, InputState input) {
        nudgeCooldown = Math.max(0.0f, nudgeCooldown - deltaSeconds);
        boolean leftPressed = input.nudgeLeft();
        boolean rightPressed = input.nudgeRight();
        boolean leftNew = leftPressed && !nudgeLeftWasPressed;
        boolean rightNew = rightPressed && !nudgeRightWasPressed;
        // Direction comes from the key that was just pressed, not whichever key happens to be held.
        float direction = (rightNew ? 1.0f : 0.0f) - (leftNew ? 1.0f : 0.0f);
        if (!ballInLauncher && !gameOver && nudgeCooldown == 0.0f && direction != 0.0f) {
            ball.setVelocity(ball.velocity().add(new Vec2(direction * 180.0f, 90.0f)));
            nudgeCooldown = NUDGE_COOLDOWN;
        }
        nudgeLeftWasPressed = leftPressed;
        nudgeRightWasPressed = rightPressed;
    }

    private void updateLauncher(float deltaSeconds, boolean launchPressed) {
        boolean released = launchWasPressed && !launchPressed;
        launchWasPressed = launchPressed;
        if (!launchPressed) {
            launchLocked = false;
        }
        if (!ballInLauncher || gameOver) {
            return;
        }

        if (launchPressed && !launchLocked) {
            launcherPower = Math.min(1.0f, launcherPower + deltaSeconds / LAUNCH_CHARGE_TIME);
            ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y - PLUNGER_TRAVEL * launcherPower));
            ball.setVelocity(new Vec2(0.0f, 0.0f));
        } else if (released && launcherPower > 0.0f) {
            // The plunger fires the ball straight up the lane; the top chamfer turns it into the playfield.
            float speed = LAUNCH_MIN_SPEED + (LAUNCH_MAX_SPEED - LAUNCH_MIN_SPEED) * launcherPower;
            ball.setVelocity(new Vec2(0.0f, speed));
            ballInLauncher = false;
            launchEvent = true;
            launcherPower = 0.0f;
            stationaryTime = 0.0f;
        }
    }

    private boolean isDrained() {
        return ball.position().y() < -ball.radius();
    }

    private boolean isOutOfBounds() {
        Vec2 position = ball.position();
        float radius = ball.radius();
        return position.x() < WALL_LEFT - radius
                || position.x() > WALL_RIGHT + radius
                || position.y() > WALL_TOP + radius;
    }

    /** A weak plunge falls back down the lane; put it back on the plunger instead of costing a ball. */
    private boolean isBackInLauncherLane() {
        Vec2 position = ball.position();
        return position.x() > LANE_DIVIDER_X
                && position.y() <= LAUNCHER_REST_Y - PLUNGER_TRAVEL + 4.0f
                && ball.velocity().length() < 120.0f;
    }

    private void drainBall() {
        drainEvent = true;
        if (score >= nextLevelScore) {
            level++;
            score = 0;
            nextLevelScore = BASE_LEVEL_TARGET;
            ballsRemaining = Math.min(STARTING_BALLS, ballsRemaining + 1);
            generateLevelLayout();
            levelUpEvent = true;
        } else {
            ballsRemaining--;
        }
        if (ballsRemaining <= 0) {
            gameOver = true;
        }
        returnToLauncher();
    }

    private void returnToLauncher() {
        ballInLauncher = true;
        launcherGateClosed = false;
        launcherPower = 0.0f;
        stationaryTime = 0.0f;
        launchLocked = launchWasPressed;
        ball.setPosition(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y));
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
        }
        // No floor: the bottom of the table is the drain.
        ball.setPosition(position);
        ball.setVelocity(velocity);
    }

    private void collideWithRails() {
        for (Rail rail : rails) {
            boolean struck = rail.collide(ball, rail.isKicker() ? 1.0f : RESTITUTION);
            if (struck && rail.isKicker()) {
                score += 5;
                kickerEvent = true;
            }
        }
    }

    private void collideWithBumpers() {
        for (int index = 0; index < bumpers.length; index++) {
            Vec2 bumper = bumpers[index];
            Vec2 separation = ball.position().subtract(bumper);
            float distance = separation.length();
            float minimumDistance = ball.radius() + BUMPER_RADIUS;
            if (distance >= minimumDistance) {
                continue;
            }
            Vec2 normal = distance > 0.0001f ? separation.multiply(1.0f / distance) : new Vec2(0.0f, 1.0f);
            ball.setPosition(bumper.add(normal.multiply(minimumDistance)));
            float incomingSpeed = ball.velocity().dot(normal);
            if (incomingSpeed < 0.0f) {
                ball.setVelocity(ball.velocity().subtract(normal.multiply(2.0f * incomingSpeed)).multiply(1.04f));
                score += bumperValues[index];
                bumperEvent = true;
                bumperGlow[index] = 1.0f;
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

    public int bumperValue(int index) {
        return bumperValues[index];
    }

    public float bumperGlow(int index) {
        return bumperGlow[index];
    }

    public List<Rail> rails() {
        return rails;
    }

    public List<ScoringRail> obstacles() {
        return obstacles;
    }

    public boolean ballInLauncher() {
        return ballInLauncher;
    }

    public float launcherPower() {
        return launcherPower;
    }

    public boolean launcherGateClosed() {
        return launcherGateClosed;
    }

    public int score() {
        return score;
    }

    public int level() {
        return level;
    }

    public int nextLevelScore() {
        return nextLevelScore;
    }

    public int ballsRemaining() {
        return ballsRemaining;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public boolean consumeLaunchEvent() {
        boolean occurred = launchEvent;
        launchEvent = false;
        return occurred;
    }

    public boolean consumeBumperEvent() {
        boolean occurred = bumperEvent;
        bumperEvent = false;
        return occurred;
    }

    public boolean consumeKickerEvent() {
        boolean occurred = kickerEvent;
        kickerEvent = false;
        return occurred;
    }

    public boolean consumeDrainEvent() {
        boolean occurred = drainEvent;
        drainEvent = false;
        return occurred;
    }

    public boolean consumeLevelUpEvent() {
        boolean occurred = levelUpEvent;
        levelUpEvent = false;
        return occurred;
    }
}