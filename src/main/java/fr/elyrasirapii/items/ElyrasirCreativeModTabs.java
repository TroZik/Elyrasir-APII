package fr.elyrasirapii.items;

import fr.elyrasirapii.Elyrasirapii;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ElyrasirCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MOD_TAB =
             DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Elyrasirapii.MODID);

    public static final RegistryObject<CreativeModeTab> ELYRASIR_TAB = CREATIVE_MOD_TAB.register("elyrasir_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.UNPOMMECOINITEM.get()))
                    .title(Component.translatable("creativetab.elyrasirapii"))
                    .displayItems((pParameters, pOutput) ->{
                        pOutput.accept(ModItems.ARCHITECT_STICK.get());

                        pOutput.accept(ModItems.UNPOMMECOINITEM.get());
                        pOutput.accept(ModItems.CINQPOMMECOINITEM.get());
                        pOutput.accept(ModItems.DIXPOMMECOINITEM.get());
                        pOutput.accept(ModItems.VINGTPOMMECOINITEM.get());
                        pOutput.accept(ModItems.CINQUANTEPOMMECOINITEM.get());
                        pOutput.accept(ModItems.CENTPOMMECOINITEM.get());
                        pOutput.accept(ModItems.DEUXCENTSPOMMECOINITEM.get());
                        pOutput.accept(ModItems.CINQCENTSPOMMECOINITEM.get());


                    })
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MOD_TAB.register(eventBus);
    }
}
