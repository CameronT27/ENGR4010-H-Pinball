import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
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

            while (!glfwWindowShouldClose(window)) {
                if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                    glfwSetWindowShouldClose(window, true);
                }

                glClear(GL_COLOR_BUFFER_BIT);
                drawMessage();
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

    private static void drawMessage() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glColor3f(0.25f, 0.9f, 0.65f);
        glLineWidth(6.0f);
        glBegin(GL_LINES);

        String message = "IT RUNS";
        float x = 360.0f;
        float y = 320.0f;
        float size = 72.0f;
        for (int index = 0; index < message.length(); index++) {
            char character = message.charAt(index);
            drawGlyph(character, x, y, size);
            x += character == ' ' ? size * 0.6f : size * 0.9f;
        }

        glEnd();
    }

    private static void drawGlyph(char character, float x, float y, float size) {
        float right = x + size * 0.65f;
        float middle = y + size * 0.5f;
        float top = y + size;

        switch (character) {
            case 'I' -> {
                line(x, top, right, top);
                line(x + size * 0.325f, top, x + size * 0.325f, y);
                line(x, y, right, y);
            }
            case 'T' -> {
                line(x, top, right, top);
                line(x + size * 0.325f, top, x + size * 0.325f, y);
            }
            case 'R' -> {
                line(x, y, x, top);
                line(x, top, right * 0.98f, top);
                line(right * 0.98f, top, right * 0.98f, middle);
                line(right * 0.98f, middle, x, middle);
                line(x + size * 0.325f, middle, right, y);
            }
            case 'U' -> {
                line(x, top, x, y);
                line(x, y, right, y);
                line(right, y, right, top);
            }
            case 'N' -> {
                line(x, y, x, top);
                line(x, top, right, y);
                line(right, y, right, top);
            }
            case 'S' -> {
                line(right, top, x, top);
                line(x, top, x, middle);
                line(x, middle, right, middle);
                line(right, middle, right, y);
                line(right, y, x, y);
            }
            case '!' -> {
                line(x + size * 0.325f, top, x + size * 0.325f, y + size * 0.2f);
                line(x + size * 0.325f, y, x + size * 0.325f, y);
            }
            default -> {
            }
        }
    }

    private static void line(float x1, float y1, float x2, float y2) {
        glVertex2f(x1, y1);
        glVertex2f(x2, y2);
    }
}
