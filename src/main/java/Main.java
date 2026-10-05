import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_M;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_N;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_QUAD_STRIP;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private static final int WINDOW_WIDTH = 720;
    private static final int WINDOW_HEIGHT = 1200;
    private static final float LEVEL_TRANSITION_SECONDS = 1.5f;

    // Seven-segment bit masks (a, b, c, d, e, f, g) for the digits 0-9.
    private static final int[] DIGIT_SEGMENTS = {
            0b0111111, 0b0000110, 0b1011011, 0b1001111, 0b1100110,
            0b1101101, 0b1111101, 0b0000111, 0b1111111, 0b1101111
    };

    private Main() {
    }

    public static void main(String[] args) {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        long window = createWindow();
        try {
            glfwMakeContextCurrent(window);
            glfwSwapInterval(1);
            glfwShowWindow(window);
            org.lwjgl.opengl.GL.createCapabilities();
            glClearColor(0.02f, 0.02f, 0.035f, 1.0f);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_MULTISAMPLE);

            GameSimulation simulation = new GameSimulation();
            InputState input = new InputState();
            try (AudioFeedback audio = new AudioFeedback()) {
            double previousTime = glfwGetTime();
            boolean resetWasPressed = false;
            boolean enterWasPressed = false;
            boolean menuWasPressed = false;
            boolean devLevelWasPressed = false;
            boolean showingIntro = true;
            boolean showingLevelTransition = false;
            float levelTransitionTime = 0.0f;

            while (!glfwWindowShouldClose(window)) {
                double currentTime = glfwGetTime();
                float deltaSeconds = (float) (currentTime - previousTime);
                previousTime = currentTime;
                if (isPressed(window, GLFW_KEY_ESCAPE)) {
                    glfwSetWindowShouldClose(window, true);
                }
                boolean resetPressed = isPressed(window, GLFW_KEY_R);
                if (resetPressed && !resetWasPressed) {
                    simulation.reset();
                }
                resetWasPressed = resetPressed;
                boolean menuPressed = isPressed(window, GLFW_KEY_M);
                if (!showingIntro && simulation.gameOver() && menuPressed && !menuWasPressed) {
                    simulation.reset();
                    showingIntro = true;
                }
                menuWasPressed = menuPressed;
                boolean devLevelPressed = isPressed(window, GLFW_KEY_N);
                if (!showingIntro && devLevelPressed && !devLevelWasPressed) {
                    simulation.qualifyForNextLevel();
                }
                devLevelWasPressed = devLevelPressed;
                boolean enterPressed = isPressed(window, GLFW_KEY_ENTER);
                if (showingIntro && enterPressed && !enterWasPressed) {
                    showingIntro = false;
                    showingLevelTransition = true;
                    levelTransitionTime = LEVEL_TRANSITION_SECONDS;
                }
                enterWasPressed = enterPressed;
                input.setLeftFlipper(isPressed(window, GLFW_KEY_A) || isPressed(window, GLFW_KEY_LEFT));
                input.setRightFlipper(isPressed(window, GLFW_KEY_D) || isPressed(window, GLFW_KEY_RIGHT));
                input.setLaunchButton(isPressed(window, GLFW_KEY_SPACE));
                input.setNudgeLeft(isPressed(window, GLFW_KEY_Z));
                input.setNudgeRight(isPressed(window, GLFW_KEY_X));

                if (!showingIntro && !showingLevelTransition) {
                    simulation.update(deltaSeconds, input);
                    audio.update(simulation, input);
                    if (simulation.consumeLevelUpEvent()) {
                        showingLevelTransition = true;
                        levelTransitionTime = LEVEL_TRANSITION_SECONDS;
                    }
                    input.advanceFrame();
                    glfwSetWindowTitle(window, "2.5D Pinball | Score: " + simulation.score()
                            + " | Balls: " + simulation.ballsRemaining()
                        + (simulation.gameOver() ? " | GAME OVER - press R to reset" : ""));
                } else if (showingLevelTransition) {
                    levelTransitionTime -= Math.max(deltaSeconds, 0.0f);
                    if (levelTransitionTime <= 0.0f) {
                        showingLevelTransition = false;
                    }
                    glfwSetWindowTitle(window, "2.5D Pinball | Level " + simulation.level());
                } else {
                    glfwSetWindowTitle(window, "2.5D Pinball | Press Enter to start");
                }

                glClear(GL_COLOR_BUFFER_BIT);
                applyViewport(window);
                if (showingIntro) {
                    drawIntroScreen();
                } else if (showingLevelTransition) {
                    drawLevelTransition(simulation.level());
                } else {
                    drawTable(simulation);
                }
                glfwSwapBuffers(window);
                glfwPollEvents();
            }
            }
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
            errorCallback.free();
        }
    }

    private static long createWindow() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        // Shrink the window if the table would be taller than the screen.
        var videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        float scale = 1.0f;
        if (videoMode != null) {
            scale = Math.min(1.0f, videoMode.height() * 0.9f / WINDOW_HEIGHT);
        }
        int width = Math.round(WINDOW_WIDTH * scale);
        int height = Math.round(WINDOW_HEIGHT * scale);

        long window = glfwCreateWindow(width, height, "2.5D Pinball", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Unable to create GLFW window");
        }
        if (videoMode != null) {
            glfwSetWindowPos(window, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2);
        }
        return window;
    }

    private static boolean isPressed(long window, int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    /** Keeps the table's aspect ratio when the window is resized (letterboxed). */
    private static void applyViewport(long window) {
        int[] framebufferWidth = new int[1];
        int[] framebufferHeight = new int[1];
        glfwGetFramebufferSize(window, framebufferWidth, framebufferHeight);
        float scale = Math.min(framebufferWidth[0] / (float) WINDOW_WIDTH,
                framebufferHeight[0] / (float) WINDOW_HEIGHT);
        int viewWidth = Math.round(WINDOW_WIDTH * scale);
        int viewHeight = Math.round(WINDOW_HEIGHT * scale);
        glViewport((framebufferWidth[0] - viewWidth) / 2, (framebufferHeight[0] - viewHeight) / 2,
                viewWidth, viewHeight);
    }

    // ------------------------------------------------------------------ table

    private static void drawTable(GameSimulation simulation) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        float left = GameSimulation.WALL_LEFT;
        float right = GameSimulation.WALL_RIGHT;
        float top = GameSimulation.WALL_TOP;
        float divider = GameSimulation.LANE_DIVIDER_X;
        float centerX = GameSimulation.PLAYFIELD_CENTER_X;

        // Cabinet frame uses the same dark teal and red palette as the main screen.
        verticalGradient(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 0.01f, 0.06f, 0.08f, 0.04f, 0.24f, 0.28f);
        color(0.35f, 0.09f, 0.05f, 1.0f);
        rect(left - 5.0f, 0, left, top + 5.0f);
        rect(right, 0, right + 5.0f, top + 5.0f);
        rect(left - 5.0f, top, right + 5.0f, top + 5.0f);

        // Playfield and launcher lane.
        verticalGradient(left, 0, right, top, 0.02f, 0.09f, 0.13f, 0.05f, 0.27f, 0.33f);
        verticalGradient(divider, 0, right, top, 0.01f, 0.05f, 0.08f, 0.03f, 0.13f, 0.17f);

        drawPlayfieldArt(centerX);
        drawLaneArt();
        drawRails(simulation);
        drawProceduralObstacles(simulation);
        drawBumpers(simulation);
        drawFlippers(simulation);
        drawBall(simulation);
        drawPlunger(simulation);
        drawHud(simulation, centerX);
        drawPowerMeter(simulation);

        if (simulation.gameOver()) {
            color(0.0f, 0.0f, 0.0f, 0.62f);
            rect(left, 0, right, top);
            drawCenteredText("GAME OVER", centerX, 690.0f, 5.0f, 1.0f, 0.33f, 0.45f, 1.0f);
            drawNumber(simulation.score(), 6, centerX, 600.0f, 58.0f, 100.0f, 11.0f, 1.0f, 0.78f, 0.25f, 1.0f, false);
            drawCenteredText("PRESS R TO RESET", centerX, 470.0f, 3.0f, 0.85f, 0.90f, 0.92f, 1.0f);
            color(0.04f, 0.22f, 0.27f, 1.0f);
            rect(160.0f, 330.0f, 560.0f, 410.0f);
            drawCenteredText("M RETURN TO MENU", centerX, 370.0f, 3.0f, 0.95f, 0.75f, 0.20f, 1.0f);
        }
    }

    private static void drawIntroScreen() {
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();

            verticalGradient(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT,
                    0.01f, 0.06f, 0.08f, 0.04f, 0.24f, 0.28f);
            color(0.35f, 0.09f, 0.05f, 1.0f);
            rect(26.0f, 26.0f, WINDOW_WIDTH - 26.0f, WINDOW_HEIGHT - 26.0f);
            color(0.01f, 0.05f, 0.07f, 1.0f);
            rect(40.0f, 40.0f, WINDOW_WIDTH - 40.0f, WINDOW_HEIGHT - 40.0f);

            drawCenteredText("PINBALL", WINDOW_WIDTH / 2.0f, 980.0f, 10.0f, 1.0f, 0.33f, 0.45f, 1.0f);
            drawCenteredText("WHATEVER YOU DO,", WINDOW_WIDTH / 2.0f, 835.0f, 3.0f, 0.85f, 0.85f, 0.90f, 1.0f);
            drawCenteredText("WORK AT IT WITH ALL", WINDOW_WIDTH / 2.0f, 790.0f, 3.0f, 0.85f, 0.85f, 0.90f, 1.0f);
            drawCenteredText("YOUR HEART,", WINDOW_WIDTH / 2.0f, 745.0f, 3.0f, 0.85f, 0.85f, 0.90f, 1.0f);
            drawCenteredText("AS WORKING FOR THE LORD,", WINDOW_WIDTH / 2.0f, 700.0f, 2.5f, 0.85f, 0.85f, 0.90f, 1.0f);
            drawCenteredText("NOT FOR HUMAN MASTERS.", WINDOW_WIDTH / 2.0f, 660.0f, 2.5f, 0.85f, 0.85f, 0.90f, 1.0f);
            drawCenteredText("COLOSSIANS 3:23", WINDOW_WIDTH / 2.0f, 610.0f, 3.0f, 0.30f, 0.95f, 0.90f, 1.0f);

            color(0.04f, 0.22f, 0.27f, 1.0f);
            rect(105.0f, 350.0f, WINDOW_WIDTH - 105.0f, 555.0f);
            drawCenteredText("CONTROLS", WINDOW_WIDTH / 2.0f, 500.0f, 4.0f, 1.0f, 0.33f, 0.45f, 1.0f);
            drawCenteredText("A OR LEFT ARROW  LEFT FLIPPER", WINDOW_WIDTH / 2.0f, 445.0f, 2.0f, 0.85f, 0.90f, 0.92f, 1.0f);
            drawCenteredText("D OR RIGHT ARROW  RIGHT FLIPPER", WINDOW_WIDTH / 2.0f, 405.0f, 2.0f, 0.85f, 0.90f, 0.92f, 1.0f);
            drawCenteredText("SPACE  LAUNCH", WINDOW_WIDTH / 2.0f, 365.0f, 2.5f, 0.85f, 0.90f, 0.92f, 1.0f);

            drawCenteredText("PRESS ENTER TO START", WINDOW_WIDTH / 2.0f, 265.0f, 4.0f, 1.0f, 0.33f, 0.45f, 1.0f);
            drawCenteredText("OTHER CONTROLS", WINDOW_WIDTH / 2.0f, 190.0f, 2.5f, 0.55f, 0.70f, 0.75f, 1.0f);
            drawCenteredText("R RESET    ESC QUIT", WINDOW_WIDTH / 2.0f, 155.0f, 2.5f, 0.55f, 0.70f, 0.75f, 1.0f);
    }

    private static void drawLevelTransition(int level) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        verticalGradient(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT,
                0.01f, 0.06f, 0.08f, 0.04f, 0.24f, 0.28f);
        color(0.35f, 0.09f, 0.05f, 1.0f);
        rect(26.0f, 26.0f, WINDOW_WIDTH - 26.0f, WINDOW_HEIGHT - 26.0f);
        color(0.01f, 0.05f, 0.07f, 1.0f);
        rect(40.0f, 40.0f, WINDOW_WIDTH - 40.0f, WINDOW_HEIGHT - 40.0f);
        float levelTextWidth = textWidth("LEVEL", 10.0f);
        float levelNumberWidth = 60.0f;
        float levelStart = WINDOW_WIDTH / 2.0f - (levelTextWidth + 10.0f + levelNumberWidth) / 2.0f;
        drawText("LEVEL", levelStart, 650.0f, 10.0f, 1.0f, 0.33f, 0.45f, 1.0f);
        drawNumber(level, 1, levelStart + levelTextWidth + 10.0f + levelNumberWidth / 2.0f,
                680.0f, levelNumberWidth, 70.0f, 8.0f, 1.0f, 0.33f, 0.45f, 1.0f, false);
        drawCenteredText("NEW TABLE LAYOUT", WINDOW_WIDTH / 2.0f, 545.0f,
                2.5f, 0.85f, 0.90f, 0.92f, 1.0f);
    }
    private static void drawPlayfieldArt(float centerX) {
        // Keep the playfield background clear so decorative markers are not
        // mistaken for collectible power-ups or pickups.
    }

    private static void drawLaneArt() {
        // Upward chevrons showing the launch direction.
        for (int index = 0; index < 4; index++) {
            float y = 240.0f + index * 90.0f;
            float x = GameSimulation.LAUNCHER_X;
            color(0.20f, 0.60f, 0.62f, 0.45f);
            capsule(new Vec2(x - 14.0f, y), new Vec2(x, y + 12.0f), 2.5f);
            capsule(new Vec2(x + 14.0f, y), new Vec2(x, y + 12.0f), 2.5f);
        }
    }

    private static void drawRails(GameSimulation simulation) {
        List<Rail> kickers = new ArrayList<>();
        for (Rail rail : simulation.rails()) {
            if (rail.isKicker()) {
                kickers.add(rail);
            }
        }
        // Slingshot bodies: rails come in triples that form one triangle.
        for (int index = 0; index + 2 < kickers.size(); index += 3) {
            color(0.75f, 0.10f, 0.25f, 1.0f);
            triangle(kickers.get(index).a(), kickers.get(index + 1).a(), kickers.get(index + 2).a());
        }
        // Dark outline first, then the bright rail on top.
        for (Rail rail : simulation.rails()) {
            color(0.02f, 0.10f, 0.12f, 1.0f);
            capsule(rail.a(), rail.b(), rail.radius() + 2.0f);
        }
        for (Rail rail : simulation.rails()) {
            if (rail.isKicker()) {
                color(1.0f, 0.33f, 0.45f, 1.0f);
            } else {
                color(0.30f, 0.88f, 0.80f, 1.0f);
            }
            capsule(rail.a(), rail.b(), rail.radius());
        }
        if (simulation.launcherGateClosed()) {
            color(0.02f, 0.10f, 0.12f, 1.0f);
            capsule(new Vec2(GameSimulation.LANE_DIVIDER_X, GameSimulation.LANE_DIVIDER_TOP),
                new Vec2(GameSimulation.LANE_DIVIDER_X, GameSimulation.WALL_TOP), 6.0f);
            color(0.30f, 0.88f, 0.80f, 1.0f);
            capsule(new Vec2(GameSimulation.LANE_DIVIDER_X, GameSimulation.LANE_DIVIDER_TOP),
                new Vec2(GameSimulation.LANE_DIVIDER_X, GameSimulation.WALL_TOP), 4.0f);
        }
    }

    private static void drawBumpers(GameSimulation simulation) {
        Vec2[] bumpers = simulation.bumpers();
        for (int index = 0; index < bumpers.length; index++) {
            Vec2 bumper = bumpers[index];
            float glow = simulation.bumperGlow(index);
            color(1.0f, 0.85f, 0.30f, 0.10f + 0.40f * glow);
            circle(bumper.x(), bumper.y(), 44.0f);
            color(0.25f, 0.12f, 0.02f, 1.0f);
            circle(bumper.x(), bumper.y(), 31.0f);
            color(0.90f + 0.10f * glow, 0.60f + 0.32f * glow, 0.14f + 0.45f * glow, 1.0f);
            circle(bumper.x(), bumper.y(), 28.0f);
            color(0.98f, 0.86f + 0.10f * glow, 0.40f + 0.40f * glow, 1.0f);
            circle(bumper.x(), bumper.y(), 19.0f);
            color(0.38f + 0.50f * glow, 0.16f + 0.40f * glow, 0.04f + 0.10f * glow, 1.0f);
            circle(bumper.x(), bumper.y(), 9.0f);
            drawNumber(simulation.bumperValue(index), 2, bumper.x(), bumper.y() - 2.0f,
                    8.0f, 12.0f, 2.0f, 0.20f, 0.10f, 0.02f, 0.9f, false);
        }
    }

    private static void drawProceduralObstacles(GameSimulation simulation) {
        for (ScoringRail obstacle : simulation.obstacles()) {
            Rail rail = obstacle.rail();
            color(0.02f, 0.10f, 0.12f, 1.0f);
            capsule(rail.a(), rail.b(), rail.radius() + 3.0f);
            color(1.0f, 0.33f, 0.45f, 1.0f);
            capsule(rail.a(), rail.b(), rail.radius());
        }
    }

    private static void drawFlippers(GameSimulation simulation) {
        Flipper[] flippers = {simulation.leftFlipper(), simulation.rightFlipper()};
        for (Flipper flipper : flippers) {
            color(0.22f, 0.04f, 0.06f, 1.0f);
            capsule(flipper.pivot(), flipper.endpoint(), 14.0f);
            color(1.0f, 0.33f, 0.45f, 1.0f);
            capsule(flipper.pivot(), flipper.endpoint(), 12.0f);
            color(1.0f, 0.62f, 0.68f, 1.0f);
            capsule(flipper.pivot(), flipper.endpoint(), 5.0f);
            color(0.22f, 0.04f, 0.06f, 1.0f);
            circle(flipper.pivot().x(), flipper.pivot().y(), 4.5f);
        }
    }

    private static void drawBall(GameSimulation simulation) {
        Vec2 position = simulation.ball().position();
        float radius = simulation.ball().radius();
        color(0.0f, 0.0f, 0.0f, 0.35f);
        circle(position.x() + 4.0f, position.y() - 4.0f, radius);
        color(0.55f, 0.58f, 0.66f, 1.0f);
        circle(position.x(), position.y(), radius);
        color(0.82f, 0.85f, 0.91f, 1.0f);
        circle(position.x() - radius * 0.15f, position.y() + radius * 0.15f, radius * 0.72f);
        color(1.0f, 1.0f, 1.0f, 0.95f);
        circle(position.x() - radius * 0.35f, position.y() + radius * 0.35f, radius * 0.25f);
    }

    private static void drawPlunger(GameSimulation simulation) {
        float x = GameSimulation.LAUNCHER_X;
        float ballY = simulation.ballInLauncher()
                ? simulation.ball().position().y() : GameSimulation.LAUNCHER_REST_Y;
        float headTop = ballY - simulation.ball().radius();
        float headBottom = headTop - 8.0f;
        float springBottom = 33.0f;

        color(0.72f, 0.74f, 0.80f, 1.0f);
        int coils = 8;
        for (int index = 0; index < coils; index++) {
            float y0 = springBottom + (headBottom - springBottom) * index / coils;
            float y1 = springBottom + (headBottom - springBottom) * (index + 1) / coils;
            float x0 = x + (index % 2 == 0 ? -10.0f : 10.0f);
            float x1 = x + (index % 2 == 0 ? 10.0f : -10.0f);
            capsule(new Vec2(x0, y0), new Vec2(x1, y1), 1.8f);
        }
        color(1.0f, 0.33f, 0.45f, 1.0f);
        rect(x - 14.0f, headBottom, x + 14.0f, headTop);
    }

    // ------------------------------------------------------------------ HUD

    private static void drawHud(GameSimulation simulation, float centerX) {
        drawText("SCORE", 300.0f, 1165.0f, 2.0f, 0.95f, 0.75f, 0.20f, 1.0f);
        drawNumber(simulation.score(), 6, centerX, 1105.0f, 34.0f, 58.0f, 7.0f, 0.95f, 0.75f, 0.2f, 0.65f, true);
        drawText("LEVEL " + simulation.level(), 300.0f, 1040.0f, 1.8f, 0.95f, 0.75f, 0.20f, 1.0f);

        // Keep controls and ball indicators in the cabinet corners, away from the flippers.
        drawText("A/D OR ARROWS", 45.0f, 1165.0f, 1.8f, 0.85f, 0.90f, 0.92f, 1.0f);
        drawText("SPACE LAUNCH", 45.0f, 1135.0f, 1.8f, 0.85f, 0.90f, 0.92f, 1.0f);
        float ballsHudCenter = 594.0f;
        drawCenteredText("BALLS", ballsHudCenter, 1165.0f, 1.8f, 0.95f, 0.75f, 0.20f, 1.0f);
        drawCenteredText("NEXT " + simulation.nextLevelScore(), ballsHudCenter, 1090.0f,
                1.8f, 0.85f, 0.90f, 0.92f, 1.0f);
        for (int index = 0; index < 3; index++) {
            float x = ballsHudCenter - 24.0f + index * 24.0f;
            if (index < simulation.ballsRemaining()) {
                color(0.95f, 0.70f, 0.20f, 0.95f);
            } else {
                color(0.10f, 0.24f, 0.27f, 0.9f);
            }
            circle(x, 1138.0f, 6.0f);
        }

        color(0.04f, 0.22f, 0.27f, 0.95f);
        rect(445.0f, 1035.0f, 615.0f, 1065.0f);
        drawCenteredText("N DEV: QUALIFY LEVEL", 530.0f, 1045.0f,
                1.35f, 0.55f, 0.95f, 0.85f, 1.0f);
    }

    private static void drawPowerMeter(GameSimulation simulation) {
        float x0 = GameSimulation.WALL_RIGHT + 8.0f;
        float x1 = x0 + 12.0f;
        float y0 = 120.0f;
        float y1 = 520.0f;
        color(0.05f, 0.05f, 0.07f, 1.0f);
        rect(x0 - 2.0f, y0 - 2.0f, x1 + 2.0f, y1 + 2.0f);
        color(0.14f, 0.15f, 0.18f, 1.0f);
        rect(x0, y0, x1, y1);
        float power = simulation.launcherPower();
        if (power > 0.0f) {
            float fillTop = y0 + (y1 - y0) * power;
            verticalGradient(x0, y0, x1, fillTop, 0.20f, 0.85f, 0.35f, 0.95f * power + 0.05f, 0.85f - 0.65f * power, 0.15f);
        }
    }

    private static void drawNumber(int value, int digits, float centerX, float centerY, float digitWidth,
                                   float digitHeight, float thickness, float r, float g, float b, float alpha,
                                   boolean showGhost) {
        float gap = digitWidth * 0.35f;
        float totalWidth = digits * digitWidth + (digits - 1) * gap;
        float startX = centerX - totalWidth / 2.0f;
        int clamped = Math.max(0, Math.min(value, (int) Math.pow(10, digits) - 1));
        boolean leading = true;
        for (int index = 0; index < digits; index++) {
            int divisor = (int) Math.pow(10, digits - 1 - index);
            int digit = (clamped / divisor) % 10;
            if (digit != 0 || index == digits - 1) {
                leading = false;
            }
            float x = startX + index * (digitWidth + gap);
            float y = centerY - digitHeight / 2.0f;
            if (showGhost) {
                color(r, g, b, 0.07f);
                drawDigit(x, y, digitWidth, digitHeight, thickness, 0b1111111);
            }
            color(r, g, b, leading && showGhost ? alpha * 0.35f : alpha);
            drawDigit(x, y, digitWidth, digitHeight, thickness, DIGIT_SEGMENTS[digit]);
        }
    }

    private static void drawDigit(float x, float y, float w, float h, float t, int mask) {
        float half = h / 2.0f;
        if ((mask & 0b0000001) != 0) rect(x + t, y + h - t, x + w - t, y + h);          // a (top)
        if ((mask & 0b0000010) != 0) rect(x + w - t, y + half, x + w, y + h - t);      // b (top right)
        if ((mask & 0b0000100) != 0) rect(x + w - t, y + t, x + w, y + half);          // c (bottom right)
        if ((mask & 0b0001000) != 0) rect(x + t, y, x + w - t, y + t);                 // d (bottom)
        if ((mask & 0b0010000) != 0) rect(x, y + t, x + t, y + half);                  // e (bottom left)
        if ((mask & 0b0100000) != 0) rect(x, y + half, x + t, y + h - t);              // f (top left)
        if ((mask & 0b1000000) != 0) rect(x + t, y + half - t / 2.0f, x + w - t, y + half + t / 2.0f); // g (middle)
    }

    // ------------------------------------------------------------------ primitives

    private static void drawText(String text, float x, float y, float scale,
                                 float r, float g, float b, float a) {
        text = text.toUpperCase();
        float cursor = x;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int[] rows = glyph(character);
            for (int row = 0; row < rows.length; row++) {
                for (int column = 0; column < 5; column++) {
                    if ((rows[row] & (1 << (4 - column))) != 0) {
                        color(r, g, b, a);
                        rect(cursor + column * scale, y + (6 - row) * scale,
                                cursor + (column + 1) * scale, y + (7 - row) * scale);
                    }
                }
            }
            cursor += character == ' ' ? 3.0f * scale : 6.0f * scale;
        }
    }

    private static void drawCenteredText(String text, float centerX, float y, float scale,
                                         float r, float g, float b, float a) {
        drawText(text, centerX - textWidth(text, scale) / 2.0f, y, scale, r, g, b, a);
    }

    private static float textWidth(String text, float scale) {
        float width = 0.0f;
        for (int index = 0; index < text.length(); index++) {
            width += text.charAt(index) == ' ' ? 3.0f * scale : 6.0f * scale;
        }
        return Math.max(0.0f, width - scale);
    }

    private static int[] glyph(char character) {
        return switch (character) {
            case 'A' -> new int[]{14, 17, 17, 31, 17, 17, 17};
            case 'B' -> new int[]{30, 17, 17, 30, 17, 17, 30};
            case 'C' -> new int[]{14, 17, 16, 16, 16, 17, 14};
            case 'D' -> new int[]{30, 17, 17, 17, 17, 17, 30};
            case 'E' -> new int[]{31, 16, 16, 30, 16, 16, 31};
            case 'F' -> new int[]{31, 16, 16, 30, 16, 16, 16};
            case 'G' -> new int[]{14, 17, 16, 23, 17, 17, 15};
            case 'H' -> new int[]{17, 17, 17, 31, 17, 17, 17};
            case 'I' -> new int[]{31, 4, 4, 4, 4, 4, 31};
            case 'K' -> new int[]{17, 18, 20, 24, 20, 18, 17};
            case 'L' -> new int[]{16, 16, 16, 16, 16, 16, 31};
            case 'M' -> new int[]{17, 27, 21, 21, 17, 17, 17};
            case 'N' -> new int[]{17, 25, 21, 21, 19, 17, 17};
            case 'O' -> new int[]{14, 17, 17, 17, 17, 17, 14};
            case 'P' -> new int[]{30, 17, 17, 30, 16, 16, 16};
            case 'Q' -> new int[]{14, 17, 17, 17, 21, 18, 13};
            case 'R' -> new int[]{30, 17, 17, 30, 20, 18, 17};
            case 'S' -> new int[]{15, 16, 16, 14, 1, 1, 30};
            case 'T' -> new int[]{31, 4, 4, 4, 4, 4, 4};
            case 'U' -> new int[]{17, 17, 17, 17, 17, 17, 14};
            case 'V' -> new int[]{17, 17, 17, 17, 17, 10, 4};
            case 'W' -> new int[]{17, 17, 17, 21, 21, 21, 10};
            case 'X' -> new int[]{17, 17, 10, 4, 10, 17, 17};
            case 'Y' -> new int[]{17, 17, 10, 4, 4, 4, 4};
            case 'Z' -> new int[]{31, 1, 2, 4, 8, 16, 31};
            case '0' -> new int[]{14, 17, 19, 21, 25, 17, 14};
            case '1' -> new int[]{4, 12, 4, 4, 4, 4, 14};
            case '2' -> new int[]{14, 17, 1, 2, 4, 8, 31};
            case '3' -> new int[]{30, 1, 1, 14, 1, 1, 30};
            case '4' -> new int[]{2, 6, 10, 18, 31, 2, 2};
            case '5' -> new int[]{31, 16, 16, 30, 1, 1, 30};
            case '6' -> new int[]{14, 16, 16, 30, 17, 17, 14};
            case '7' -> new int[]{31, 1, 2, 4, 8, 8, 8};
            case '8' -> new int[]{14, 17, 17, 14, 17, 17, 14};
            case '9' -> new int[]{14, 17, 17, 15, 1, 1, 14};
            case ' ' -> new int[]{0, 0, 0, 0, 0, 0, 0};
            case ',' -> new int[]{0, 0, 0, 0, 0, 4, 8};
            case '.' -> new int[]{0, 0, 0, 0, 0, 6, 6};
            case ':' -> new int[]{0, 4, 4, 0, 4, 4, 0};
            default -> new int[]{0, 0, 0, 0, 0, 0, 0};
        };
    }

    private static void color(float r, float g, float b, float a) {
        glColor4f(r, g, b, a);
    }

    private static void rect(float x0, float y0, float x1, float y1) {
        glBegin(GL_QUADS);
        glVertex2f(x0, y0);
        glVertex2f(x1, y0);
        glVertex2f(x1, y1);
        glVertex2f(x0, y1);
        glEnd();
    }

    private static void verticalGradient(float x0, float y0, float x1, float y1,
                                         float br, float bg, float bb, float tr, float tg, float tb) {
        glBegin(GL_QUADS);
        glColor4f(br, bg, bb, 1.0f);
        glVertex2f(x0, y0);
        glVertex2f(x1, y0);
        glColor4f(tr, tg, tb, 1.0f);
        glVertex2f(x1, y1);
        glVertex2f(x0, y1);
        glEnd();
    }

    private static void circle(float cx, float cy, float radius) {
        int segments = Math.max(24, (int) (radius * 2.0f));
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(cx, cy);
        for (int index = 0; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            glVertex2f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius);
        }
        glEnd();
    }

    private static void capsule(Vec2 a, Vec2 b, float radius) {
        Vec2 axis = b.subtract(a);
        float length = axis.length();
        if (length > 0.0001f) {
            float nx = -axis.y() / length * radius;
            float ny = axis.x() / length * radius;
            glBegin(GL_QUADS);
            glVertex2f(a.x() + nx, a.y() + ny);
            glVertex2f(b.x() + nx, b.y() + ny);
            glVertex2f(b.x() - nx, b.y() - ny);
            glVertex2f(a.x() - nx, a.y() - ny);
            glEnd();
        }
        circle(a.x(), a.y(), radius);
        circle(b.x(), b.y(), radius);
    }

    private static void triangle(Vec2 a, Vec2 b, Vec2 c) {
        glBegin(GL_TRIANGLES);
        glVertex2f(a.x(), a.y());
        glVertex2f(b.x(), b.y());
        glVertex2f(c.x(), c.y());
        glEnd();
    }

}