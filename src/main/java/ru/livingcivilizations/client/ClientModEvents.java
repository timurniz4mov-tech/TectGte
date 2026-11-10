package ru.livingcivilizations.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ru.livingcivilizations.LivingCivilizations;
import ru.livingcivilizations.entity.CivilizationNpc;
import ru.livingcivilizations.registry.ModEntities;

@Mod.EventBusSubscriber(modid = LivingCivilizations.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(ModEntities.TRAVELER.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.CARAVAN_MERCHANT.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.VILLAGE_MERCHANT.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.WORKER.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.GUARD.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.WARRIOR.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.LORD.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.KING.get(), PlayerLikeNpcRenderer::new);
            EntityRenderers.register(ModEntities.BANDIT.get(), PlayerLikeNpcRenderer::new);
        });
    }

    private static class PlayerLikeNpcRenderer extends MobRenderer<CivilizationNpc, PlayerModel<CivilizationNpc>> {
        private PlayerLikeNpcRenderer(EntityRendererProvider.Context context) {
            super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
            this.addLayer(new HumanoidArmorLayer<>(this,
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                    context.getModelManager()));
        }

        @Override
        public ResourceLocation getTextureLocation(CivilizationNpc entity) {
            return new ResourceLocation(LivingCivilizations.MOD_ID,
                    "textures/entity/" + entity.getRole().id() + ".png");
        }
    }
}
