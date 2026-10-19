
package rendering;

public class GameObject {

    private final Mesh mesh;
    private final Transform transform;
    private final float[] color;

    public GameObject(Mesh mesh, Transform transform) {
        this(mesh, transform, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public GameObject(
            Mesh mesh,
            Transform transform,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        this.mesh = mesh;
        this.transform = transform;
        this.color = new float[] { red, green, blue, alpha };
    }

    public void render(ShaderProgram shaderProgram) {
        shaderProgram.setUniform(
                "model",
                transform.getModelMatrix()
        );

        shaderProgram.setUniform(
                "objectColor",
                color[0],
                color[1],
                color[2],
                color[3]
        );

        mesh.render();
    }

    public void cleanup() {
        mesh.cleanup();
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Transform getTransform() {
        return transform;
    }
}
