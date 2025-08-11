package fr.elyrasirapii.client.init;

import fr.elyrasirapii.parcels.item.ArchitectStickItem;
import net.minecraft.world.item.Item;

public class ModItemClientInit {
    public static Item createArchitectStick() {
        return new ArchitectStickItem(new Item.Properties().stacksTo(1));
    }
}
