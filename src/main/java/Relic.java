/**
 * Relics are permanent run upgrades that modify gameplay.
 * Collected at level milestones, mystery holes, or boss defeats.
 */
public enum Relic {
    /** Ball has increased mass and hits harder, but moves slower. */
    HEAVY_BALL("Heavy Ball", "Ball mass +20%, bumper damage +15%, speed -10%"),

    /** Bumpers exert a gentle magnetic pull on the ball when nearby. */
    MAGNET_CORE("Magnet Core", "Bumpers pull ball within 60px radius"),

    /** Each flipper hit charges a capacitor; release for a speed burst. */
    KINETIC_CAPACITOR("Kinetic Capacitor", "Flipper hits charge meter; release for 1.5x speed burst"),

    /** Lucky shots count double. */
    LUCKY_COIN("Lucky Coin", "15% chance bumper hits count as 2"),

    /** Ball can phase through one obstacle per life. */
    PHASE_BALL("Phase Ball", "Pass through 1 rail/obstacle per ball (recharges on drain)"),

    /** Score multiplier increases each level, resets on drain. */
    MULTIPLIER_CORE("Multiplier Core", "Score multiplier +0.1x per level, resets on drain"),

    /** Power-ups spawn twice as often and are worth double. */
    POWERUP_ATTRACTOR("Power-Up Attractor", "Power-ups spawn at 5 hits (was 10) and worth 50 pts (was 25)");

    private final String displayName;
    private final String description;

    Relic(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** Returns all relics as an array for random selection. */
    public static Relic[] valuesArray() {
        return values();
    }
}