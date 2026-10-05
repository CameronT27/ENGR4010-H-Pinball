package rendering;

import org.joml.Matrix4f;
import org.joml.Vector2f;

public class Transform {

    private final Vector2f position;
    private final Vector2f scale;

    private float rotation;

    public Transform() {

        position = new Vector2f(0.0f, 0.0f);
        scale = new Vector2f(1.0f, 1.0f);

        rotation = 0.0f;
    }

    public Matrix4f getModelMatrix() {

        return new Matrix4f()
                .identity()
                .translate(position.x, position.y, 0.0f)
                .rotateZ((float) Math.toRadians(rotation))
                .scale(scale.x, scale.y, 1.0f);
    }

    public Vector2f getPosition() {
        return position;
    }

    public Vector2f getScale() {
        return scale;
    }

    public float getRotation() {
        return rotation;
    }

    public void setPosition(float x, float y) {

        position.set(x, y);
    }

    public void setScale(float x, float y) {

        scale.set(x, y);
    }

    public void setRotation(float rotation) {

        this.rotation = rotation;
    }
}
