
package rendering;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import game.Window;

public class Renderer {

    private GameObject[] tableObjects;
    private GameObject[] playfieldLightObjects;
    private GameObject[] bumperGlowObjects;
    private ShaderProgram shaderProgram;
    private Camera2D camera;
    private Matrix4f angledProjection;
    private Window window;
    private game.GameSimulation simulation;
    private GameObject[] ballObjects;

    public void init(Window window, game.GameSimulation simulation) {
        this.window = window;
        this.simulation = simulation;

        GL.createCapabilities();

        GL11.glClearColor(0.025f, 0.035f, 0.065f, 1.0f);

        // Alpha blending lets translucent light and glow layers accumulate.
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        camera = new Camera2D(720.0f, 1200.0f);
        angledProjection = createAngledProjection();

        shaderProgram = new ShaderProgram(
                "shaders/basic.vert",
                "shaders/basic.frag"
        );

        tableObjects = new GameObject[] {
            // Front cabinet wall.
            cabinetPanel(
                    18, -35, 707, -35, 707, 35, 18, 35,
                    0.075f, 0.13f, 0.23f
            ),

            // Cabinet side panels.
            cabinetPanel(
                    18, 35, 40, 35, 43, 1165, 27, 1165,
                    0.055f, 0.11f, 0.21f
            ),
            cabinetPanel(
                    680, 35, 707, 35, 693, 1165, 677, 1165,
                    0.12f, 0.21f, 0.34f
            ),

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

            // Rail bevels.
            rectangle(55, 70, 3, 1060, 0.46f, 0.86f, 1.0f),
            rectangle(58, 70, 4, 1060, 0.22f, 0.73f, 0.98f),
            rectangle(67, 70, 4, 1060, 0.035f, 0.21f, 0.38f),

            rectangle(660, 70, 4, 1060, 0.46f, 0.86f, 1.0f),
            rectangle(655, 70, 3, 1060, 0.22f, 0.73f, 0.98f),
            rectangle(649, 70, 4, 1060, 0.035f, 0.21f, 0.38f),

            rectangle(55, 1127, 610, 3, 0.46f, 0.86f, 1.0f),
            rectangle(55, 1114, 610, 4, 0.035f, 0.21f, 0.38f),

            // Launcher lane.
            rectangle(
                    575, 90, 55, 1000,
                    0.12f, 0.18f, 0.29f
            ),

            // Soft cast shadows: broad faint silhouettes first, then tighter
            // darker silhouettes. Draw before supports and flippers.
            flipperLayer(175 - 5, 198 - 15, 125, 38, false,
                    0.008f, 0.012f, 0.025f, 0.10f),
            flipperLayer(545 + 5, 198 - 15, 125, 38, true,
                    0.008f, 0.012f, 0.025f, 0.10f),
            flipperLayer(175 - 3, 198 - 15, 121, 34, false,
                    0.008f, 0.012f, 0.025f, 0.17f),
            flipperLayer(545 + 3, 198 - 15, 121, 34, true,
                    0.008f, 0.012f, 0.025f, 0.17f),
            flipperLayer(175, 198 - 15, 115, 28, false,
                    0.008f, 0.012f, 0.025f, 0.28f),
            flipperLayer(545, 198 - 15, 115, 28, true,
                    0.008f, 0.012f, 0.025f, 0.28f),

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

            // Bumper cores.
            circle(285, 865, 38, 1.0f, 0.20f, 0.30f),
            circle(435, 865, 38, 0.20f, 0.85f, 1.0f),
            circle(360, 985, 38, 0.75f, 0.30f, 1.0f)
        };

        // Soft playfield lighting.
        playfieldLightObjects = new GameObject[16];
        for (int i = 0; i < playfieldLightObjects.length; i++) {
            float t = i / (float) (playfieldLightObjects.length - 1);
            float radiusX = 280.0f - 225.0f * t;
            float radiusY = 495.0f - 395.0f * t;
            playfieldLightObjects[i] = lightEllipse(
                    355.0f, 625.0f, radiusX, radiusY,
                    0.42f, 0.63f, 0.92f, 0.014f
            );
        }

        // Colored bumper halos. Each bumper gets multiple translucent rings,
        // from widest/faintest to narrowest/brightest.
        bumperGlowObjects = new GameObject[] {
            glowCircle(285, 865, 88, 1.00f, 0.20f, 0.30f, 0.06f),
            glowCircle(285, 865, 72, 1.00f, 0.20f, 0.30f, 0.09f),
            glowCircle(285, 865, 58, 1.00f, 0.20f, 0.30f, 0.12f),
            glowCircle(285, 865, 47, 1.00f, 0.20f, 0.30f, 0.16f),

            glowCircle(435, 865, 88, 0.20f, 0.85f, 1.00f, 0.06f),
            glowCircle(435, 865, 72, 0.20f, 0.85f, 1.00f, 0.09f),
            glowCircle(435, 865, 58, 0.20f, 0.85f, 1.00f, 0.12f),
            glowCircle(435, 865, 47, 0.20f, 0.85f, 1.00f, 0.16f),

            glowCircle(360, 985, 88, 0.75f, 0.30f, 1.00f, 0.06f),
            glowCircle(360, 985, 72, 0.75f, 0.30f, 1.00f, 0.09f),
            glowCircle(360, 985, 58, 0.75f, 0.30f, 1.00f, 0.12f),
            glowCircle(360, 985, 47, 0.75f, 0.30f, 1.00f, 0.16f)
        };

        game.Vec2 ballPosition = simulation.ball().position();
        float ballRadius = simulation.ball().radius();

        ballObjects = createBallObjects(
                ballPosition.x(),
                ballPosition.y(),
                ballRadius
        );
    }

    private GameObject cabinetPanel(
            float bottomLeftX, float bottomLeftY,
            float bottomRightX, float bottomRightY,
            float topRightX, float topRightY,
            float topLeftX, float topLeftY,
            float red, float green, float blue
    ) {
        float[] vertices = {
                bottomLeftX, bottomLeftY,
                bottomRightX, bottomRightY,
                topRightX, topRightY,

                bottomLeftX, bottomLeftY,
                topRightX, topRightY,
                topLeftX, topLeftY
        };

        Mesh mesh = new Mesh(vertices);
        Transform transform = new Transform();
        transform.setPosition(0.0f, 0.0f);
        transform.setScale(1.0f, 1.0f);
        transform.setRotation(0.0f);

        return new GameObject(mesh, transform, red, green, blue, 1.0f);
    }

    private GameObject lightEllipse(
            float centerX, float centerY,
            float radiusX, float radiusY,
            float red, float green, float blue, float alpha
    ) {
        int segments = 64;
        float[] vertices = new float[segments * 6];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;
            int index = i * 6;
            vertices[index] = 0.0f;
            vertices[index + 1] = 0.0f;
            vertices[index + 2] = (float) Math.cos(angle1);
            vertices[index + 3] = (float) Math.sin(angle1);
            vertices[index + 4] = (float) Math.cos(angle2);
            vertices[index + 5] = (float) Math.sin(angle2);
        }

        Mesh mesh = new Mesh(vertices);
        Transform transform = new Transform();
        transform.setPosition(centerX, centerY);
        transform.setScale(radiusX, radiusY);
        transform.setRotation(0.0f);
        return new GameObject(mesh, transform, red, green, blue, alpha);
    }

    private GameObject glowCircle(
            float centerX,
            float centerY,
            float radius,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int segments = 56;
        float[] vertices = new float[segments * 6];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;
            int index = i * 6;

            vertices[index] = 0.5f;
            vertices[index + 1] = 0.5f;
            vertices[index + 2] = 0.5f + 0.5f * (float) Math.cos(angle1);
            vertices[index + 3] = 0.5f + 0.5f * (float) Math.sin(angle1);
            vertices[index + 4] = 0.5f + 0.5f * (float) Math.cos(angle2);
            vertices[index + 5] = 0.5f + 0.5f * (float) Math.sin(angle2);
        }

        Mesh mesh = new Mesh(vertices);
        Transform transform = new Transform();
        transform.setPosition(centerX - radius, centerY - radius);
        transform.setScale(radius * 2.0f, radius * 2.0f);
        transform.setRotation(0.0f);

        return new GameObject(mesh, transform, red, green, blue, alpha);
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
        return flipperLayer(x, y, length, thickness, mirror,
                red, green, blue, 1.0f);
    }

    // Shared geometry for the opaque flipper and translucent shadow layers.
    private GameObject flipperLayer(
            float x, float y, float length, float thickness,
            boolean mirror, float red, float green, float blue, float alpha
    ) {
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
                alpha
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
        float[] vertices = new float[segments * 6];

        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            int index = i * 6;

            vertices[index] = 0.5f;
            vertices[index + 1] = 0.5f;
            vertices[index + 2] = 0.5f + 0.5f * (float) Math.cos(angle1);
            vertices[index + 3] = 0.5f + 0.5f * (float) Math.sin(angle1);
            vertices[index + 4] = 0.5f + 0.5f * (float) Math.cos(angle2);
            vertices[index + 5] = 0.5f + 0.5f * (float) Math.sin(angle2);
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

    // Metallic ball: layered shapes, using the existing alpha-capable geometry.
    private GameObject circleAlpha(
            float centerX, float centerY, float radius,
            float red, float green, float blue, float alpha
    ) {
        return glowCircle(centerX, centerY, radius, red, green, blue, alpha);
    }

    private GameObject[] createBallObjects(
            float centerX, float centerY, float radius
    ) {
        return new GameObject[] {
            // 0 - Contact shadow.
            lightEllipse(centerX + radius * 0.10f,
                    centerY - radius * 1.02f,
                    radius * 0.82f, radius * 0.28f,
                    0.01f, 0.015f, 0.03f, 0.42f),

            // 1 - Dark outer steel rim.
            circleAlpha(centerX, centerY, radius,
                    0.34f, 0.41f, 0.53f, 1.0f),

            // 2 - Darker lower-right steel mass.
            circleAlpha(centerX + radius * 0.12f,
                    centerY - radius * 0.10f, radius * 0.90f,
                    0.14f, 0.18f, 0.26f, 0.95f),

            // 3 - Brighter silver body.
            circleAlpha(centerX - radius * 0.06f,
                    centerY + radius * 0.06f, radius * 0.76f,
                    0.86f, 0.90f, 0.97f, 0.95f),

            // 4 - Soft upper glow.
            lightEllipse(centerX - radius * 0.14f,
                    centerY + radius * 0.18f,
                    radius * 0.42f, radius * 0.30f,
                    1.0f, 1.0f, 1.0f, 0.18f),

            // 5 - Cyan ambient rail reflection.
            lightEllipse(centerX + radius * 0.16f,
                    centerY - radius * 0.04f,
                    radius * 0.38f, radius * 0.10f,
                    0.25f, 0.75f, 1.0f, 0.42f),

            // 6 - Bright specular highlight.
            lightEllipse(centerX - radius * 0.28f,
                    centerY + radius * 0.27f,
                    radius * 0.16f, radius * 0.10f,
                    1.0f, 1.0f, 1.0f, 0.88f),

            // 7 - Subtle lower-right dark reflection.
            lightEllipse(centerX + radius * 0.24f,
                    centerY - radius * 0.22f,
                    radius * 0.22f, radius * 0.15f,
                    0.08f, 0.11f, 0.17f, 0.22f)
        };
    }

    private void updateBallObjects(float centerX, float centerY, float radius) {
        setEllipseTransform(ballObjects[0],
                centerX + radius * 0.10f, centerY - radius * 1.02f,
                radius * 0.82f, radius * 0.28f);
        setCircleTransform(ballObjects[1], centerX, centerY, radius);
        setCircleTransform(ballObjects[2],
                centerX + radius * 0.12f, centerY - radius * 0.10f,
                radius * 0.90f);
        setCircleTransform(ballObjects[3],
                centerX - radius * 0.06f, centerY + radius * 0.06f,
                radius * 0.76f);
        setEllipseTransform(ballObjects[4],
                centerX - radius * 0.14f, centerY + radius * 0.18f,
                radius * 0.42f, radius * 0.30f);
        setEllipseTransform(ballObjects[5],
                centerX + radius * 0.16f, centerY - radius * 0.04f,
                radius * 0.38f, radius * 0.10f);
        setEllipseTransform(ballObjects[6],
                centerX - radius * 0.28f, centerY + radius * 0.27f,
                radius * 0.16f, radius * 0.10f);
        setEllipseTransform(ballObjects[7],
                centerX + radius * 0.24f, centerY - radius * 0.22f,
                radius * 0.22f, radius * 0.15f);
    }

    private void setCircleTransform(
            GameObject object, float centerX, float centerY, float radius
    ) {
        object.getTransform().setPosition(centerX - radius, centerY - radius);
        object.getTransform().setScale(radius * 2.0f, radius * 2.0f);
        object.getTransform().setRotation(0.0f);
    }

    private void setEllipseTransform(
            GameObject object, float centerX, float centerY,
            float radiusX, float radiusY
    ) {
        object.getTransform().setPosition(centerX, centerY);
        object.getTransform().setScale(radiusX, radiusY);
        object.getTransform().setRotation(0.0f);
    }

    private Matrix4f createAngledProjection() {
        final float tableCenterX = 360.0f;
        final float nearWidthScale = 0.90f;
        final float depthPerspective = 0.00050f;
        final float nearScreenY = 85.0f;
        final float farScreenY = 1060.0f;
        final float tableHeight = 1200.0f;

        float verticalCoefficient =
                (farScreenY - nearScreenY)
                * (1.0f + depthPerspective * tableHeight)
                / tableHeight;

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
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        shaderProgram.bind();
        shaderProgram.setUniform("projection", angledProjection);

        int firstBumperIndex = tableObjects.length - 3;

        for (int i = 0; i < tableObjects.length; i++) {
            tableObjects[i].render(shaderProgram);

            if (i == 3) {
                for (GameObject light : playfieldLightObjects) {
                    light.render(shaderProgram);
                }
            }

            if (i == firstBumperIndex - 1) {
                for (GameObject glow : bumperGlowObjects) {
                    glow.render(shaderProgram);
                }
            }
        }

        game.Vec2 ballPosition = simulation.ball().position();
        float ballRadius = simulation.ball().radius();

        updateBallObjects(
                ballPosition.x(),
                ballPosition.y(),
                ballRadius
        );

        for (GameObject object : ballObjects) {
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

        if (playfieldLightObjects != null) {
            for (GameObject light : playfieldLightObjects) {
                light.cleanup();
            }
        }

        if (bumperGlowObjects != null) {
            for (GameObject glow : bumperGlowObjects) {
                glow.cleanup();
            }
        }

        if (ballObjects != null) {
            for (GameObject object : ballObjects) {
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
