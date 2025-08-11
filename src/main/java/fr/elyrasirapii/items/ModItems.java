package fr.elyrasirapii.items;

import fr.elyrasirapii.parcels.item.ArchitectStickItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems { public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "elyrasirapii");

    //ajout de l'architectStick
    public static final RegistryObject<Item> ARCHITECT_STICK =
            ITEMS.register("architect_stick", () -> new ArchitectStickItem(new Item.Properties()));

    //ajout du RoadStick
   /* public static final RegistryObject<Item> ROAD_STICK =
            ITEMS.register("road_stick", () -> new RoadStickItem(new Item.Properties()));*/



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
