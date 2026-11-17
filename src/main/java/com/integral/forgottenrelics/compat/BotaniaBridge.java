package com.integral.forgottenrelics.compat;

import com.integral.forgottenrelics.registry.FRTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.FakePlayer;
import velrondevs.botania.client.fx.SparkleParticleData;
import velrondevs.botania.client.fx.WispParticleData;

/**
 * Everything the port takes from Botania Revived (package {@code velrondevs.botania}). Botania Revived is an
 * unofficial and young port, so its API is used only through this class.
 *
 * <p>Particles: the original called {@code Botania.proxy.wispFX/sparkleFX} on the client after a packet; here the
 * server spawns Botania's own particle options with {@link ServerLevel#sendParticles}, so no custom packet is needed.
 */
public final class BotaniaBridge {
    private static final Identifier EQUIP_BAUBLE = Identifier.fromNamespaceAndPath("botania", "equip_bauble");

    private BotaniaBridge() {
    }

    /** Botania's bauble equip sound, or the generic armour sound if Botania renames it. */
    public static SoundEvent equipBaubleSound() {
        return BuiltInRegistries.SOUND_EVENT.getOptional(EQUIP_BAUBLE).orElse(SoundEvents.ARMOR_EQUIP_GENERIC.value());
    }

    /** Original {@code wispFX(world, x, y, z, r, g, b, size, motionX, motionY, motionZ, maxAgeMul)}. */
    public static void wisp(ServerLevel level, double x, double y, double z, float r, float g, float b, float size,
                            double motionX, double motionY, double motionZ, float maxAgeMul) {
        // count 0 = one particle whose (dx, dy, dz) is its velocity, scaled by speed
        level.sendParticles(WispParticleData.wisp(size, r, g, b, maxAgeMul), x, y, z, 0, motionX, motionY, motionZ, 1.0);
    }

    /** Original {@code sparkleFX(world, x, y, z, r, g, b, size, maxAge)}. */
    public static void sparkle(ServerLevel level, double x, double y, double z, float r, float g, float b, float size, int maxAge) {
        level.sendParticles(SparkleParticleData.sparkle(size, r, g, b, maxAge), x, y, z, 1, 0, 0, 0, 0);
    }

    /** Original {@code isEntityBlacklistedFromTelekinesis} and the boss checks: Thaumaturge bosses, Gaia, c:bosses. */
    public static boolean isRelicBoss(Entity entity) {
        return entity.getType().builtInRegistryHolder().is(FRTags.RELIC_BOSSES);
    }

    /** Original {@code EntityDoppleganger.isTruePlayer}: a real player, not a mod's fake player. */
    public static boolean isTruePlayer(Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player && !(entity instanceof FakePlayer);
    }

    /**
     * Client-only part of the bridge; it is loaded only when the client registers its renderers. The original
     * {@code RenderBabylonWeaponSS} was a copy of Botania's {@code RenderBabylonWeapon}, so the port draws the weapon
     * the same way Botania Revived's {@code BabylonWeaponRenderer} does: one of the King's Key weapon models and the
     * spinning summoning sigil ({@code BABYLON_ICON}, the halo shader of 1.7).
     */
    public static final class Client {
        private Client() {
        }

        /**
         * TC4 {@code FXLightningBolt} type 0 (checked in the TC4 4.2 classes: glow 0.6/0.3/0.6, core 1.0/0.6/1.0, additive)
         * drawn by Botania's fractal bolt renderer (Botania 1.7 used a copy of the same renderer). One bolt in the glow
         * colour; Botania's lightning shader draws the bright core itself.
         */
        public static void lightning(com.integral.forgottenrelics.network.LightningPayload p) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            var info = velrondevs.botania.client.fx.BoltParticleOptions.BoltRenderInfo.DEFAULT.color(new org.joml.Vector4f(0.6F, 0.3F, 0.6F, 0.9F));
            var bolt = new velrondevs.botania.client.fx.BoltParticleOptions(info, p.from(), p.to()).size(p.width()).lifespan(p.duration());
            velrondevs.botania.client.fx.BoltRenderer.INSTANCE.add(level, bolt, 0F);
        }

        public static net.minecraft.client.renderer.entity.EntityRenderer<com.integral.forgottenrelics.entity.BabylonWeaponEntity, ?> babylonWeaponRenderer(
                net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
            return new BabylonWeaponRenderer(ctx);
        }

        private static final class BabylonWeaponRenderer
                extends velrondevs.botania.client.render.entity.BotaniaEntityRenderer<com.integral.forgottenrelics.entity.BabylonWeaponEntity> {
            BabylonWeaponRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx) {
                super(ctx);
                shadowRadius = 0;
            }

            @Override
            protected void render(com.integral.forgottenrelics.entity.BabylonWeaponEntity weapon, float yaw, float partialTicks,
                                  com.mojang.blaze3d.vertex.PoseStack ms, net.minecraft.client.renderer.MultiBufferSource buffers,
                                  net.minecraft.client.renderer.SubmitNodeCollector collector, int light) {
                ms.pushPose();
                ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(weapon.rotation()));
                int live = weapon.liveTicks();
                int delay = weapon.delay();
                // chargeTicks of the original grew by one per tick during the first 15 ticks
                float charge = Math.min(10.0F, Math.max(live, Math.min(weapon.tickCount, 16)) + partialTicks);
                float chargeMul = charge / 10.0F;

                ms.pushPose();
                ms.translate(-0.75, 0.0, 1.0);
                ms.scale(1.5F, 1.5F, 1.5F);
                ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-45.0F));
                var models = velrondevs.botania.client.core.handler.MiscellaneousModels.INSTANCE.kingKeyWeaponModels;
                velrondevs.botania.client.core.helper.RenderHelper.renderModelPart(models[Math.floorMod(weapon.variety(), models.length)], ms, buffers,
                        0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                ms.popPose();

                java.util.Random rand = new java.util.Random(weapon.getUUID().getMostSignificantBits());
                ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                ms.translate(0.0F, -0.3F + rand.nextFloat() * 0.1F, 0.0F);
                float s = chargeMul;
                if (live > delay) {
                    s -= Math.min(1.0F, (live - delay + partialTicks) * 0.2F);
                }
                s *= 2.0F;
                ms.scale(s, s, s);
                ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(charge * 9.0F + (weapon.tickCount + partialTicks) * 0.5F + rand.nextFloat() * 360.0F));
                var buffer = buffers.getBuffer(velrondevs.botania.client.core.helper.RenderHelper.BABYLON_ICON);
                org.joml.Matrix4f mat = ms.last().pose();
                buffer.addVertex(mat, -1.0F, 0.0F, -1.0F).setColor(1.0F, 1.0F, 1.0F, chargeMul).setUv(0.0F, 0.0F);
                buffer.addVertex(mat, -1.0F, 0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, chargeMul).setUv(0.0F, 1.0F);
                buffer.addVertex(mat, 1.0F, 0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, chargeMul).setUv(1.0F, 1.0F);
                buffer.addVertex(mat, 1.0F, 0.0F, -1.0F).setColor(1.0F, 1.0F, 1.0F, chargeMul).setUv(1.0F, 0.0F);
                ms.popPose();
            }
        }
    }
}
