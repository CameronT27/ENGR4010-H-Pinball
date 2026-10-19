
package rendering;

import org.joml.Matrix4f;

public class Camera2D {

    private final float width;
    private final float height;
    private final Matrix4f projectionMatrix;

    public Camera2D(float width, float height) {
        this.width = width;
        this.height = height;
        projectionMatrix = new Matrix4f();
        updateProjection();
    }

    private void updateProjection() {
        projectionMatrix.identity().ortho(
                0.0f,
                width,
                0.0f,
                height,
                -1.0f,
                1.0f
        );
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
