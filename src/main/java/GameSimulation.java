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
    private static final float GRAVITY = -900.0f;
    private static final float RESTITUTION = 0.68f;
    private static final float GUIDE_CHANNEL_KICK = 0.90f;
    private static final float MAX_BALL_SPEED = 2000.0f;
    private static final float MAX_STEP = 1.0f / 240.0f;
    private static final float NUDGE_COOLDOWN = 0.35f;
    private static final float STATIONARY_SPEED = 8.0f;
    private static final float STATIONARY_TIME_LIMIT = 2.5f;
    private static final float STATIONARY_WARNING_TIME = 1.5f;
    private static final float BUMPER_RADIUS = 28.0f;
    private static final float MIN_BUMPER_SPACING = BUMPER_RADIUS * 2.0f + 4.0f;
    private static final float BUMPER_MIN_X = WALL_LEFT + BUMPER_RADIUS + 8.0f;
    private static final float BUMPER_MAX_X = LANE_DIVIDER_X - BUMPER_RADIUS - 8.0f;
    private static final float BUMPER_MIN_Y = 500.0f;
    private static final float BUMPER_MAX_Y = WALL_TOP - BUMPER_RADIUS - 24.0f;
    private static final float LAUNCH_EXIT_CLEARANCE = 96.0f;
    private static final float LOWER_MIDDLE_LEFT = 210.0f;
    private static final float LOWER_MIDDLE_RIGHT = 430.0f;
    private static final float LOWER_MIDDLE_TOP = 650.0f;
    private static final float SCORING_RAIL_RADIUS = 5.0f;
    private static final float SCORING_RAIL_CLEARANCE = 6.0f;
    private static final float POWER_UP_RADIUS = 18.0f;
    private static final int POWER_UP_BUMPER_HIT_REQUIREMENT = 10;
    private static final int POINT_POWER_UP_VALUE = 25;
    private static final int STARTING_BALLS = 3;
    private static final int STARTING_LEVEL = 1;
    private static final int BASE_LEVEL_TARGET = 350;

    private final Ball ball = new Ball(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y), 14.0f);
    private final Flipper leftFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X - FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(-28.0), (float) Math.toRadians(28.0), 10.0f, 12.0f);
    private final Flipper rightFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X + FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(208.0), (float) Math.toRadians(152.0), 10.0f, 12.0f);
    private Vec2[] bumpers;
    private int[] bumperValues;
    private float[] bumperGlow;
    private Vec2 pointPowerUp;
    private int bumperHitsSincePowerUp;
    private final List<Rail> rails = buildRails();
    private List<ScoringRail> obstacles;
    private boolean[] scoringRailContacts;
    private final List<Rail> launcherGate = buildLauncherGate();

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
    private int totalScore;
    private static int highScore;
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
        for (int index = 0; index < obstacles.size(); index++) {
            Rail rail = obstacles.get(index).rail();
            float contactDistance = ball.radius() + rail.radius();
            float distance = ball.position().subtract(rail.closestPoint(ball.position())).length();
            if (scoringRailContacts[index] && distance > contactDistance + SCORING_RAIL_CLEARANCE) {
                scoringRailContacts[index] = false;
            }
            if (rail.collide(ball, RESTITUTION) && !scoringRailContacts[index]) {
                ScoringRail obstacle = obstacles.get(index);
                addScore(obstacle.points());
                kickerEvent = true;
                scoringRailContacts[index] = true;
                registerBumperHit();
            }
        }
    }

    private void addScore(int points) {
        score += points;
        totalScore += points;
        highScore = Math.max(highScore, totalScore);
    }

    private void registerBumperHit() {
        if (pointPowerUp != null) {
            return;
        }
        bumperHitsSincePowerUp++;
        if (bumperHitsSincePowerUp >= POWER_UP_BUMPER_HIT_REQUIREMENT) {
            Vec2 spawnedPowerUp = randomPowerUpPosition(new Random());
            if (spawnedPowerUp != null) {
                pointPowerUp = spawnedPowerUp;
                bumperHitsSincePowerUp = 0;
            }
        }
    }

    private List<Rail> buildRails() {
        List<Rail> list = new ArrayList<>();
        Vec2 leftPivot = leftFlipper.pivot();
        Vec2 rightPivot = rightFlipper.pivot();

        // Rounded top corners turn the ball coming up the lane toward the playfield.
        // The lower inlane guides remain straight so they keep their sharp angle toward
        // the flippers.
        addRoundedTopCorner(list, false);
        addRoundedTopCorner(list, true);

        // Inlane guides: funnel every ball onto a flipper so the only exit is the center drain.
        list.add(new Rail(new Vec2(WALL_LEFT, 300.0f), leftPivot, RAIL_RADIUS + 1.0f));
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, 300.0f), rightPivot, RAIL_RADIUS + 1.0f));

        // Launcher lane: divider wall and floor.
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, LANE_FLOOR_Y), new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP), RAIL_RADIUS));
        list.add(new Rail(new Vec2(LANE_DIVIDER_X, LANE_FLOOR_Y), new Vec2(WALL_RIGHT, LANE_FLOOR_Y), RAIL_RADIUS));

        // Curved guide channels redirect the ball toward the flippers without adding score.
        Vec2[] outerChannel = {new Vec2(92.0f, 350.0f), new Vec2(98.0f, 315.0f),
                new Vec2(116.0f, 280.0f), new Vec2(145.0f, 250.0f),
                new Vec2(180.0f, 228.0f)};
        Vec2[] innerChannel = {new Vec2(158.0f, 375.0f), new Vec2(166.0f, 338.0f),
                new Vec2(184.0f, 304.0f), new Vec2(212.0f, 278.0f),
                new Vec2(246.0f, 260.0f)};
        for (int side = 0; side < 2; side++) {
            addGuideChannel(list, side == 0 ? outerChannel : mirrorPoints(outerChannel));
            addGuideChannel(list, side == 0 ? innerChannel : mirrorPoints(innerChannel));
        }
        return Collections.unmodifiableList(list);
    }

    private static void addRoundedTopCorner(List<Rail> rails, boolean rightSide) {
        float centerX = rightSide ? WALL_RIGHT - 120.0f : WALL_LEFT + 120.0f;
        float centerY = 900.0f;
        float radius = 120.0f;
        int segments = 8;
        float startAngle = rightSide ? 0.0f : (float) Math.PI;
        float endAngle = rightSide ? (float) (Math.PI / 2.0) : (float) (Math.PI / 2.0);
        Vec2 previous = new Vec2(
                centerX + (float) Math.cos(startAngle) * radius,
                centerY + (float) Math.sin(startAngle) * radius);
        for (int index = 1; index <= segments; index++) {
            float progress = index / (float) segments;
            float angle = startAngle + (endAngle - startAngle) * progress;
            Vec2 current = new Vec2(
                    centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius);
            rails.add(new Rail(previous, current, RAIL_RADIUS));
            previous = current;
        }
    }

    private static void addGuideChannel(List<Rail> rails, Vec2[] points) {
        for (int index = 0; index + 1 < points.length; index++) {
            rails.add(new Rail(points[index], points[index + 1], RAIL_RADIUS, GUIDE_CHANNEL_KICK));
        }
    }

    private static List<Rail> buildLauncherGate() {
        Vec2 start = new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP);
        Vec2 end = new Vec2(WALL_RIGHT, 900.0f);
        return Collections.singletonList(new Rail(start, end, RAIL_RADIUS));
    }

    private static Vec2[] mirrorPoints(Vec2[] points) {
        Vec2[] mirrored = new Vec2[points.length];
        for (int index = 0; index < points.length; index++) {
            mirrored[index] = new Vec2(mirrorX(points[index].x()), points[index].y());
        }
        return mirrored;
    }

    private void generateLevelLayout() {
        Random random = new Random();
        // Add a bumper only every three levels and keep later layouts readable.
        int bumperCount = Math.min(3 + (level - 1) / 3, 4);
        bumpers = new Vec2[bumperCount];
        bumperValues = new int[bumperCount];
        bumperGlow = new float[bumperCount];

        for (int index = 0; index < bumperCount; index++) {
            bumpers[index] = randomBumperPosition(random, index);
            bumperValues[index] = 15 + random.nextInt(4) * 5;
        }
        pointPowerUp = null;
        bumperHitsSincePowerUp = 0;

        List<ScoringRail> generated = new ArrayList<>();
        int obstacleCount = Math.min(level - 1, 4);
        for (int index = 0; index < obstacleCount; index++) {
            ScoringRail obstacle = randomScoringRail(random, generated);
            if (obstacle == null) {
                throw new IllegalStateException("Unable to place non-overlapping scoring rail " + index);
            }
            generated.add(obstacle);
        }
        obstacles = Collections.unmodifiableList(generated);
        scoringRailContacts = new boolean[generated.size()];
    }

    private ScoringRail randomScoringRail(Random random, List<ScoringRail> existing) {
        for (int attempt = 0; attempt < 100; attempt++) {
            float x = 170.0f + random.nextInt(300);
            float y = 410.0f + random.nextInt(270);
            float length = 55.0f + random.nextInt(35);
            Rail rail = createScoringRail(x, y, length, random.nextBoolean() ? 0.35f : -0.35f);
            if (isScoringRailAvailable(rail, existing)) {
                return new ScoringRail(rail, 20 + level * 5);
            }
        }
        for (float y = 430.0f; y <= 670.0f; y += 45.0f) {
            for (float x = 180.0f; x <= 460.0f; x += 55.0f) {
                for (float angle : new float[]{0.35f, -0.35f}) {
                    Rail rail = createScoringRail(x, y, 60.0f, angle);
                    if (isScoringRailAvailable(rail, existing)) {
                        return new ScoringRail(rail, 20 + level * 5);
                    }
                }
            }
        }
        return null;
    }

    private static Rail createScoringRail(float x, float y, float length, float angle) {
        Vec2 start = new Vec2(x - length * 0.5f, y - (float) Math.sin(angle) * length * 0.5f);
        Vec2 end = new Vec2(x + length * 0.5f, y + (float) Math.sin(angle) * length * 0.5f);
        return new Rail(start, end, SCORING_RAIL_RADIUS);
    }

    private boolean isScoringRailAvailable(Rail candidate, List<ScoringRail> existing) {
        float bumperClearance = BUMPER_RADIUS + candidate.radius() + SCORING_RAIL_CLEARANCE;
        for (Vec2 bumper : bumpers) {
            if (bumper.subtract(candidate.closestPoint(bumper)).lengthSquared()
                    < bumperClearance * bumperClearance) {
                return false;
            }
        }
        float railClearance = candidate.radius() + SCORING_RAIL_RADIUS + SCORING_RAIL_CLEARANCE;
        for (ScoringRail obstacle : existing) {
            Rail other = obstacle.rail();
            if (isPointNearRail(candidate.a(), other, railClearance)
                    || isPointNearRail(candidate.b(), other, railClearance)
                    || isPointNearRail(other.a(), candidate, railClearance)
                    || isPointNearRail(other.b(), candidate, railClearance)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPointNearRail(Vec2 point, Rail rail, float clearance) {
        return point.subtract(rail.closestPoint(point)).lengthSquared() < clearance * clearance;
    }

    private Vec2 randomBumperPosition(Random random, int bumperIndex) {
        for (int attempt = 0; attempt < 100; attempt++) {
            float x;
            if (random.nextBoolean()) {
                x = 100.0f + random.nextFloat() * 100.0f;
            } else {
                x = 425.0f + random.nextFloat() * 100.0f;
            }
            if (random.nextFloat() < 0.35f) {
                x = BUMPER_MIN_X + random.nextFloat() * (BUMPER_MAX_X - BUMPER_MIN_X);
            }
            float y = 590.0f + random.nextFloat() * (BUMPER_MAX_Y - 590.0f);
            Vec2 candidate = new Vec2(x, y);
            if (isBumperPositionAvailable(candidate, bumperIndex)) {
                return candidate;
            }
        }
        // A deterministic grid keeps generated levels valid even if random attempts cluster.
        for (float y = BUMPER_MIN_Y; y <= BUMPER_MAX_Y; y += MIN_BUMPER_SPACING) {
            for (float x = BUMPER_MIN_X; x <= BUMPER_MAX_X; x += MIN_BUMPER_SPACING) {
                Vec2 candidate = new Vec2(x, y);
                if (isBumperPositionAvailable(candidate, bumperIndex)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Unable to place non-overlapping bumper " + bumperIndex);
    }

    private boolean isBumperPositionAvailable(Vec2 candidate, int bumperCount) {
        if (candidate.x() < BUMPER_MIN_X || candidate.x() > BUMPER_MAX_X
                || candidate.y() < BUMPER_MIN_Y || candidate.y() > BUMPER_MAX_Y) {
            return false;
        }
        if (candidate.x() > LOWER_MIDDLE_LEFT && candidate.x() < LOWER_MIDDLE_RIGHT
                && candidate.y() < LOWER_MIDDLE_TOP) {
            return false;
        }
        Vec2 launchExit = new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP);
        if (candidate.subtract(launchExit).lengthSquared()
                < LAUNCH_EXIT_CLEARANCE * LAUNCH_EXIT_CLEARANCE) {
            return false;
        }
        for (int index = 0; index < bumperCount; index++) {
            if (candidate.subtract(bumpers[index]).lengthSquared()
                    < MIN_BUMPER_SPACING * MIN_BUMPER_SPACING) {
                return false;
            }
        }
        return true;
    }

    private Vec2 randomPowerUpPosition(Random random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            Vec2 candidate = new Vec2(100.0f + random.nextFloat() * 420.0f,
                    430.0f + random.nextFloat() * 420.0f);
            if (isPowerUpPositionAvailable(candidate)) {
                return candidate;
            }
        }
        float spacing = BUMPER_RADIUS + POWER_UP_RADIUS + 4.0f;
        for (float y = 430.0f; y <= 850.0f; y += spacing) {
            for (float x = 100.0f; x <= 520.0f; x += spacing) {
                Vec2 candidate = new Vec2(x, y);
                if (isPowerUpPositionAvailable(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isPowerUpPositionAvailable(Vec2 candidate) {
        float bumperSpacing = BUMPER_RADIUS + POWER_UP_RADIUS + 4.0f;
        for (Vec2 bumper : bumpers) {
            if (candidate.subtract(bumper).lengthSquared() < bumperSpacing * bumperSpacing) {
                return false;
            }
        }
        float channelSpacing = POWER_UP_RADIUS + RAIL_RADIUS + 6.0f;
        for (Rail rail : rails) {
            if (rail.isGuideChannel()
                    && candidate.subtract(rail.closestPoint(candidate)).lengthSquared()
                    < channelSpacing * channelSpacing) {
                return false;
            }
        }
        for (ScoringRail obstacle : obstacles) {
            float obstacleSpacing = POWER_UP_RADIUS + obstacle.rail().radius() + 6.0f;
            if (candidate.subtract(obstacle.rail().closestPoint(candidate)).lengthSquared()
                    < obstacleSpacing * obstacleSpacing) {
                return false;
            }
        }
        return true;
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
            for (Rail rail : launcherGate) {
                rail.collide(ball, RESTITUTION);
            }
        }
        collideWithBumpers();
        collideWithPointPowerUp();
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
        pointPowerUp = null;
        bumperHitsSincePowerUp = 0;
        score = 0;
        totalScore = 0;
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
            nextLevelScore = level * BASE_LEVEL_TARGET;
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
            rail.collide(ball, RESTITUTION);
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
                addScore(bumperValues[index]);
                bumperEvent = true;
                bumperGlow[index] = 1.0f;
                registerBumperHit();
            }
        }
    }

    private void collideWithPointPowerUp() {
        if (pointPowerUp == null) {
            return;
        }
        float minimumDistance = ball.radius() + POWER_UP_RADIUS;
        if (ball.position().subtract(pointPowerUp).lengthSquared() < minimumDistance * minimumDistance) {
            addScore(POINT_POWER_UP_VALUE);
            pointPowerUp = null;
            bumperHitsSincePowerUp = 0;
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

    public Vec2 pointPowerUp() {
        return pointPowerUp;
    }

    public int bumperHitsSincePowerUp() {
        return bumperHitsSincePowerUp;
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

    public boolean stationaryWarning() {
        return stationaryTime >= STATIONARY_WARNING_TIME && stationaryTime < STATIONARY_TIME_LIMIT;
    }

    public boolean launcherGateClosed() {
        return launcherGateClosed;
    }

    public List<Rail> launcherGate() {
        return launcherGate;
    }

    public int score() {
        return score;
    }

    public int totalScore() {
        return totalScore;
    }

    public int highScore() {
        return highScore;
    }

    public int level() {
        return level;
    }

    public int nextLevelScore() {
        return nextLevelScore;
    }

    /** Temporary development shortcut: qualify the current level without changing it immediately. */
    public void qualifyForNextLevel() {
        score = Math.max(score, nextLevelScore);
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