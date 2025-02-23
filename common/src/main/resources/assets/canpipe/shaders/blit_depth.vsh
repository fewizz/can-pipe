#version 150

in vec4 Position;

void main(){
    vec2[4] positions = vec2[4](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );
    gl_Position = vec4(Position.xy, 0.0, 1.0);
}