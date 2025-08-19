package fr.elyrasirapii.items;

import fr.elyrasirapii.bank.items.*;
import fr.elyrasirapii.parcels.item.ArchitectStickItem;
import fr.elyrasirapii.road.item.RoadStickItem;
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
    public static final RegistryObject<Item> ROAD_STICK =
            ITEMS.register("road_stick", () -> new RoadStickItem(new Item.Properties()));

   //ajout des coupures de la monnaie

    public static final RegistryObject<Item> UNPOMMECOINITEM =
            ITEMS.register("1_pc", () -> new UnPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> CINQPOMMECOINITEM =
            ITEMS.register("5_pc", () -> new CinqPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> DIXPOMMECOINITEM =
            ITEMS.register("10_pc", () -> new DixPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> VINGTPOMMECOINITEM =
            ITEMS.register("20_pc", () -> new VingtPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> CINQUANTEPOMMECOINITEM =
            ITEMS.register("50_pc", () -> new CinquantePommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> CENTPOMMECOINITEM =
            ITEMS.register("100_pc", () -> new CentPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> DEUXCENTSPOMMECOINITEM =
            ITEMS.register("200_pc", () -> new DeuxCentsPommeCoinItem(new Item.Properties()));

    public static final RegistryObject<Item> CINQCENTSPOMMECOINITEM =
            ITEMS.register("500_pc", () -> new CinqCentsPommeCoinItem(new Item.Properties()));






    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
