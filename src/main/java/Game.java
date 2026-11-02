
package game;

import org.lwjgl.glfw.GLFW;

import rendering.Renderer;

public class Game {

    private final Window window;
    private final Renderer renderer;
    private final GameSimulation simulation;
    private final InputState input;

    public Game() {
        window = new Window(1280, 720, "Online Pinball");
        renderer = new Renderer();
        simulation = new GameSimulation();
        input = new InputState();
    }

    public void run() {
        initialize();
        gameLoop();
        cleanup();
    }

    private void initialize() {
        window.create();
        renderer.init(window, simulation);
    }

    private void gameLoop() {
        long previousFrameTime = System.nanoTime();

        while (!window.shouldClose()) {
            long currentFrameTime = System.nanoTime();

            float deltaSeconds =
                    (currentFrameTime - previousFrameTime) / 1_000_000_000.0f;

            previousFrameTime = currentFrameTime;

            readInput();

            // Advance the physics and gameplay simulation.
            simulation.update(deltaSeconds, input);

            // Draw the latest simulation state.
            renderer.render();

            // Display the frame and process window events.
            window.update();
        }
    }

    private void readInput() {
        // Flippers: A / D or the left / right arrow keys.
        input.setLeftFlipper(
                window.isKeyDown(GLFW.GLFW_KEY_A)
                || window.isKeyDown(GLFW.GLFW_KEY_LEFT)
        );

        input.setRightFlipper(
                window.isKeyDown(GLFW.GLFW_KEY_D)
                || window.isKeyDown(GLFW.GLFW_KEY_RIGHT)
        );

        // Hold Space to charge; release it to launch.
        input.setLaunchButton(
                window.isKeyDown(GLFW.GLFW_KEY_SPACE)
        );

        // Nudges.
        input.setNudgeLeft(
                window.isKeyDown(GLFW.GLFW_KEY_Q)
        );

        input.setNudgeRight(
                window.isKeyDown(GLFW.GLFW_KEY_E)
        );
    }

    private void cleanup() {
        renderer.cleanup();
        window.destroy();
    }
}
