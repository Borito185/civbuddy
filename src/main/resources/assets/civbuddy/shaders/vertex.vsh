#version 150

#line 0 1
/*#version 150*/

layout(std140) uniform DynamicTransforms {
mat4 ModelViewMat;
vec4 ColorModulator;
vec3 ModelOffset;
mat4 TextureMat;
float LineWidth;
};
#line 0 2
/*#version 150*/

layout(std140) uniform Projection {
mat4 ProjMat;
};

vec4 projection_from_position(vec4 position) {
vec4 projection = position * 0.5;
projection.xy = vec2(projection.x + projection.w, projection.y + projection.w);
projection.zw = position.zw;
return projection;
}
#line 4 0

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
gl_Position.w += 0.001f;
vertexColor = Color;
}
