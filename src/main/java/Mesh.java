package rendering;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

public class Mesh {

    private final int vao;
    private final int vbo;
    private final int vertexCount;

    public Mesh(float[] vertices) {

        vertexCount = vertices.length / 2;

        // Create Vertex Array Object
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // Create Vertex Buffer Object
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Convert Java array into native buffer
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        // Send vertex data to OpenGL
        GL15.glBufferData(
                GL15.GL_ARRAY_BUFFER,
                vertexBuffer,
                GL15.GL_STATIC_DRAW
        );

        // Each vertex contains:
        // X, Y
        GL30.glVertexAttribPointer(
                0,
                2,
                GL11.GL_FLOAT,
                false,
                2 * Float.BYTES,
                0
        );

        GL30.glEnableVertexAttribArray(0);

        // Free temporary native memory
        MemoryUtil.memFree(vertexBuffer);

        // Unbind
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public void render() {

        GL30.glBindVertexArray(vao);

        GL11.glDrawArrays(
                GL11.GL_TRIANGLES,
                0,
                vertexCount
        );

        GL30.glBindVertexArray(0);
    }

    public void cleanup() {

        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vao);
    }
}
