#version 460

in mediump vec4 vertexColor;

out mediump vec4 fragmentColor;

void main() {
  fragmentColor = vertexColor;
}
