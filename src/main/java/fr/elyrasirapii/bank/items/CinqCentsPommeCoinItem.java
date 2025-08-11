package fr.elyrasirapii.bank.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class CinqCentsPommeCoinItem extends Item {
    public CinqCentsPommeCoinItem(Item.Properties properties) {
        super(new Item.Properties().stacksTo(50).fireResistant().rarity(Rarity.EPIC));
    }


}
