#version 460

layout(std140) uniform u_projection {
  mat4 projection;
};

layout (location = 0) in vec3 i_pos;

void main() {
  gl_Position = projection * vec4(i_pos, 1.0f);
}
