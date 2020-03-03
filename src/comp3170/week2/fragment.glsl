uniform vec3 u_colour;	/* colour as a 4D vector (r,g,b,a) */

void main() {
	/* set the fragement (pixel) colour */
    gl_FragColor = vec4(u_colour,1);
}

