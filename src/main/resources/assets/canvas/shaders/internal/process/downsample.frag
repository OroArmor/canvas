#include canvas:shaders/internal/process/header.glsl
#include canvas:shaders/lib/sample.glsl

/******************************************************
  canvas:shaders/internal/process/downsample.frag
******************************************************/
uniform sampler2D _cvu_input;
uniform ivec2 _cvu_size;
uniform vec2 _cvu_distance;
uniform int _cvu_lod;

varying vec2 _cvv_texcoord;

void main() {
	gl_FragData[0] = cv_sample13(_cvu_input, _cvv_texcoord, _cvu_distance / _cvu_size, _cvu_lod);
}
