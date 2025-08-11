package fr.elyrasirapii;

import com.mojang.logging.LogUtils;
import fr.elyrasirapii.items.ElyrasirCreativeModTabs;
import fr.elyrasirapii.items.ModItems;
import fr.elyrasirapii.server.network.PacketHandler;
import fr.elyrasirapii.server.utils.MinecraftServerHolder;
import fr.elyrasirapii.server.utils.onServerTick;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;


@Mod(Elyrasirapii.MODID)
public class Elyrasirapii {

    public static final String MODID = "elyrasirapii";
    private static final Logger LOGGER = LogUtils.getLogger();



    public Elyrasirapii() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ElyrasirCreativeModTabs.register(modEventBus);
        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(onServerTick.class);
        MinecraftForge.EVENT_BUS.register(MinecraftServerHolder.class);


        MinecraftForge.EVENT_BUS.register(this);// Register ourselves for server and other game events we are interested in
        modEventBus.addListener(this::addCreative);// Register the item to a creative tab
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC); // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(() -> {
            PacketHandler.register();  // Enregistre les packets
            LOGGER.info("PacketHandler registered");
        });

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // ajout de blocks ou d'items dans un créatif tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {


    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");

    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
