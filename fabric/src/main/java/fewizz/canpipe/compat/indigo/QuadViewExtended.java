package fewizz.canpipe.compat.indigo;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;

public interface QuadViewExtended extends QuadView {

    // int 0:
    //      0..4 - sprite index
    // int 1:
    //      4..6 - material index
    //      6..8 - padding
    // int 2:
    //      8..9   - ao 0
    //      9..10  - ao 1
    //      10..11 - ao 2
    //      11..12 - ao 3
    public static final int CANPIPE_DATA_STRIDE_INTS = 3;

    int[] canpipe_getQuadData();

    void canpipe_setQuadData(int[] data);

}
