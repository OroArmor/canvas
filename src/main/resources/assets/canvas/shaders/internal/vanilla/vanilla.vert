#include canvas:shaders/internal/header.glsl
#include canvas:shaders/api/context.glsl
#include canvas:shaders/internal/varying.glsl
#include canvas:shaders/internal/vertex.glsl
#include canvas:shaders/internal/flags.glsl
#include canvas:shaders/api/vertex.glsl
#include canvas:shaders/internal/diffuse.glsl

#include canvas:apitarget

/******************************************************
  canvas:shaders/internal/vanilla/vanilla.vert
******************************************************/

attribute vec4 in_color;
attribute vec2 in_uv;
attribute vec4 in_normal_ao;
attribute vec4 in_lightmap;

void main() {
	cv_VertexData data = cv_VertexData(
		gl_Vertex,
		in_uv,
		in_color,
		in_normal_ao.xyz,
		// Lightmap texture coorinates come in as 0-256.
		// Scale and offset slightly to hit center pixels
		// vanilla does this with a texture matrix
		in_lightmap.rg * 0.00390625 + 0.03125
	);

	// Adding +0.5 prevents striping or other strangeness in flag-dependent rendering
	// due to FP error on some cards/drivers.  Also made varying attribute invariant (rolls eyes at OpenGL)
	_cvv_flags = in_lightmap.b + 0.5;

#if _CV_HAS_VERTEX_START
	cv_startVertex(data);
#endif

	data.spriteUV = _cv_textureCoord(data.spriteUV, 0);

	vec4 viewCoord = gl_ModelViewMatrix * data.vertex;
	gl_ClipVertex = viewCoord;
	gl_FogFragCoord = length(viewCoord.xyz);

	data.vertex = gl_ModelViewProjectionMatrix * data.vertex;

	gl_Position = data.vertex;

#if _CV_HAS_VERTEX_END
	cv_endVertex(data);
#endif

	_cvv_texcoord = data.spriteUV;
	_cvv_color = data.color;
	_cvv_normal = data.normal;

#if CONTEXT_IS_BLOCK
	_cvv_ao = (in_normal_ao.w + 1.0) * 0.5;
#endif

#if DIFFUSE_SHADING_MODE != DIFFUSE_MODE_NONE
	_cvv_diffuse = _cv_diffuse(_cv_diffuseNormal(viewCoord, data.normal));
#endif

#if !CONTEXT_IS_GUI
	_cvv_lightcoord = data.light;
#endif

}
