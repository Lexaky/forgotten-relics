package com.integral.forgottenrelics.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * The original's orb renderers, checked against the TC4 4.2 classes:
 * <ul>
 * <li>{@code RenderCrimsonOrb} (Crimson Orb): one camera-facing 1×1 quad of the golem-orb frames of Thaumcraft's particle
 * sheet (6 frames, one per tick), white at 0.8 alpha, additive, scaled by {@code 1 + sin(t / 5) * 0.2 + 0.2};</li>
 * <li>TC4 {@code RenderPrimalOrb} (Rageous Missile, Chaotic Orb): 12 light rays seeded by the entity id, each rotated on
 * top of the previous one, white at the core and fading to a primal shard colour, growing over the first 10 ticks and
 * spinning once per 80 ticks; then a 0.5-block additive core of 13 swirl frames at 0.8 alpha;</li>
 * <li>TC4 {@code RenderEldritchOrb} (Dark Matter Orb): the same rays with the order-shard colour and a 1-block
 * translucent core of 13 dark frames.</li>
 * </ul>
 * Thaumaturge does not ship Thaumcraft's particle sheet; it has the same frames as separate strips: {@code golem_orb_blue}
 * (6 × 32 px, the sheet's bottom row), {@code flux_swirl} (the cyan swirl row) and {@code eldritch_orb}.
 */
public class OrbRenderer<T extends Entity> extends EntityRenderer<T, OrbRenderer.State> {
    private static final Identifier GOLEM_ORB = Identifier.fromNamespaceAndPath("thaumaturge", "textures/effect/golem_orb_blue.png");
    private static final Identifier SWIRL = Identifier.fromNamespaceAndPath("thaumaturge", "textures/particle/flux_swirl.png");
    private static final Identifier ELDRITCH_ORB = Identifier.fromNamespaceAndPath("thaumaturge", "textures/effect/eldritch_orb.png");
    /** TC4 {@code BlockCustomOreItem.colors}: white, then the shards air, fire, water, earth, order, entropy. */
    private static final int[] SHARD = {0xFFFFFF, 0xFFFF7E, 0xFF3C01, 0x0090FF, 0x00A000, 0xEECCFF, 0x555577};

    public enum Style {
        CRIMSON, PRIMAL, ELDRITCH
    }

    private final Style style;

    public OrbRenderer(EntityRendererProvider.Context context, Style style) {
        super(context);
        this.style = style;
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.ticks = entity.tickCount;
        state.id = entity.getId();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        int t = state.ticks;
        switch (style) {
            case CRIMSON -> {
                float bob = Mth.sin(t / 5.0F) * 0.2F + 0.2F;
                billboard(poseStack, collector, camera, RenderTypes.eyes(GOLEM_ORB), t % 6, 6, 1.0F + bob, ARGB.colorFromFloat(0.8F, 1, 1, 1));
            }
            case PRIMAL -> {
                rays(poseStack, collector, state, false);
                billboard(poseStack, collector, camera, RenderTypes.eyes(SWIRL), t % 13, 14, 0.5F, ARGB.colorFromFloat(0.8F, 1, 1, 1));
            }
            case ELDRITCH -> {
                rays(poseStack, collector, state, true);
                billboard(poseStack, collector, camera, RenderTypes.entityTranslucentEmissive(ELDRITCH_ORB), t % 13, 13, 1.0F, -1);
            }
        }
    }

    private static void billboard(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, RenderType type,
                                  int frame, int frames, float scale, int color) {
        float u0 = frame / (float) frames;
        float u1 = (frame + 1) / (float) frames;
        poseStack.pushPose();
        poseStack.mulPose(camera.orientation);
        poseStack.scale(scale, scale, scale);
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.5F, u0, 1, color);
            vertex(buffer, pose, 0.5F, -0.5F, u1, 1, color);
            vertex(buffer, pose, 0.5F, 0.5F, u1, 0, color);
            vertex(buffer, pose, -0.5F, 0.5F, u0, 0, color);
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** The 12 rays of TC4's orb renderers (triangle fans with an opaque white apex and transparent coloured tips). */
    private static void rays(PoseStack poseStack, SubmitNodeCollector collector, State state, boolean eldritch) {
        float spin = state.ticks / 80.0F * 360.0F;
        float grow = Math.min(state.ticks, 10) / 10.0F;
        poseStack.pushPose();
        Random random = new Random(state.id);
        // the original rotated the same matrix again for every ray, so each ray builds on the previous rotation
        for (int i = 0; i < 12; i++) {
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + spin));
            float length = (random.nextFloat() * 20.0F + 5.0F) / 30.0F * grow;
            float width = (random.nextFloat() * 2.0F + 1.0F) / 30.0F * grow;
            int tip = (eldritch ? SHARD[5] : SHARD[i / 2 + 1]) & 0xFFFFFF; // alpha 0
            ray(poseStack, collector, length, width, tip);
        }
        poseStack.popPose();
    }

    private static void ray(PoseStack poseStack, SubmitNodeCollector collector, float length, float width, int tip) {
        Vector3f apex = new Vector3f();
        Vector3f a = new Vector3f(-0.866F * width, length, -0.5F * width);
        Vector3f b = new Vector3f(0.866F * width, length, -0.5F * width);
        Vector3f c = new Vector3f(0.0F, length, width);
        collector.submitCustomGeometry(poseStack, RenderTypes.dragonRays(), (pose, buffer) -> {
            int white = 0xFFFFFFFF;
            buffer.addVertex(pose, apex).setColor(white);
            buffer.addVertex(pose, a).setColor(tip);
            buffer.addVertex(pose, b).setColor(tip);
            buffer.addVertex(pose, apex).setColor(white);
            buffer.addVertex(pose, b).setColor(tip);
            buffer.addVertex(pose, c).setColor(tip);
            buffer.addVertex(pose, apex).setColor(white);
            buffer.addVertex(pose, c).setColor(tip);
            buffer.addVertex(pose, a).setColor(tip);
        });
    }

    public static final class State extends EntityRenderState {
        public int ticks;
        public int id;
    }
}
