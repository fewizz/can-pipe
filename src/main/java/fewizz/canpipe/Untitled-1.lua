dim = vec3(1/w, 1/h, 1) * 2

p0 = vec4((p+a)*dim - 1, 1)
p1 = vec4( p   *dim - 1, 1)

p1 - p0 = vec4(dim*a, 0)

P0 = I*p0
P1 = I*p1

, a -> 0

P0.w = (I*p0).w = I[3][n]*p0[n]

normalize(P1.xyz/P1.w - P0.xyz/P0.w)


P1.xyz/P1.w - P0.xyz/P0.w
-------------------------
length(P1.xyz/P1.w - P0.xyz/P0.w)


P1.xyz/P1.w - P0.xyz/P0.w
-------------------------
sqrt((P1.x/P1.w - P0.x/P0.w)^2 + ...)


P1.x/P1.w - P0.x/P0.w
-------------------------------------,   ....
sqrt((P1.x/P1.w - P0.x/P0.w)^2 + ...)


(I.x*p1)/(I.w*p1) - (I.x*p0)/(I.w*p0)
-------------------------------------,   ....
sqrt(((I.x*p1)/(I.w*p1) - (I.x*p0)/(I.w*p0))^2 + ...)


(I.x*((p+a)*dim - 1))/(I.w*p1) - (I.x*((p*dim - 1))/(I.w*p0)
-------------------------------------,   ....
sqrt(((I.x*p1)/(I.w*p1) - (I.x*p0)/(I.w*p0))^2 + ...)