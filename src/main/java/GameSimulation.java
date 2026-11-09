import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private static final float MAX_BALL_SPEED = 2000.0f;
    private static final float MAX_STEP = 1.0f / 240.0f;
    private static final float NUDGE_COOLDOWN = 0.35f;
    private static final float STATIONARY_SPEED = 8.0f;
    private static final float STATIONARY_TIME_LIMIT = 2.5f;
    private static final float STATIONARY_WARNING_TIME = 1.5f;
    private static final float ORBIT_SPEED = 520.0f;
    private static final float ORBIT_HEIGHT = 105.0f;
    private static final int ORBIT_SCORE = 100;
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
    private static final int STARTING_BALLS = 3;
    private static final int STARTING_LEVEL = 1;
    private static final int BASE_LEVEL_TARGET = 350;
    private static final int TABLE_COUNT = 4;
    private static final int MAX_COINS = 99;
    private static final float BASE_COMBO_WINDOW = 2.5f;
    private static final int MAX_COMBO_UPGRADES = 4;
    private static final int MAX_WINDOW_UPGRADES = 3;
    private static final int MAX_BONUS_UPGRADES = 3;
    private static final int MAX_EXTRA_BALLS = 2;

    private final Ball ball = new Ball(new Vec2(LAUNCHER_X, LAUNCHER_REST_Y), 14.0f);
    private final Flipper leftFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X - FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(-28.0), (float) Math.toRadians(28.0), 10.0f, 12.0f);
    private final Flipper rightFlipper = new Flipper(new Vec2(PLAYFIELD_CENTER_X + FLIPPER_PIVOT_OFFSET, FLIPPER_PIVOT_Y),
            100.0f, (float) Math.toRadians(208.0), (float) Math.toRadians(152.0), 10.0f, 12.0f);
    private Vec2[] bumpers;
    private int[] bumperValues;
    private float[] bumperGlow;
    private List<Rail> rails;
    private List<Vec2> orbitPath = buildOrbitPath(STARTING_LEVEL);
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
    private boolean orbitActive;
    private float orbitDistance;
    private float orbitTransferSpeed;
    private boolean orbitScored;
    private int coins;
    private boolean shopOpen;
    private int shopSelection;
    private int shopRefreshCost = 1;
    private int[] shopOptions = {0, 1, 2};
    private int comboUpgradeLevel;
    private int comboWindowUpgradeLevel;
    private int bumperBonusLevel;
    private int extraBallLevel;
    private int comboMultiplier = 1;
    private int comboHits;
    private float comboTimer;

    public GameSimulation() {
        generateLevelLayout();
    }

    private static int[] freshShopOptions(int offset) {
        int start = Math.floorMod(offset, 4);
        return new int[]{start, (start + 1) % 4, (start + 2) % 4};
    }

    public int coins() {
        return coins;
    }

    public boolean shopOpen() {
        return shopOpen;
    }

    public int shopSelection() {
        return shopSelection;
    }

    public int[] shopOptions() {
        return shopOptions.clone();
    }

    public int shopRefreshCost() {
        return shopRefreshCost;
    }

    public int comboMultiplier() {
        return comboMultiplier;
    }

    public int comboHits() {
        return comboHits;
    }

    public int comboCapValue() {
        return comboCap();
    }

    public float comboWindowValue() {
        return comboWindow();
    }

    public int bumperBonusPercent() {
        return bumperBonusLevel * 15;
    }

    public int extraBallLevel() {
        return extraBallLevel;
    }

    public int lifeCount() {
        return STARTING_BALLS + extraBallLevel;
    }

    public void moveShopSelection(int direction) {
        if (!shopOpen) {
            return;
        }
        shopSelection = (shopSelection + direction + shopOptions.length) % shopOptions.length;
    }

    public boolean buySelectedPerk() {
        if (!shopOpen) {
            return false;
        }
        int perk = shopOptions[shopSelection];
        if (perk < 0) {
            return false;
        }
        int cost = perkCost(perk);
        if (cost < 0 || coins < cost) {
            return false;
        }
        coins -= cost;
        switch (perk) {
            case 0 -> comboUpgradeLevel++;
            case 1 -> comboWindowUpgradeLevel++;
            case 2 -> bumperBonusLevel++;
            case 3 -> {
                extraBallLevel++;
                ballsRemaining++;
            }
            default -> throw new IllegalStateException("Unknown perk: " + perk);
        }
        shopOptions[shopSelection] = -1;
        return true;
    }

    public boolean refreshShop() {
        if (!shopOpen || shopRefreshCost > coins) {
            return false;
        }
        coins -= shopRefreshCost;
        int next = shopRefreshCost > MAX_COINS / 2 ? MAX_COINS + 1 : shopRefreshCost * 2;
        shopRefreshCost = next;
        shopOptions = freshShopOptions(shopOptions[0] < 0 ? 0 : shopOptions[0] + 1);
        shopSelection = 0;
        return true;
    }

    public int perkCost(int perk) {
        return switch (perk) {
            case 0 -> comboUpgradeLevel >= MAX_COMBO_UPGRADES ? -1 : 3 + comboUpgradeLevel * 2;
            case 1 -> comboWindowUpgradeLevel >= MAX_WINDOW_UPGRADES ? -1 : 2 + comboWindowUpgradeLevel * 2;
            case 2 -> bumperBonusLevel >= MAX_BONUS_UPGRADES ? -1 : 3 + bumperBonusLevel * 2;
            case 3 -> extraBallLevel >= MAX_EXTRA_BALLS ? -1 : 6 + extraBallLevel * 3;
            default -> throw new IllegalArgumentException("Unknown perk: " + perk);
        };
    }

    public int perkLevel(int perk) {
        return switch (perk) {
            case 0 -> comboUpgradeLevel;
            case 1 -> comboWindowUpgradeLevel;
            case 2 -> bumperBonusLevel;
            case 3 -> extraBallLevel;
            default -> throw new IllegalArgumentException("Unknown perk: " + perk);
        };
    }

    public void leaveShop() {
        if (!shopOpen) {
            return;
        }
        shopOpen = false;
        generateLevelLayout();
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
            }
        }
    }

    private void addScore(int points) {
        score += points;
        totalScore += points;
        highScore = Math.max(highScore, totalScore);
    }

    private void registerBumperHit() {
        comboHits++;
        comboTimer = comboWindow();
        comboMultiplier = Math.min(comboMultiplier + 1, comboCap());
    }

    private int comboCap() {
        return Math.min(5, 1 + comboUpgradeLevel);
    }

    private float comboWindow() {
        return BASE_COMBO_WINDOW + comboWindowUpgradeLevel;
    }

    private int bumperScore(int points) {
        int bonus = points * (100 + bumperBonusLevel * 15) / 100;
        return bonus * comboMultiplier;
    }

    private List<Rail> buildRails(int table) {
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

        addTableRails(list, table);
        return Collections.unmodifiableList(list);
    }

    private static void addTableRails(List<Rail> rails, int table) {
        switch (table) {
            case 1 -> {
                rails.add(new Rail(new Vec2(135.0f, 840.0f), new Vec2(265.0f, 750.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(395.0f, 750.0f), new Vec2(525.0f, 840.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(210.0f, 925.0f), new Vec2(430.0f, 925.0f), RAIL_RADIUS, true));
            }
            case 2 -> {
                rails.add(new Rail(new Vec2(130.0f, 900.0f), new Vec2(265.0f, 690.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(350.0f, 900.0f), new Vec2(505.0f, 755.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(430.0f, 575.0f), new Vec2(555.0f, 690.0f), RAIL_RADIUS, true));
            }
            case 3 -> {
                rails.add(new Rail(new Vec2(145.0f, 790.0f), new Vec2(300.0f, 695.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(365.0f, 565.0f), new Vec2(540.0f, 665.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(265.0f, 935.0f), new Vec2(485.0f, 855.0f), RAIL_RADIUS, true));
            }
            case 4 -> {
                rails.add(new Rail(new Vec2(150.0f, 850.0f), new Vec2(260.0f, 740.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(440.0f, 740.0f), new Vec2(560.0f, 850.0f), RAIL_RADIUS, true));
                rails.add(new Rail(new Vec2(285.0f, 600.0f), new Vec2(435.0f, 600.0f), RAIL_RADIUS, true));
            }
            default -> throw new IllegalArgumentException("Unknown table: " + table);
        }
    }

    private static List<Vec2> buildOrbitPath(int table) {
        List<Vec2> path = new ArrayList<>();
        switch (table) {
            case 1 -> {
                path.add(new Vec2(145.0f, 340.0f));
                path.add(new Vec2(120.0f, 480.0f));
                path.add(new Vec2(145.0f, 650.0f));
                path.add(new Vec2(220.0f, 820.0f));
                path.add(new Vec2(330.0f, 915.0f));
                path.add(new Vec2(430.0f, 950.0f));
            }
            case 2 -> {
                path.add(new Vec2(360.0f, 340.0f));
                path.add(new Vec2(305.0f, 485.0f));
                path.add(new Vec2(335.0f, 640.0f));
                path.add(new Vec2(430.0f, 780.0f));
                path.add(new Vec2(500.0f, 895.0f));
                path.add(new Vec2(430.0f, 960.0f));
            }
            case 3 -> {
                path.add(new Vec2(548.0f, 340.0f));
                path.add(new Vec2(574.0f, 470.0f));
                path.add(new Vec2(580.0f, 680.0f));
                path.add(new Vec2(570.0f, 830.0f));
                path.add(new Vec2(520.0f, 930.0f));
                path.add(new Vec2(430.0f, 970.0f));
            }
            case 4 -> {
                path.add(new Vec2(145.0f, 340.0f));
                path.add(new Vec2(120.0f, 500.0f));
                path.add(new Vec2(145.0f, 680.0f));
                path.add(new Vec2(230.0f, 820.0f));
                path.add(new Vec2(360.0f, 900.0f));
                path.add(new Vec2(480.0f, 900.0f));
                path.add(new Vec2(555.0f, 820.0f));
            }
            default -> throw new IllegalArgumentException("Unknown table: " + table);
        }
        return Collections.unmodifiableList(path);
    }

    private float orbitLength() {
        float length = 0.0f;
        for (int index = 1; index < orbitPath.size(); index++) {
            length += orbitPath.get(index).subtract(orbitPath.get(index - 1)).length();
        }
        return length;
    }

    private Vec2 orbitPosition(float distance) {
        float remaining = distance;
        for (int index = 1; index < orbitPath.size(); index++) {
            Vec2 start = orbitPath.get(index - 1);
            Vec2 end = orbitPath.get(index);
            float segmentLength = end.subtract(start).length();
            if (remaining <= segmentLength) {
                return start.add(end.subtract(start).multiply(
                        segmentLength == 0.0f ? 0.0f : remaining / segmentLength));
            }
            remaining -= segmentLength;
        }
        return orbitPath.get(orbitPath.size() - 1);
    }

    private Vec2 orbitTangent(float distance) {
        float remaining = Math.min(Math.max(distance, 0.0f), orbitLength());
        for (int index = 1; index < orbitPath.size(); index++) {
            Vec2 start = orbitPath.get(index - 1);
            Vec2 end = orbitPath.get(index);
            Vec2 segment = end.subtract(start);
            float segmentLength = segment.length();
            if (remaining <= segmentLength) {
                return segmentLength == 0.0f ? new Vec2(0.0f, 0.0f) : segment.multiply(1.0f / segmentLength);
            }
            remaining -= segmentLength;
        }
        Vec2 finalSegment = orbitPath.get(orbitPath.size() - 1)
                .subtract(orbitPath.get(orbitPath.size() - 2));
        return finalSegment.normalized();
    }

    private void updateOrbit(float step) {
        orbitDistance += ORBIT_SPEED * step;
        float length = orbitLength();
        float progress = Math.min(1.0f, orbitDistance / length);
        ball.setPosition(orbitPosition(orbitDistance));
        ball.setVelocity(orbitTangent(orbitDistance).multiply(orbitTransferSpeed));
        if (!orbitScored && progress >= 0.58f) {
            addScore(ORBIT_SCORE);
            kickerEvent = true;
            orbitScored = true;
        }
        if (progress >= 1.0f) {
            orbitActive = false;
            orbitDistance = 0.0f;
            orbitScored = false;
            stationaryTime = 0.0f;
        }
    }

    private boolean tryEnterOrbit() {
        Vec2 position = ball.position();
        Vec2 entrance = orbitPath.get(0);
        if (position.x() >= entrance.x() - 30.0f && position.x() <= entrance.x() + 30.0f
                && position.y() >= entrance.y() - 45.0f && position.y() <= entrance.y() + 45.0f
                && ball.velocity().y() > 180.0f) {
            Vec2 entranceTangent = orbitTangent(0.0f);
            orbitTransferSpeed = Math.max(180.0f, Math.abs(ball.velocity().dot(entranceTangent)));
            orbitActive = true;
            orbitDistance = 0.0f;
            orbitScored = false;
            ball.setPosition(orbitPath.get(0));
            ball.setVelocity(new Vec2(0.0f, 0.0f));
            return true;
        }
        return false;
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

    private static List<Rail> buildLauncherGate() {
        Vec2 start = new Vec2(LANE_DIVIDER_X, LANE_DIVIDER_TOP);
        Vec2 end = new Vec2(WALL_RIGHT, 900.0f);
        return Collections.singletonList(new Rail(start, end, RAIL_RADIUS));
    }

    private void generateLevelLayout() {
        int table = table();
        rails = buildRails(table);
        orbitPath = buildOrbitPath(table);
        bumpers = switch (table) {
            case 1 -> new Vec2[]{new Vec2(170.0f, 650.0f), new Vec2(350.0f, 650.0f), new Vec2(500.0f, 650.0f)};
            case 2 -> new Vec2[]{new Vec2(170.0f, 620.0f), new Vec2(330.0f, 700.0f), new Vec2(520.0f, 560.0f)};
            case 3 -> new Vec2[]{new Vec2(160.0f, 560.0f), new Vec2(360.0f, 760.0f), new Vec2(540.0f, 560.0f)};
            case 4 -> new Vec2[]{new Vec2(165.0f, 540.0f), new Vec2(360.0f, 820.0f), new Vec2(520.0f, 620.0f)};
            default -> throw new IllegalStateException("Unknown table: " + table);
        };
        bumperValues = new int[]{20, 25, 30};
        bumperGlow = new float[bumpers.length];

        List<ScoringRail> generated = new ArrayList<>();
        switch (table) {
            case 1 -> {
                generated.add(new ScoringRail(createScoringRail(220.0f, 560.0f, 90.0f, 0.35f), 25));
                generated.add(new ScoringRail(createScoringRail(410.0f, 560.0f, 90.0f, -0.35f), 25));
            }
            case 2 -> {
                generated.add(new ScoringRail(createScoringRail(240.0f, 480.0f, 80.0f, -0.35f), 30));
                generated.add(new ScoringRail(createScoringRail(440.0f, 480.0f, 80.0f, 0.35f), 30));
            }
            case 3 -> {
                generated.add(new ScoringRail(createScoringRail(285.0f, 500.0f, 100.0f, 0.0f), 35));
                generated.add(new ScoringRail(createScoringRail(425.0f, 500.0f, 100.0f, 0.0f), 35));
            }
            case 4 -> {
                generated.add(new ScoringRail(createScoringRail(250.0f, 500.0f, 90.0f, 0.35f), 40));
                generated.add(new ScoringRail(createScoringRail(450.0f, 520.0f, 90.0f, -0.35f), 40));
            }
            default -> throw new IllegalStateException("Unknown table: " + table);
        }
        validateLayout(generated, table);
        obstacles = Collections.unmodifiableList(generated);
        scoringRailContacts = new boolean[generated.size()];
    }

    private void validateLayout(List<ScoringRail> scoringRails, int table) {
        for (int index = 0; index < bumpers.length; index++) {
            if (!isBumperPositionAvailable(bumpers[index], index)) {
                throw new IllegalStateException("Invalid bumper placement on table " + table);
            }
            for (Rail rail : rails) {
                float clearance = BUMPER_RADIUS + rail.radius() + SCORING_RAIL_CLEARANCE;
                if (isPointNearRail(bumpers[index], rail, clearance)) {
                    throw new IllegalStateException("Bumper " + index + " overlaps table rail on table " + table);
                }
            }
        }
        for (int index = 0; index < scoringRails.size(); index++) {
            if (!isScoringRailAvailable(scoringRails.get(index).rail(),
                    scoringRails.subList(0, index))) {
                throw new IllegalStateException("Overlapping scoring rail on table " + table);
            }
            for (Rail rail : rails) {
                if (isPointNearRail(scoringRails.get(index).rail().a(), rail, SCORING_RAIL_CLEARANCE)
                        || isPointNearRail(scoringRails.get(index).rail().b(), rail, SCORING_RAIL_CLEARANCE)) {
                    throw new IllegalStateException("Scoring rail overlaps table rail on table " + table);
                }
            }
        }
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

    public void update(float deltaSeconds, InputState input) {
        if (shopOpen) {
            return;
        }
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

        if (orbitActive) {
            updateOrbit(step);
            return;
        }

        ball.integrate(step, new Vec2(0.0f, GRAVITY));
        if (comboTimer > 0.0f) {
            comboTimer = Math.max(0.0f, comboTimer - step);
            if (comboTimer == 0.0f) {
                comboMultiplier = 1;
                comboHits = 0;
            }
        }
        limitSpeed();
        if (isDrained() || isOutOfBounds()) {
            drainBall();
            return;
        }
        if (isBackInLauncherLane()) {
            returnToLauncher();
            return;
        }
        if (tryEnterOrbit()) {
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
        totalScore = 0;
        level = STARTING_LEVEL;
        nextLevelScore = BASE_LEVEL_TARGET;
        ballsRemaining = STARTING_BALLS;
        gameOver = false;
        levelUpEvent = false;
        orbitActive = false;
        orbitDistance = 0.0f;
        orbitScored = false;
        coins = 0;
        shopOpen = false;
        shopSelection = 0;
        shopRefreshCost = 1;
        shopOptions = freshShopOptions(level);
        comboUpgradeLevel = 0;
        comboWindowUpgradeLevel = 0;
        bumperBonusLevel = 0;
        extraBallLevel = 0;
        comboMultiplier = 1;
        comboHits = 0;
        comboTimer = 0.0f;
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
        if (score >= nextLevelScore && !shopOpen) {
            coins = Math.min(MAX_COINS, coins + score / 100);
            level++;
            score = 0;
            nextLevelScore = level * BASE_LEVEL_TARGET;
            ballsRemaining = Math.min(lifeCount(), ballsRemaining + 1);
            comboMultiplier = 1;
            comboHits = 0;
            comboTimer = 0.0f;
            shopOpen = true;
            shopSelection = 0;
            shopRefreshCost = 1;
            shopOptions = freshShopOptions(level);
            returnToLauncher();
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
                addScore(bumperScore(bumperValues[index]));
                registerBumperHit();
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

    public List<Vec2> orbitPath() {
        return orbitPath;
    }

    public boolean orbitActive() {
        return orbitActive;
    }

    public float orbitHeight() {
        if (!orbitActive) {
            return 0.0f;
        }
        float progress = Math.min(1.0f, orbitDistance / orbitLength());
        return (float) Math.sin(Math.PI * progress) * ORBIT_HEIGHT;
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

    public int table() {
        return ((level - STARTING_LEVEL) % TABLE_COUNT) + STARTING_LEVEL;
    }

    public int nextLevelScore() {
        return nextLevelScore;
    }

    /** Temporary development shortcut: qualify the current level without changing it immediately. */
    public void qualifyForNextLevel() {
        score = Math.max(score, nextLevelScore) + 100000;
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