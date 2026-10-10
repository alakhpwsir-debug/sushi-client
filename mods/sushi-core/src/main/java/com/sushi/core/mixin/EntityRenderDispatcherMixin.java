package com.sushi.core.mixin;

import com.sushi.core.SushiConfig;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Colours the hitbox of the entity under the crosshair. Vanilla draws every hitbox in white by passing
 * (1, 1, 1) as the colour; here the colour for the aimed entity is swapped for the one chosen in Sushi.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    /** The entity the crosshair is on. Set by the game each frame. */
    @Shadow
    public Entity targetedEntity;

    @ModifyArgs(
            method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitbox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/entity/Entity;FFFF)V"))
    private void sushi$aimedHitboxColor(Args args) {
        if (!SushiConfig.enabled[SushiConfig.HITBOX]) return;
        Entity entity = args.get(2);
        if (entity == null || entity != targetedEntity) return;
        int argb = SushiConfig.COLORS[SushiConfig.hitboxColor];
        // renderHitbox(matrices, consumer, entity, tickDelta, r, g, b): colour is arguments 4 to 6.
        args.set(4, ((argb >> 16) & 0xFF) / 255f);
        args.set(5, ((argb >> 8) & 0xFF) / 255f);
        args.set(6, (argb & 0xFF) / 255f);
    }
}
