package fewizz.canpipe;

import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;

public class IndigoCompat {

    public static void onAbstractBlockRenderContextRenderQuadEnd(
        MutableQuadViewImpl quad, AoCalculator ao
    ) {
    }

}
