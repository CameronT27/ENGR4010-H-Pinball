package rendering;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import game.Window;

public class Renderer {

    private GameObject[] tableObjects;
    private ShaderProgram shaderProgram;
    private Camera2D camera;
    private Matrix4f angledProjection;
    private Window window;
    private game.GameSimulation simulation;
    private GameObject ballObject;

    public void init(Window window, game.GameSimulation simulation) {
        this.window = window;
        this.simulation = simulation;

        GL.createCapabilities();

        GL11.glClearColor(0.025f, 0.035f, 0.065f, 1.0f);

        camera = new Camera2D(720.0f, 1200.0f);
        angledProjection = createAngledProjection();

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
            flipper(175, 198, 115, 28, false, 1.0f, 0.72f, 0.18f),
            flipper(545, 198, 115, 28, true, 1.0f, 0.72f, 0.18f),

            // Bumper placeholders.
            circle(285, 865, 38, 1.0f, 0.20f, 0.30f),
            circle(435, 865, 38, 0.20f, 0.85f, 1.0f),
            circle(360, 985, 38, 0.75f, 0.30f, 1.0f)
        };

        // Create the ball using its initial simulation position.
        game.Vec2 ballPosition = simulation.ball().position();
        float ballRadius = simulation.ball().radius();

        ballObject = circle(
                ballPosition.x(),
                ballPosition.y(),
                ballRadius,
                0.92f, 0.95f, 1.0f
        );
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
        // Both flippers use the same shape.
        // The broad hinge is at x = 0; the narrow tip is at x = 1.
        float[] vertices = {
                0.00f, 0.30f,
                0.10f, 0.00f,
                0.78f, 0.18f,

                0.00f, 0.30f,
                0.78f, 0.18f,
                1.00f, 0.50f,

                0.00f, 0.30f,
                1.00f, 0.50f,
                0.78f, 0.82f,

                0.00f, 0.30f,
                0.78f, 0.82f,
                0.10f, 1.00f
        };

        Mesh mesh = new Mesh(vertices);
        Transform transform = new Transform();

        // Preserve the current flipper placement.
        transform.setPosition(
                x,
                y - thickness / 2.0f + 65.0f
        );

        if (mirror) {
            transform.setScale(-length, thickness);
            transform.setRotation(25.0f);
        } else {
            transform.setScale(length, thickness);
            transform.setRotation(-25.0f);
        }

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

        // Three vertices per triangle, two coordinates per vertex.
        float[] vertices = new float[segments * 6];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            int index = i * 6;

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
        }

        Mesh mesh = new Mesh(vertices);

        Transform transform = new Transform();
        transform.setPosition(
                centerX - radius,
                centerY - radius
        );
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

    /**
     * Applies a single perspective transform to the entire table, including
     * rails, bumpers, flippers, and the ball. No physics positions are changed.
     *
     * The near edge stays wide, the far edge narrows, and distance along the
     * table becomes increasingly compressed. The table fits inside the same
     * 720 x 1200 camera view.
     */
    private Matrix4f createAngledProjection() {
        final float tableCenterX = 360.0f;
        final float nearWidthScale = 0.90f;
        final float depthPerspective = 0.00050f;
        final float nearScreenY = 85.0f;
        final float farScreenY = 1060.0f;
        final float tableHeight = 1200.0f;

        // At y = tableHeight, the perspective divider is (1 + k * y).
        // Solve for the vertical coefficient so the far end stays visible.
        float verticalCoefficient =
                (farScreenY - nearScreenY)
                * (1.0f + depthPerspective * tableHeight)
                / tableHeight;

        // Homography: projected (x,y) = (numeratorX, numeratorY) / w.
        // This matrix is applied before the existing orthographic camera.
        Matrix4f tablePerspective = new Matrix4f().identity();
        tablePerspective.m00(nearWidthScale);
        tablePerspective.m10(tableCenterX * depthPerspective);
        tablePerspective.m30(tableCenterX * (1.0f - nearWidthScale));
        tablePerspective.m11(verticalCoefficient + nearScreenY * depthPerspective);
        tablePerspective.m31(nearScreenY);
        tablePerspective.m13(depthPerspective);

        return new Matrix4f(camera.getProjectionMatrix()).mul(tablePerspective);
    }

    public void render() {
        updateViewport();

        // Clear the previous frame.
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Activate the shader and set the camera projection.
        shaderProgram.bind();

        shaderProgram.setUniform(
                "projection",
                angledProjection
        );

        // Draw the table and all existing table objects.
        for (GameObject object : tableObjects) {
            object.render(shaderProgram);
        }

        // Update the ball's visual position from the simulation.
        game.Vec2 ballPosition = simulation.ball().position();
        float ballRadius = simulation.ball().radius();

        ballObject.getTransform().setPosition(
                ballPosition.x() - ballRadius,
                ballPosition.y() - ballRadius
        );

        // Draw the ball on top of the table.
        ballObject.render(shaderProgram);

        shaderProgram.unbind();
    }

    public void cleanup() {
        if (tableObjects != null) {
            for (GameObject object : tableObjects) {
                object.cleanup();
            }
        }

        if (ballObject != null) {
            ballObject.cleanup();
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
