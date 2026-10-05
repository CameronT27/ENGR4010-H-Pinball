package game;

import org.lwjgl.glfw.GLFW;

public class Window {

    private final int width;
    private final int height;
    private final String title;

    private long windowHandle;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void create() {

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        windowHandle = GLFW.glfwCreateWindow(
                width,
                height,
                title,
                0,
                0
        );

        if (windowHandle == 0) {
            GLFW.glfwTerminate();
            throw new IllegalStateException("Unable to create GLFW window.");
        }

        GLFW.glfwMakeContextCurrent(windowHandle);

        GLFW.glfwSwapInterval(1);

        GLFW.glfwShowWindow(windowHandle);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void update() {
        GLFW.glfwSwapBuffers(windowHandle);
        GLFW.glfwPollEvents();
    }

    public void destroy() {
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
    }

    public long getHandle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
