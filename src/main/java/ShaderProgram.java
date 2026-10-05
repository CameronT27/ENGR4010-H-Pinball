package rendering;

import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class ShaderProgram {

    private final int programId;

    public ShaderProgram(
            String vertexShaderPath,
            String fragmentShaderPath
    ) {

        // Read shader source files from the classpath.
        String vertexSource = readShaderFile(vertexShaderPath);
        String fragmentSource = readShaderFile(fragmentShaderPath);

        // Compile the vertex shader.
        int vertexShader = compileShader(
                vertexSource,
                GL20.GL_VERTEX_SHADER
        );

        // Compile the fragment shader.
        int fragmentShader = compileShader(
                fragmentSource,
                GL20.GL_FRAGMENT_SHADER
        );

        // Create the shader program.
        programId = GL20.glCreateProgram();

        // Attach both shaders.
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);

        // Link the program.
        GL20.glLinkProgram(programId);

        // Check whether linking succeeded.
        if (GL20.glGetProgrami(
                programId,
                GL20.GL_LINK_STATUS
        ) == GL20.GL_FALSE) {

            throw new IllegalStateException(
                    "Unable to link shader program:\n"
                    + GL20.glGetProgramInfoLog(programId)
            );
        }

        // The individual shaders are no longer needed
        // after they have been linked into the program.
        GL20.glDetachShader(programId, vertexShader);
        GL20.glDetachShader(programId, fragmentShader);

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private String readShaderFile(String path) {

        try (InputStream inputStream =
                     getClass().getClassLoader().getResourceAsStream(path)) {

            if (inputStream == null) {

                throw new IllegalStateException(
                        "Shader resource not found: " + path
                );
            }

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Unable to read shader resource: " + path,
                    e
            );
        }
    }

    private int compileShader(
            String source,
            int shaderType
    ) {

        int shaderId = GL20.glCreateShader(shaderType);

        GL20.glShaderSource(
                shaderId,
                source
        );

        GL20.glCompileShader(shaderId);

        // Check compilation.
        if (GL20.glGetShaderi(
                shaderId,
                GL20.GL_COMPILE_STATUS
        ) == GL20.GL_FALSE) {

            throw new IllegalStateException(
                    "Unable to compile shader:\n"
                    + GL20.glGetShaderInfoLog(shaderId)
            );
        }

        return shaderId;
    }

    public void bind() {

        GL20.glUseProgram(programId);
    }

    public void unbind() {

        GL20.glUseProgram(0);
    }

    public void cleanup() {

        GL20.glDeleteProgram(programId);
    }

    public void setUniform(
        String name,
        Matrix4f matrix
        ) 
        {

        try (MemoryStack stack = MemoryStack.stackPush()) {

                int location = GL20.glGetUniformLocation(
                        programId,
                        name
                );

                if (location == -1) {

                throw new IllegalStateException(
                        "Uniform not found: " + name
                );
                }

                GL20.glUniformMatrix4fv(
                        location,
                        false,
                        matrix.get(stack.mallocFloat(16))
                );
        }
        }
    
}
