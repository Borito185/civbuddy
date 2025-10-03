#version 460

uniform mediump vec4 color;

out mediump vec4 fragmentColor;

void main() {
  fragmentColor = color;
}
