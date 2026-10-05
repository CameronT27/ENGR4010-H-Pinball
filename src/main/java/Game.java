package game;

import rendering.Renderer;

public class Game {

    private final Window window;
    private final Renderer renderer;

    public Game() {
        window = new Window(
                1280,
                720,
                "Online Pinball"
        );

        renderer = new Renderer();
    }

    public void run() {

        initialize();

        gameLoop();

        cleanup();
    }

    private void initialize() {

        window.create();

        renderer.init();
    }

    private void gameLoop() {

        while (!window.shouldClose()) {

            renderer.render();

            window.update();
        }
    }

    private void cleanup() {

        renderer.cleanup();

        window.destroy();
    }
}
