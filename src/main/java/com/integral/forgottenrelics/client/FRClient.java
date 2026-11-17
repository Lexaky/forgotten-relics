package com.integral.forgottenrelics.client;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.client.renderer.OrbRenderer;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.registry.FREntities;
import com.integral.forgottenrelics.util.TooltipKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Client wiring (original ClientProxy): entity renderers and the Shift/Ctrl state for tooltips. */
@EventBusSubscriber(modid = ForgottenRelics.MODID, value = Dist.CLIENT)
public final class FRClient {
    private FRClient() {
    }

    /**
     * Renderers as in the original {@code ClientProxy}: Rageous Missile and Chaotic Orb → TC4 {@code RenderPrimalOrb},
     * Crimson Orb → {@code RenderCrimsonOrb}, Dark Matter Orb → TC4 {@code RenderEldritchOrb}, Babylon weapon →
     * {@code RenderBabylonWeaponSS}. Lunar Flare and the soul/shiny energies had no renderer: they are only the
     * particles drawn in {@code FRProjectile#clientTick}.
     */
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FREntities.RAGEOUS_MISSILE.get(), ctx -> new OrbRenderer<>(ctx, OrbRenderer.Style.PRIMAL));
        event.registerEntityRenderer(FREntities.CHAOTIC_ORB.get(), ctx -> new OrbRenderer<>(ctx, OrbRenderer.Style.PRIMAL));
        event.registerEntityRenderer(FREntities.CRIMSON_ORB.get(), ctx -> new OrbRenderer<>(ctx, OrbRenderer.Style.CRIMSON));
        event.registerEntityRenderer(FREntities.DARK_MATTER_ORB.get(), ctx -> new OrbRenderer<>(ctx, OrbRenderer.Style.ELDRITCH));
        event.registerEntityRenderer(FREntities.BABYLON_WEAPON.get(), BotaniaBridge.Client::babylonWeaponRenderer);
        event.registerEntityRenderer(FREntities.LUNAR_FLARE.get(), NoopRenderer::new);
        event.registerEntityRenderer(FREntities.SOUL_ENERGY.get(), NoopRenderer::new);
        event.registerEntityRenderer(FREntities.SHINY_ENERGY.get(), NoopRenderer::new);
    }

    @SubscribeEvent
    public static void payloads(net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent event) {
        event.register(com.integral.forgottenrelics.network.LightningPayload.TYPE, (payload, context) -> {
            ScenarioRunner.lightningPackets++;
            BotaniaBridge.Client.lightning(payload);
        });
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        TooltipKeys.shift = () -> Minecraft.getInstance().hasShiftDown();
        TooltipKeys.control = () -> Minecraft.getInstance().hasControlDown();
    }
}
