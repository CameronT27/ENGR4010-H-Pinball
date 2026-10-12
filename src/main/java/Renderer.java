
package rendering;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Renderer {

    private GameObject[] tableObjects;
    private ShaderProgram shaderProgram;
    private Camera2D camera;

    public void init() {
        GL.createCapabilities();

        GL11.glClearColor(0.035f, 0.045f, 0.075f, 1.0f);

        camera = new Camera2D(720.0f, 1200.0f);

        shaderProgram = new ShaderProgram(
                "shaders/basic.vert",
                "shaders/basic.frag"
        );

        tableObjects = new GameObject[] {
            // Main playfield.
            rectangle(40, 40, 640, 1120),

            // Left and right side rails.
            rectangle(55, 70, 12, 1060),
            rectangle(653, 70, 12, 1060),

            // Top rail.
            rectangle(55, 1118, 610, 12),

            // Launcher lane on the right.
            rectangle(575, 90, 55, 1000),

            // Lower flipper supports.
            rectangle(175, 180, 130, 18),
            rectangle(415, 180, 130, 18),

            // Left and right flippers (initially simple bars).
            rectangle(205, 205, 150, 24),
            rectangle(365, 205, 150, 24),

            // Three bumper shapes (square placeholders for now).
            rectangle(250, 830, 70, 70),
            rectangle(400, 830, 70, 70),
            rectangle(325, 950, 70, 70)
        };
    }

    private GameObject rectangle(
            float x,
            float y,
            float width,
            float height
    ) {
        float[] vertices = {
            0, 0,
            1, 0,
            1, 1,

            0, 0,
            1, 1,
            0, 1
        };

        Mesh mesh = new Mesh(vertices);

        Transform transform = new Transform();
        transform.setPosition(x, y);
        transform.setScale(width, height);
        transform.setRotation(0);

        return new GameObject(mesh, transform);
    }

    public void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        shaderProgram.bind();
        shaderProgram.setUniform(
                "projection",
                camera.getProjectionMatrix()
        );

        for (GameObject object : tableObjects) {
            object.render(shaderProgram);
        }

        shaderProgram.unbind();
    }

    public void cleanup() {
        if (tableObjects != null) {
            for (GameObject object : tableObjects) {
                object.cleanup();
            }
        }

        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }
}
