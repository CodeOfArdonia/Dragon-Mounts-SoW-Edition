package com.iafenvoy.dragonmounts.dragon;

import com.iafenvoy.dragonmounts.DMConstants;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.registry.DMEntities;
import com.iafenvoy.dragonmounts.registry.DMItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.Optional;

public class DragonSpawnEgg extends SpawnEggItem {
    private static final String DATA_TAG = "ItemData";
    private static final String DATA_ITEM_NAME = "ItemName";
    private static final String DATA_PRIM_COLOR = "PrimaryColor";
    private static final String DATA_SEC_COLOR = "SecondaryColor";

    public DragonSpawnEgg() {
        super(DMEntities.DRAGON, 0, 0, new Item.Settings());
    }

    public static ItemStack create(DragonBreed breed, DynamicRegistryManager reg) {
        Identifier id = breed.id(reg);
        NbtCompound root = new NbtCompound();
        // entity tag
        NbtCompound entityTag = new NbtCompound();
        entityTag.putString(TameableDragon.NBT_BREED, id.toString());
        root.put(EntityType.ENTITY_TAG_KEY, entityTag);
        // name & colors
        // storing these in the stack nbt is more performant than getting the breed everytime
        NbtCompound itemDataTag = new NbtCompound();
        itemDataTag.putString(DATA_ITEM_NAME, String.join(".", DMItems.SPAWN_EGG.getTranslationKey(), id.getNamespace(), id.getPath()));
        itemDataTag.putInt(DATA_PRIM_COLOR, breed.primaryColor());
        itemDataTag.putInt(DATA_SEC_COLOR, breed.secondaryColor());
        root.put(DATA_TAG, itemDataTag);
        ItemStack stack = new ItemStack(DMItems.SPAWN_EGG);
        stack.setNbt(root);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        preconditionSpawnEgg(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        NbtCompound tag = stack.getSubNbt(DATA_TAG);
        if (tag != null && tag.contains(DATA_ITEM_NAME))
            return Text.translatable(tag.getString(DATA_ITEM_NAME));
        return super.getName(stack);
    }

    @Override
    public Optional<MobEntity> spawnBaby(PlayerEntity pPlayer, MobEntity pMob, EntityType<? extends MobEntity> pEntityType, ServerWorld pServerLevel, Vec3d pPos, ItemStack pStack) {
        NbtCompound entityTag = pStack.getSubNbt(EntityType.ENTITY_TAG_KEY);
        if (entityTag != null) {
            String breedID = entityTag.getString(TameableDragon.NBT_BREED);
            if (!breedID.isEmpty() && pMob instanceof TameableDragon dragon && dragon.getBreed() != BreedRegistry.get(breedID, pServerLevel.getRegistryManager()))
                return Optional.empty();
        }
        return super.spawnBaby(pPlayer, pMob, pEntityType, pServerLevel, pPos, pStack);
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        NbtCompound tag = stack.getSubNbt(DATA_TAG);
        if (tag != null)
            return tintIndex == 0 ? tag.getInt(DATA_PRIM_COLOR) : tag.getInt(DATA_SEC_COLOR);
        return 0xffffff;
    }

    @SuppressWarnings("ConstantConditions")
    private static void preconditionSpawnEgg(ItemStack stack) {
        if (DMConstants.server == null) return;
        NbtCompound root = stack.getOrCreateNbt();
        NbtCompound blockEntityData = stack.getOrCreateSubNbt(EntityType.ENTITY_TAG_KEY);
        String breedId = blockEntityData.getString(TameableDragon.NBT_BREED);
        DynamicRegistryManager.Immutable regAcc = DMConstants.server.getRegistryManager();
        Registry<DragonBreed> reg = BreedRegistry.registry(regAcc);
        if (breedId.isEmpty() || !reg.containsId(new Identifier(breedId))) {// this item doesn't contain a breed yet?
            // assign one ourselves then.
            RegistryEntry.Reference<DragonBreed> breed = reg.getRandom(Random.create()).orElseThrow();
            ItemStack updated = create(breed.value(), regAcc);
            root.copyFrom(updated.getNbt());
        }
    }
}
