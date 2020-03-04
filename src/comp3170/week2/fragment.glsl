uniform vec3 u_colour;	/* colour as a 3D vector (r,g,b) */

void main() {
	/* set the fragement (pixel) colour */
    gl_FragColor = vec4(u_colour,1);
}

