import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_Z;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_LINE_LOOP;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_POLYGON;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private static final int WINDOW_WIDTH = 720;
    private static final int WINDOW_HEIGHT = 1200;

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
            glClearColor(0.04f, 0.05f, 0.08f, 1.0f);
            GameSimulation simulation = new GameSimulation();
            InputState input = new InputState();
            double previousTime = glfwGetTime();

            while (!glfwWindowShouldClose(window)) {
                double currentTime = glfwGetTime();
                float deltaSeconds = (float) (currentTime - previousTime);
                previousTime = currentTime;
                if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    glfwSetWindowShouldClose(window, true);
                }
                input.setLeftFlipper(isPressed(window, GLFW_KEY_A) || isPressed(window, GLFW_KEY_LEFT));
                input.setRightFlipper(isPressed(window, GLFW_KEY_D) || isPressed(window, GLFW_KEY_RIGHT));
                input.setLaunchButton(isPressed(window, GLFW_KEY_SPACE));
                input.setNudgeLeft(isPressed(window, GLFW_KEY_Z));
                input.setNudgeRight(isPressed(window, GLFW_KEY_X));
                simulation.update(deltaSeconds, input);
                glfwSetWindowTitle(window, "Online Pinball | Score: " + simulation.score()
                    + " | Balls: " + simulation.ballsRemaining());

                glClear(GL_COLOR_BUFFER_BIT);
                drawTable(simulation);
                glfwSwapBuffers(window);
                glfwPollEvents();
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
        glfwWindowHint(org.lwjgl.glfw.GLFW.GLFW_VISIBLE, GLFW_FALSE);

        long window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Online Pinball", NULL, NULL);
        if (window == NULL) {
            throw new IllegalStateException("Unable to create GLFW window");
        }

        var videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode != null) {
            org.lwjgl.glfw.GLFW.glfwSetWindowPos(
                    window,
                    (videoMode.width() - WINDOW_WIDTH) / 2,
                    (videoMode.height() - WINDOW_HEIGHT) / 2
            );
        }
        return window;
    }

    private static boolean isPressed(long window, int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    private static void drawTable(GameSimulation simulation) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glColor3f(0.02f, 0.025f, 0.035f);
        glBegin(GL_QUADS);
        drawWall(0.0f, 0.0f, WINDOW_WIDTH, WINDOW_HEIGHT);
        glEnd();

        glColor3f(0.25f, 0.09f, 0.035f);
        drawPolygon(new Vec2(18.0f, 18.0f), new Vec2(702.0f, 18.0f),
            new Vec2(702.0f, 1182.0f), new Vec2(18.0f, 1182.0f));
        glColor3f(0.04f, 0.24f, 0.27f);
        drawPolygon(new Vec2(48.0f, 34.0f), new Vec2(580.0f, 34.0f),
            new Vec2(580.0f, 1148.0f), new Vec2(48.0f, 1148.0f));

        drawPlayfieldDetails();

        glColor3f(0.8f, 0.5f, 0.18f);
        glLineWidth(10.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(48.0f, 34.0f);
        glVertex2f(580.0f, 34.0f);
        glVertex2f(580.0f, 1148.0f);
        glVertex2f(48.0f, 1148.0f);
        glEnd();

        glColor3f(0.25f, 0.9f, 0.8f);
        glLineWidth(4.0f);
        glBegin(GL_LINES);
        glVertex2f(60.0f, 48.0f);
        glVertex2f(60.0f, 1140.0f);
        glVertex2f(580.0f, 48.0f);
        glVertex2f(580.0f, 1140.0f);
        glEnd();

        glColor3f(0.06f, 0.32f, 0.3f);
        glBegin(GL_QUADS);
        drawWall(28.0f, 20.0f, 20.0f, 1160.0f);
        drawWall(672.0f, 20.0f, 20.0f, 1160.0f);
        glEnd();

        glColor3f(0.04f, 0.14f, 0.17f);
        glBegin(GL_QUADS);
        drawWall(590.0f, 25.0f, 100.0f, 1120.0f);
        glEnd();
        glColor3f(0.25f, 0.9f, 0.8f);
        glLineWidth(5.0f);
        glBegin(GL_LINES);
        glVertex2f(590.0f, 30.0f);
        glVertex2f(590.0f, 1140.0f);
        glEnd();

        glColor3f(0.95f, 0.35f, 0.08f);
        glLineWidth(16.0f);
        glBegin(GL_LINES);
        drawSegment(simulation.leftFlipper());
        drawSegment(simulation.rightFlipper());
        glEnd();

        glColor3f(1.0f, 0.55f, 0.15f);
        drawCircle(simulation.leftFlipper().pivot(), 18.0f, 20);
        drawCircle(simulation.rightFlipper().pivot(), 18.0f, 20);

        glColor3f(0.95f, 0.75f, 0.2f);
        for (Vec2 bumper : simulation.bumpers()) {
            drawCircle(bumper, 28.0f, 24);
            glColor3f(0.98f, 0.92f, 0.4f);
            drawRing(bumper, 35.0f, 24);
            glColor3f(0.95f, 0.75f, 0.2f);
        }

        glColor3f(0.95f, 0.95f, 0.95f);
        drawCircle(simulation.ball().position(), simulation.ball().radius(), 32);

        drawLauncherPower(simulation);
    }

    private static void drawPlayfieldDetails() {
        glColor3f(0.06f, 0.45f, 0.44f);
        glLineWidth(3.0f);
        glBegin(GL_LINES);
        glVertex2f(125.0f, 1030.0f);
        glVertex2f(585.0f, 1030.0f);
        glVertex2f(150.0f, 970.0f);
        glVertex2f(560.0f, 970.0f);
        glVertex2f(155.0f, 560.0f);
        glVertex2f(280.0f, 560.0f);
        glVertex2f(440.0f, 560.0f);
        glVertex2f(565.0f, 560.0f);
        glEnd();

        glColor3f(0.95f, 0.15f, 0.3f);
        drawTriangle(165.0f, 310.0f, 285.0f, 355.0f, 255.0f, 250.0f);
        drawTriangle(555.0f, 310.0f, 435.0f, 355.0f, 465.0f, 250.0f);

        glColor3f(0.95f, 0.65f, 0.15f);
        drawRing(new Vec2(360.0f, 1050.0f), 38.0f, 32);
        drawCircle(new Vec2(360.0f, 1050.0f), 7.0f, 16);

        glColor3f(0.9f, 0.85f, 0.55f);
        drawCircle(new Vec2(150.0f, 470.0f), 9.0f, 16);
        drawCircle(new Vec2(570.0f, 470.0f), 9.0f, 16);
        drawCircle(new Vec2(180.0f, 900.0f), 9.0f, 16);
        drawCircle(new Vec2(540.0f, 900.0f), 9.0f, 16);

        glColor3f(0.95f, 0.7f, 0.2f);
        drawCircle(new Vec2(180.0f, 600.0f), 14.0f, 16);
        drawCircle(new Vec2(540.0f, 600.0f), 14.0f, 16);
        drawCircle(new Vec2(360.0f, 940.0f), 14.0f, 16);
    }

    private static void drawPolygon(Vec2... points) {
        glBegin(GL_POLYGON);
        for (Vec2 point : points) {
            glVertex2f(point.x(), point.y());
        }
        glEnd();
    }

    private static void drawWall(float x, float y, float width, float height) {
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
    }

    private static void drawCircle(Vec2 center, float radius, int segments) {
        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(center.x(), center.y());
        for (int index = 0; index <= segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            glVertex2f(center.x() + (float) Math.cos(angle) * radius,
                    center.y() + (float) Math.sin(angle) * radius);
        }
        glEnd();
    }

    private static void drawRing(Vec2 center, float radius, int segments) {
        glBegin(GL_LINE_LOOP);
        for (int index = 0; index < segments; index++) {
            double angle = Math.PI * 2.0 * index / segments;
            glVertex2f(center.x() + (float) Math.cos(angle) * radius,
                    center.y() + (float) Math.sin(angle) * radius);
        }
        glEnd();
    }

    private static void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3) {
        glBegin(GL_TRIANGLES);
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
        glVertex2f(x3, y3);
        glEnd();
    }

    private static void drawLauncherPower(GameSimulation simulation) {
        glColor3f(0.2f, 0.2f, 0.24f);
        glBegin(GL_QUADS);
        drawWall(610.0f, 240.0f, 12.0f, 260.0f);
        glEnd();

        glColor3f(0.95f, 0.35f, 0.12f);
        glBegin(GL_QUADS);
        drawWall(610.0f, 240.0f, 12.0f, 260.0f * simulation.launcherPower());
        glEnd();
    }

    private static void drawSegment(Flipper flipper) {
        glVertex2f(flipper.pivot().x(), flipper.pivot().y());
        glVertex2f(flipper.endpoint().x(), flipper.endpoint().y());
    }

}
