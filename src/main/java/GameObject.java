package rendering;

public class GameObject {

    private final Mesh mesh;
    private final Transform transform;

    public GameObject(
            Mesh mesh,
            Transform transform
    ) {

        this.mesh = mesh;
        this.transform = transform;
    }

    public void render(ShaderProgram shaderProgram) {

        shaderProgram.setUniform(
                "model",
                transform.getModelMatrix()
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
