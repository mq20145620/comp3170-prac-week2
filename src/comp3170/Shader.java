package comp3170;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix2f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;

public class Shader {

	public int program;
	private Map<String, Integer> attributes;
	private Map<String, Integer> attributeTypes;
	private Map<String, Integer> uniforms;
	private Map<String, Integer> uniformTypes;

	/**
	 * Compile and link a vertex and fragment shader
	 * 
	 * @param vertexShaderFile
	 * @param fragmentShaderFile
	 * @throws IOException
	 * @throws GLException
	 */

	public Shader(File vertexShaderFile, File fragmentShaderFile) throws IOException, GLException {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		// compile the shaders

		int vertexShader = compileShader(GL4.GL_VERTEX_SHADER, vertexShaderFile);
		int fragmentShader = compileShader(GL4.GL_FRAGMENT_SHADER, fragmentShaderFile);

		// link the shaders

		this.program = gl.glCreateProgram();
		gl.glAttachShader(this.program, vertexShader);
		gl.glAttachShader(this.program, fragmentShader);
		gl.glLinkProgram(program);
		GLException.checkGLErrors();

		// check for linker errors

		int[] linked = new int[1];
		gl.glGetProgramiv(this.program, GL4.GL_LINK_STATUS, linked, 0);
		if (linked[0] != 1) {
			int[] maxlen = new int[1];
			int[] len = new int[1];
			byte[] log = null;
			String logString = "";

			// determine length of the program compilation log
			gl.glGetProgramiv(this.program, GL4.GL_INFO_LOG_LENGTH, maxlen, 0);

			if (maxlen[0] > 0) {
				log = new byte[maxlen[0]];

				gl.glGetProgramInfoLog(this.program, maxlen[0], len, 0, log, 0);
				logString = new String(log);
			}

			String message = String.format("Link failed:\n", logString);
			throw new GLException(message);
		}

		// record attribute and uniforms

		recordAttributes();
		recordUniforms();
	}

	/**
	 * Get the handle for an attribute
	 * 
	 * @param name
	 * @return
	 */

	public int getAttribute(String name) {
		if (!this.attributes.containsKey(name)) {
			throw new IllegalArgumentException(String.format("Unknown attribute: '%s'", name));
		}

		return this.attributes.get(name);
	}

	/**
	 * Get the handle for a uniform
	 * 
	 * @param name
	 * @return
	 */

	public int getUniform(String name) {
		if (!this.uniforms.containsKey(name)) {
			throw new IllegalArgumentException(String.format("Unknown uniform: '%s'", name));
		}

		return this.uniforms.get(name);
	}

	/**
	 * Enable the shader
	 */

	public void enable() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(this.program);
	}

	/**
	 * Create a new VBO (vertex buffer object) in graphics memory and copy data into
	 * it
	 * 
	 * @param gl   The GL context
	 * @param data The data as an array of floats
	 * @return
	 */
	public int createBuffer(float[] data) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] buffers = new int[1];
		gl.glGenBuffers(buffers.length, buffers, 0);

		FloatBuffer buffer = Buffers.newDirectFloatBuffer(data);
		gl.glBindBuffer(GL_ARRAY_BUFFER, buffers[0]);
		gl.glBufferData(GL_ARRAY_BUFFER, data.length * Buffers.SIZEOF_FLOAT, buffer, GL_STATIC_DRAW);

		return buffers[0];
	}

	/**
	 * Connect a buffer to a shader attribute
	 * 
	 * @param attributeName The name of the shader attribute
	 * @param buffer        The buffer
	 * @param size          The number of values per item (i.e. 2 for a vec2, 3 for
	 *                      a vec3 etc)
	 * @param type          The type of the data in the buffer
	 * @param gl            The GL context
	 */
	public void setAttribute(String attributeName, int buffer, int size, int type) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glBindBuffer(GL_ARRAY_BUFFER, buffer);
		int attribute = getAttribute(attributeName);
		gl.glVertexAttribPointer(attribute, size, type, false, 0, 0);
		gl.glEnableVertexAttribArray(attribute);
	}

	/**
	 * Set the value of a uniform
	 * 
	 * @param uniformName
	 * @param value
	 */
	public void setUniform(String uniformName, float value) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int uniform = getUniform(uniformName);
		int type = uniformTypes.get(uniformName);

		if (type != GL.GL_FLOAT) {		
			throw new IllegalArgumentException(
					String.format("Expected %s got float", typeName(type)));
		}

		gl.glUniform1f(uniform, value);
	}
	

	/**
	 * Set the value of a uniform
	 * 
	 * @param uniformName
	 * @param value
	 */
	public void setUniform(String uniformName, float[] value) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int uniform = getUniform(uniformName);
		int type = uniformTypes.get(uniformName);

		int expectedArgs = -1;
		switch (type) {
		case GL.GL_FLOAT:
			expectedArgs = 1;
			break;
		case GL4.GL_FLOAT_VEC2:
			expectedArgs = 2;
			break;
		case GL4.GL_FLOAT_VEC3:
			expectedArgs = 3;
			break;
		case GL4.GL_FLOAT_VEC4:
			expectedArgs = 4;
			break;
		}

		if (value.length != expectedArgs) {
			throw new IllegalArgumentException(
					String.format("Expected %s got float[%d]", typeName(type), value.length));
		}

		switch (type) {
		case GL.GL_FLOAT:
			gl.glUniform1f(uniform, value[0]);
			break;
		case GL4.GL_FLOAT_VEC2:
			gl.glUniform2f(uniform, value[0], value[1]);
			break;
		case GL4.GL_FLOAT_VEC3:
			gl.glUniform3f(uniform, value[0], value[1], value[2]);
			break;
		case GL4.GL_FLOAT_VEC4:
			gl.glUniform4f(uniform, value[0], value[1], value[2], value[3]);
			break;
		}

	}

	private String typeName(int type) {
		switch (type) {
		case GL4.GL_FLOAT:
			return "float";

		case GL4.GL_FLOAT_VEC2:
			return "vec2";

		case GL4.GL_FLOAT_VEC3:
			return "vec3";

		case GL4.GL_FLOAT_VEC4:
			return "vec4";

		case GL4.GL_DOUBLE:
			return "double";

		case GL4.GL_DOUBLE_VEC2:
			return "dvec2";

		case GL4.GL_DOUBLE_VEC3:
			return "dvec3";

		case GL4.GL_DOUBLE_VEC4:
			return "dvec4";

		case GL4.GL_INT:
			return "int";

		case GL4.GL_INT_VEC2:
			return "ivec2";

		case GL4.GL_INT_VEC3:
			return "ivec3";

		case GL4.GL_INT_VEC4:
			return "ivec4";

		case GL4.GL_UNSIGNED_INT:
			return "unsigned int";

		case GL4.GL_UNSIGNED_INT_VEC2:
			return "uvec2";

		case GL4.GL_UNSIGNED_INT_VEC3:
			return "uvec3";

		case GL4.GL_UNSIGNED_INT_VEC4:
			return "uvec4";

		case GL4.GL_BOOL:
			return "bool";

		case GL4.GL_BOOL_VEC2:
			return "bvec2";

		case GL4.GL_BOOL_VEC3:
			return "bvec3";

		case GL4.GL_BOOL_VEC4:
			return "bvec4";

		case GL4.GL_FLOAT_MAT2:
			return "mat2";

		case GL4.GL_FLOAT_MAT3:
			return "mat3";

		case GL4.GL_FLOAT_MAT4:
			return "mat4";

		case GL4.GL_FLOAT_MAT2x3:
			return "mat2x3";

		case GL4.GL_FLOAT_MAT2x4:
			return "mat2x4";

		case GL4.GL_FLOAT_MAT3x2:
			return "mat3x2";

		case GL4.GL_FLOAT_MAT3x4:
			return "mat3x4";

		case GL4.GL_FLOAT_MAT4x2:
			return "mat4x2";

		case GL4.GL_FLOAT_MAT4x3:
			return "mat4x3";

		case GL4.GL_DOUBLE_MAT2:
			return "dmat2";

		case GL4.GL_DOUBLE_MAT3:
			return "dmat3";

		case GL4.GL_DOUBLE_MAT4:
			return "dmat4";

		case GL4.GL_DOUBLE_MAT2x3:
			return "dmat2x3";

		case GL4.GL_DOUBLE_MAT2x4:
			return "dmat2x4";

		case GL4.GL_DOUBLE_MAT3x2:
			return "dmat3x2";

		case GL4.GL_DOUBLE_MAT3x4:
			return "dmat3x4";

		case GL4.GL_DOUBLE_MAT4x2:
			return "dmat4x2";

		case GL4.GL_DOUBLE_MAT4x3:
			return "dmat4x3";

		case GL4.GL_SAMPLER_1D:
			return "sampler1D";

		case GL4.GL_SAMPLER_2D:
			return "sampler2D";

		case GL4.GL_SAMPLER_3D:
			return "sampler3D";

		case GL4.GL_SAMPLER_CUBE:
			return "samplerCube";

		case GL4.GL_SAMPLER_1D_SHADOW:
			return "sampler1DShadow";

		case GL4.GL_SAMPLER_2D_SHADOW:
			return "sampler2DShadow";

		case GL4.GL_SAMPLER_1D_ARRAY:
			return "sampler1DArray";

		case GL4.GL_SAMPLER_2D_ARRAY:
			return "sampler2DArray";

		case GL4.GL_SAMPLER_1D_ARRAY_SHADOW:
			return "sampler1DArrayShadow";

		case GL4.GL_SAMPLER_2D_ARRAY_SHADOW:
			return "sampler2DArrayShadow";

		case GL4.GL_SAMPLER_2D_MULTISAMPLE:
			return "sampler2DMS";

		case GL4.GL_SAMPLER_2D_MULTISAMPLE_ARRAY:
			return "sampler2DMSArray";

		case GL4.GL_SAMPLER_CUBE_SHADOW:
			return "samplerCubeShadow";

		case GL4.GL_SAMPLER_BUFFER:
			return "samplerBuffer";

		case GL4.GL_SAMPLER_2D_RECT:
			return "sampler2DRect";

		case GL4.GL_SAMPLER_2D_RECT_SHADOW:
			return "sampler2DRectShadow";

		case GL4.GL_INT_SAMPLER_1D:
			return "isampler1D";

		case GL4.GL_INT_SAMPLER_2D:
			return "isampler2D";

		case GL4.GL_INT_SAMPLER_3D:
			return "isampler3D";

		case GL4.GL_INT_SAMPLER_CUBE:
			return "isamplerCube";

		case GL4.GL_INT_SAMPLER_1D_ARRAY:
			return "isampler1DArray";

		case GL4.GL_INT_SAMPLER_2D_ARRAY:
			return "isampler2DArray";

		case GL4.GL_INT_SAMPLER_2D_MULTISAMPLE:
			return "isampler2DMS";

		case GL4.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY:
			return "isampler2DMSArray";

		case GL4.GL_INT_SAMPLER_BUFFER:
			return "isamplerBuffer";

		case GL4.GL_INT_SAMPLER_2D_RECT:
			return "isampler2DRect";

		case GL4.GL_UNSIGNED_INT_SAMPLER_1D:
			return "usampler1D";

		case GL4.GL_UNSIGNED_INT_SAMPLER_2D:
			return "usampler2D";

		case GL4.GL_UNSIGNED_INT_SAMPLER_3D:
			return "usampler3D";

		case GL4.GL_UNSIGNED_INT_SAMPLER_CUBE:
			return "usamplerCube";

		case GL4.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY:
			return "usampler2DArray";

		case GL4.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY:
			return "usampler2DArray";

		case GL4.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE:
			return "usampler2DMS";

		case GL4.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY:
			return "usampler2DMSArray";

		case GL4.GL_UNSIGNED_INT_SAMPLER_BUFFER:
			return "usamplerBuffer";

		case GL4.GL_UNSIGNED_INT_SAMPLER_2D_RECT:
			return "usampler2DRect";

		case GL4.GL_IMAGE_1D:
			return "image1D";

		case GL4.GL_IMAGE_2D:
			return "image2D";

		case GL4.GL_IMAGE_3D:
			return "image3D";

		case GL4.GL_IMAGE_2D_RECT:
			return "image2DRect";

		case GL4.GL_IMAGE_CUBE:
			return "imageCube";

		case GL4.GL_IMAGE_BUFFER:
			return "imageBuffer";

		case GL4.GL_IMAGE_1D_ARRAY:
			return "image1DArray";

		case GL4.GL_IMAGE_2D_ARRAY:
			return "image2DArray";

		case GL4.GL_IMAGE_2D_MULTISAMPLE:
			return "image2DMS";

		case GL4.GL_IMAGE_2D_MULTISAMPLE_ARRAY:
			return "image2DMSArray";

		case GL4.GL_INT_IMAGE_1D:
			return "iimage1D";

		case GL4.GL_INT_IMAGE_2D:
			return "iimage2D";

		case GL4.GL_INT_IMAGE_3D:
			return "iimage3D";

		case GL4.GL_INT_IMAGE_2D_RECT:
			return "iimage2DRect";

		case GL4.GL_INT_IMAGE_CUBE:
			return "iimageCube";

		case GL4.GL_INT_IMAGE_BUFFER:
			return "iimageBuffer";

		case GL4.GL_INT_IMAGE_1D_ARRAY:
			return "iimage1DArray";

		case GL4.GL_INT_IMAGE_2D_ARRAY:
			return "iimage2DArray";

		case GL4.GL_INT_IMAGE_2D_MULTISAMPLE:
			return "iimage2DMS";

		case GL4.GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY:
			return "iimage2DMSArray";

		case GL4.GL_UNSIGNED_INT_IMAGE_1D:
			return "uimage1D";

		case GL4.GL_UNSIGNED_INT_IMAGE_2D:
			return "uimage2D";

		case GL4.GL_UNSIGNED_INT_IMAGE_3D:
			return "uimage3D";

		case GL4.GL_UNSIGNED_INT_IMAGE_2D_RECT:
			return "uimage2DRect";

		case GL4.GL_UNSIGNED_INT_IMAGE_CUBE:
			return "uimageCube";

		case GL4.GL_UNSIGNED_INT_IMAGE_BUFFER:
			return "uimageBuffer";

		case GL4.GL_UNSIGNED_INT_IMAGE_1D_ARRAY:
			return "uimage1DArray";

		case GL4.GL_UNSIGNED_INT_IMAGE_2D_ARRAY:
			return "uimage2DArray";

		case GL4.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE:
			return "uimage2DMS";

		case GL4.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY:
			return "uimage2DMSArray";

		case GL4.GL_UNSIGNED_INT_ATOMIC_COUNTER:
			return "atomic_uint";
		}
		throw new IllegalArgumentException("Unknown GL type: " + type);
	}

	/**
	 * Establish the mapping from attribute names to IDs
	 */

	private void recordAttributes() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		this.attributes = new HashMap<String, Integer>();
		this.attributeTypes = new HashMap<String, Integer>();

		int[] iBuff = new int[1];
		gl.glGetProgramiv(this.program, GL4.GL_ACTIVE_ATTRIBUTES, iBuff, 0);
		int activeAttributes = iBuff[0];

		gl.glGetProgramiv(this.program, GL4.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, iBuff, 0);
		int maxNameSize = iBuff[0];

		byte[] nameBuffer = new byte[maxNameSize];

		int[] sizeBuffer = new int[1];
		int[] typeBuffer = new int[1];
		int[] nameLenBuffer = new int[1];
		for (int i = 0; i < activeAttributes; ++i) {
			gl.glGetActiveAttrib(this.program, i, maxNameSize, nameLenBuffer, 0, sizeBuffer, 0, typeBuffer, 0,
					nameBuffer, 0);
			String name = new String(nameBuffer, 0, nameLenBuffer[0]);
			this.attributes.put(name, i);
			this.attributeTypes.put(name, typeBuffer[0]);
		}
	}

	/**
	 * Establish the mapping from uniform names to IDs
	 */

	private void recordUniforms() {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		this.uniforms = new HashMap<String, Integer>();
		this.uniformTypes = new HashMap<String, Integer>();

		int[] iBuff = new int[1];
		gl.glGetProgramiv(this.program, GL4.GL_ACTIVE_UNIFORMS, iBuff, 0);
		int activeUniforms = iBuff[0];

		gl.glGetProgramiv(this.program, GL4.GL_ACTIVE_UNIFORM_MAX_LENGTH, iBuff, 0);
		int maxNameSize = iBuff[0];

		byte[] nameBuffer = new byte[maxNameSize];

		int[] sizeBuffer = new int[1];
		int[] typeBuffer = new int[1];
		int[] nameLenBuffer = new int[1];
		for (int i = 0; i < activeUniforms; ++i) {
			gl.glGetActiveUniform(this.program, i, maxNameSize, nameLenBuffer, 0, sizeBuffer, 0, typeBuffer, 0,
					nameBuffer, 0);
			String name = new String(nameBuffer, 0, nameLenBuffer[0]);
			this.uniforms.put(name, i);
			this.uniformTypes.put(name, typeBuffer[0]);
		}
	}

	/**
	 * Read source code from a shader file.
	 * 
	 * @param shaderFile
	 * @return
	 * @throws IOException
	 */
	public static String[] readSource(File shaderFile) throws IOException {
		ArrayList<String> source = new ArrayList<String>();
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader(shaderFile));

			for (String line = in.readLine(); line != null; line = in.readLine()) {
				source.add(line + "\n");
			}

		} catch (IOException e) {
			throw e;
		} finally {
			if (in != null) {
				in.close();
			}
		}

		String[] lines = new String[source.size()];
		return source.toArray(lines);
	}

	/**
	 * Compile a shader
	 * 
	 * @param type
	 * @param sourceFile
	 * @return
	 * @throws GLException
	 * @throws IOException
	 */

	public static int compileShader(int type, File sourceFile) throws GLException, IOException {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		String[] source = readSource(sourceFile);

		int shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, source.length, source, null, 0);
		gl.glCompileShader(shader);
		GLException.checkGLErrors();

		// check compilation

		int[] compiled = new int[1];
		gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compiled, 0);
		String logString = "";

		if (compiled[0] != 1) {

			int[] maxlen = new int[1];
			gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, maxlen, 0);

			if (maxlen[0] > 0) {
				int[] len = new int[1];
				byte[] log = null;

				log = new byte[maxlen[0]];
				gl.glGetShaderInfoLog(shader, maxlen[0], len, 0, log, 0);
				logString = new String(log);
			}

			String message = String.format("%s: compilation error\n%s", sourceFile.getName(), logString);
			throw new GLException(message);
		}

		return shader;
	}

	/**
	 * Turn a shader type constant into a descriptive string.
	 * 
	 * @param type
	 * @return
	 */
	public static String shaderType(int type) {
		switch (type) {
		case GL4.GL_VERTEX_SHADER:
			return "Vertex shader";
		case GL4.GL_FRAGMENT_SHADER:
			return "Fragment shader";
		}
		return "Unknown shader";
	}

}
