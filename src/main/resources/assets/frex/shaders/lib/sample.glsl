// Used for bloom upsample, as described by Jorge Jiminez, 2014
// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
vec4 frx_sampleTent(sampler2D tex, vec2 uv, vec2 distance, int lod) {
	vec4 d = distance.xyxy * vec4(1.0, 1.0, -1.0, 0.0);

	vec4 sum = textureLod(tex, uv - d.xy, lod)
	+ textureLod(tex, uv - d.wy, lod) * 2.0
	+ textureLod(tex, uv - d.zy, lod)
	+ textureLod(tex, uv + d.zw, lod) * 2.0
	+ textureLod(tex, uv, lod) * 4.0
	+ textureLod(tex, uv + d.xw, lod) * 2.0
	+ textureLod(tex, uv + d.zy, lod)
	+ textureLod(tex, uv + d.wy, lod) * 2.0
	+ textureLod(tex, uv + d.xy, lod);

	return sum * (1.0 / 16.0);
}

// non-LOD version of above
vec4 frx_sampleTent(sampler2D tex, vec2 uv, vec2 distance) {
	vec4 d = distance.xyxy * vec4(1.0, 1.0, -1.0, 0.0);

	vec4 sum = texture(tex, uv - d.xy)
	+ texture(tex, uv - d.wy) * 2.0
	+ texture(tex, uv - d.zy)
	+ texture(tex, uv + d.zw) * 2.0
	+ texture(tex, uv) * 4.0
	+ texture(tex, uv + d.xw) * 2.0
	+ texture(tex, uv + d.zy)
	+ texture(tex, uv + d.wy) * 2.0
	+ texture(tex, uv + d.xy);

	return sum * (1.0 / 16.0);
}