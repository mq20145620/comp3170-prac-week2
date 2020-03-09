#version 410

uniform vec3 u_colour;	/* colour as a 3D vector (r,g,b) */

layout(location = 0) out vec4 color;

void main() {
	/* set the fragement (pixel) colour */
    color = vec4(u_colour,1);
}

