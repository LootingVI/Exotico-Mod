package de.flori.exotico.mixin.client;

import de.flori.exotico.config.ExoticoConfig;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CapeFeatureRenderer.class, priority = 1500)
public class WavyCapeMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light,
            PlayerEntityRenderState state, float yaw, float pitch, CallbackInfo ci) {
        if (!ExoticoConfig.getInstance().enableWavyCapes)
            return;

        if (!state.capeVisible || state.invisible || state.skinTextures.cape() == null)
            return;


        ci.cancel();

        Identifier capeTexture = state.skinTextures.cape().id();
        RenderLayer layer = RenderLayers.armorCutoutNoCull(capeTexture);

        matrices.push();


        matrices.translate(0.0F, 0.0F, 0.15F);

        float speed = state.limbSwingAmplitude;
        float progress = state.limbSwingAnimationProgress;
        float time = (float) (System.currentTimeMillis() % 10000) / 1000.0f;


        float baseTilt = 0.15f + speed * 1.0f;
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(baseTilt));


        float sidewaysSway = (float) Math.cos(progress * 0.66625f) * speed * 0.15f;
        matrices.multiply(RotationAxis.POSITIVE_Z.rotation(sidewaysSway));

        int segments = 16;
        float totalHeight = 1.0F;
        float segmentHeight = totalHeight / segments;
        float width = 0.625F;
        float uvStepH = 16.0f / segments;


        float overlapHeight = segmentHeight * 1.05f;

        for (int i = 0; i < segments; i++) {
            float influence = (float) i / segments;


            float waveX = (float) Math.sin(time * 3.0f - i * 0.4f + progress) * (0.04f + influence * 0.08f)
                    * (1.0f + speed);
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(waveX));


            float waveZ = (float) Math.cos(time * 2.5f - i * 0.3f + progress * 0.5f) * (0.02f + influence * 0.05f)
                    * (1.0f + speed);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotation(waveZ));


            float waveY = (float) Math.sin(time * 2.0f - i * 0.2f) * 0.02f * influence;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(waveY));

            final int segmentIndex = i;

            commandQueue.submitCustom(matrices, layer, (entry, vc) -> {
                drawSegment(entry, vc, light, segmentIndex, overlapHeight, width, uvStepH);
            });


            matrices.translate(0, segmentHeight, 0);
        }

        matrices.pop();
    }

    private void drawSegment(MatrixStack.Entry entry, VertexConsumer vc, int light, int i, float h, float w,
            float uvStepH) {
        float xL = -w / 2.0f;
        float xR = w / 2.0f;
        float yT = 0;
        float yB = h;

        float uVisible1 = 1.0f / 64.0f;
        float uVisible2 = 11.0f / 64.0f;
        float uHidden1 = 12.0f / 64.0f;
        float uHidden2 = 22.0f / 64.0f;

        float v1 = (1.0f + i * uvStepH) / 32.0f;
        float v2 = (1.0f + (i + 1) * uvStepH) / 32.0f;


        vertex(vc, entry, xL, yT, 0.005f, uVisible1, v1, 0, 0, 1, light);
        vertex(vc, entry, xR, yT, 0.005f, uVisible2, v1, 0, 0, 1, light);
        vertex(vc, entry, xR, yB, 0.005f, uVisible2, v2, 0, 0, 1, light);
        vertex(vc, entry, xL, yB, 0.005f, uVisible1, v2, 0, 0, 1, light);


        vertex(vc, entry, xL, yB, 0, uHidden1, v2, 0, 0, -1, light);
        vertex(vc, entry, xR, yB, 0, uHidden2, v2, 0, 0, -1, light);
        vertex(vc, entry, xR, yT, 0, uHidden2, v1, 0, 0, -1, light);
        vertex(vc, entry, xL, yT, 0, uHidden1, v1, 0, 0, -1, light);
    }

    private void vertex(VertexConsumer vc, MatrixStack.Entry entry, float x, float y, float z, float u, float v,
            float nx, float ny, float nz, int light) {
        vc.vertex(entry.getPositionMatrix(), x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(0x0000)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}