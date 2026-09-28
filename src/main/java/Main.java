import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
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
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Main {
    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;

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
                simulation.update(deltaSeconds, input);

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

        glColor3f(0.15f, 0.8f, 0.65f);
        glLineWidth(5.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(20.0f, 20.0f);
        glVertex2f(WINDOW_WIDTH - 20.0f, 20.0f);
        glVertex2f(WINDOW_WIDTH - 20.0f, WINDOW_HEIGHT - 20.0f);
        glVertex2f(20.0f, WINDOW_HEIGHT - 20.0f);
        glEnd();

        glColor3f(0.95f, 0.45f, 0.2f);
        glLineWidth(16.0f);
        glBegin(GL_LINES);
        drawSegment(simulation.leftFlipper());
        drawSegment(simulation.rightFlipper());
        glEnd();

        glColor3f(0.95f, 0.75f, 0.2f);
        glPointSize(18.0f);
        glBegin(GL_POINTS);
        for (Vec2 bumper : simulation.bumpers()) {
            glVertex2f(bumper.x(), bumper.y());
        }
        glEnd();

        glColor3f(0.95f, 0.95f, 0.95f);
        glPointSize(simulation.ball().radius() * 2.0f);
        glBegin(GL_POINTS);
        glVertex2f(simulation.ball().position().x(), simulation.ball().position().y());
        glEnd();
    }

    private static void drawSegment(Flipper flipper) {
        glVertex2f(flipper.pivot().x(), flipper.pivot().y());
        glVertex2f(flipper.endpoint().x(), flipper.endpoint().y());
    }

}
