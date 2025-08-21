#version 460

layout(std140) uniform u_projection {
  mat4 projection;
};

layout (location = 0) in vec3 i_pos;

out vec4 vertexColor;

void main() {
  gl_Position = projection * vec4(i_pos, 1.0f);
  vertexColor = vec4(0.0 / 255.0f, 0.0 / 255.0f, 0.0 / 255.0f, 255.0 / 255.0f);
}
