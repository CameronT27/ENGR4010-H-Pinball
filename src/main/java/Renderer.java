package rendering;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import game.Window;
public class Renderer {

    private GameObject[] tableObjects;
    private ShaderProgram shaderProgram;
    private Camera2D camera;
    private Window window;

    public void init(Window window) {
        this.window = window;

        GL.createCapabilities();

        GL11.glClearColor(0.025f, 0.035f, 0.065f, 1.0f);

        camera = new Camera2D(720.0f, 1200.0f);

        shaderProgram = new ShaderProgram(
                "shaders/basic.vert",
                "shaders/basic.frag"
        );

        tableObjects = new GameObject[] {
            // Playfield background.
            rectangle(
                    40, 40, 640, 1120,
                    0.06f, 0.09f, 0.17f
            ),

            // Side rails.
            rectangle(
                    55, 70, 12, 1060,
                    0.15f, 0.65f, 1.0f
            ),
            rectangle(
                    653, 70, 12, 1060,
                    0.15f, 0.65f, 1.0f
            ),

            // Top rail.
            rectangle(
                    55, 1118, 610, 12,
                    0.15f, 0.65f, 1.0f
            ),

            // Launcher lane.
            rectangle(
                    575, 90, 55, 1000,
                    0.12f, 0.18f, 0.29f
            ),

            // Flipper supports.
            rectangle(
                    175, 180, 130, 18,
                    0.85f, 0.45f, 0.12f
            ),
            rectangle(
                    415, 180, 130, 18,
                    0.85f, 0.45f, 0.12f
            ),

            // Flippers.
            flipper(205, 225, 115, 28, false, 1.0f, 0.72f, 0.18f),
            flipper(515, 225, 115, 28, true,  1.0f, 0.72f, 0.18f),

            // Bumper placeholders.
            circle(285, 865, 38, 1.0f, 0.20f, 0.30f),
            circle(435, 865, 38, 0.20f, 0.85f, 1.0f),
            circle(360, 985, 38, 0.75f, 0.30f, 1.0f)
        };
    }

    private GameObject rectangle(
            float x,
            float y,
            float width,
            float height,
            float red,
            float green,
            float blue
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
        transform.setRotation(0.0f);

        return new GameObject(
                mesh,
                transform,
                red,
                green,
                blue,
                1.0f
        );
    }

    

    private GameObject flipper(
            float x,
            float y,
            float length,
            float thickness,
            boolean mirror,
            float red,
            float green,
            float blue
    ) {
        float[] vertices = {
            0.00f, 0.20f,
            0.08f, 0.00f,
            0.82f, 0.12f,

            0.00f, 0.20f,
            0.82f, 0.12f,
            1.00f, 0.50f,

            0.00f, 0.20f,
            1.00f, 0.50f,
            0.82f, 0.88f,

            0.00f, 0.20f,
            0.82f, 0.88f,
            0.08f, 1.00f
        };

        if (mirror) {
            for (int i = 0; i < vertices.length; i += 2) {
                vertices[i] = 1.0f - vertices[i];
            }
        }

        Mesh mesh = new Mesh(vertices);

        Transform transform = new Transform();
        transform.setPosition(x, y - thickness / 2.0f);
        transform.setScale(length, thickness);
        transform.setRotation(mirror ? 225.0f : 135.0f);

        return new GameObject(
                mesh,
                transform,
                red,
                green,
                blue,
                1.0f
        );
    }



    
    private GameObject circle(
            float centerX,
            float centerY,
            float radius,
            float red,
            float green,
            float blue
    ) {
        int segments = 48;
        float[] vertices = new float[segments * 9];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            int index = i * 9;

            // Center vertex.
            vertices[index] = 0.5f;
            vertices[index + 1] = 0.5f;

            // First point on the circumference.
            vertices[index + 2] =
                    0.5f + 0.5f * (float) Math.cos(angle1);
            vertices[index + 3] =
                    0.5f + 0.5f * (float) Math.sin(angle1);

            // Second point on the circumference.
            vertices[index + 4] =
                    0.5f + 0.5f * (float) Math.cos(angle2);
            vertices[index + 5] =
                    0.5f + 0.5f * (float) Math.sin(angle2);

            // Correct the triangle's vertex stride:
            // each triangle contains three vec2 vertices.
            vertices[index + 6] = 0.0f;
            vertices[index + 7] = 0.0f;
            vertices[index + 8] = 0.0f;
        }

        // Build a proper triangle list for the circle.
        float[] circleVertices = new float[segments * 6];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            int index = i * 6;

            circleVertices[index] = 0.5f;
            circleVertices[index + 1] = 0.5f;

            circleVertices[index + 2] =
                    0.5f + 0.5f * (float) Math.cos(angle1);
            circleVertices[index + 3] =
                    0.5f + 0.5f * (float) Math.sin(angle1);

            circleVertices[index + 4] =
                    0.5f + 0.5f * (float) Math.cos(angle2);
            circleVertices[index + 5] =
                    0.5f + 0.5f * (float) Math.sin(angle2);
        }

        Mesh mesh = new Mesh(circleVertices);

        Transform transform = new Transform();
        transform.setPosition(centerX - radius, centerY - radius);
        transform.setScale(radius * 2.0f, radius * 2.0f);
        transform.setRotation(0.0f);

        return new GameObject(
                mesh,
                transform,
                red,
                green,
                blue,
                1.0f
        );
    }
    
    private void updateViewport(int windowWidth, int windowHeight) {
        if (windowWidth <= 0 || windowHeight <= 0) {
            return;
        }

        float worldAspect = camera.getWidth() / camera.getHeight();
        float windowAspect = (float) windowWidth / windowHeight;

        int viewportWidth;
        int viewportHeight;

        if (windowAspect > worldAspect) {
            viewportHeight = windowHeight;
            viewportWidth = Math.round(viewportHeight * worldAspect);
        } else {
            viewportWidth = windowWidth;
            viewportHeight = Math.round(viewportWidth / worldAspect);
        }

        int x = (windowWidth - viewportWidth) / 2;
        int y = (windowHeight - viewportHeight) / 2;

        GL11.glViewport(x, y, viewportWidth, viewportHeight);
    }



    public void render() {
         updateViewport();

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

    private void updateViewport() {
        int[] size = window.getFramebufferSize();
        int windowWidth = size[0];
        int windowHeight = size[1];

        if (windowWidth <= 0 || windowHeight <= 0) {
            return;
        }

        float worldAspect = camera.getWidth() / camera.getHeight();
        float windowAspect = (float) windowWidth / windowHeight;

        int viewportWidth;
        int viewportHeight;

        if (windowAspect > worldAspect) {
            viewportHeight = windowHeight;
            viewportWidth = Math.round(viewportHeight * worldAspect);
        } else {
            viewportWidth = windowWidth;
            viewportHeight = Math.round(viewportWidth / worldAspect);
        }

        int x = (windowWidth - viewportWidth) / 2;
        int y = (windowHeight - viewportHeight) / 2;

        GL11.glViewport(x, y, viewportWidth, viewportHeight);
    }

}
