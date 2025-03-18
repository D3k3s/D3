package meteordevelopment.meteorclient.utils.render.postprocess;

import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.client.render.VertexConsumerProvider;

import static meteordevelopment.meteorclient.D3.mc;

public class PostProcessShaders {
    public static EntityShader ENTITY_OUTLINE;
    public static PostProcessShader STORAGE_OUTLINE;

    public static boolean rendering;

    private PostProcessShaders() {
    }

    @PreInit
    public static void init() {
        ENTITY_OUTLINE = new EntityOutlineShader();
        STORAGE_OUTLINE = new StorageOutlineShader();
    }

    public static void beginRender() {
        ENTITY_OUTLINE.beginRender();
        STORAGE_OUTLINE.beginRender();
    }

    public static void endRender() {
        ENTITY_OUTLINE.endRender();
    }

    public static void onResized(int width, int height) {
        if (mc == null) return;

        ENTITY_OUTLINE.onResized(width, height);
        STORAGE_OUTLINE.onResized(width, height);
    }

    public static boolean isCustom(VertexConsumerProvider vcp) {
        return vcp == ENTITY_OUTLINE.vertexConsumerProvider;
    }
}
