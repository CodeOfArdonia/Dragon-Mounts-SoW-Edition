package com.iafenvoy.dragonmounts.dragon.egg;

import com.google.common.base.Suppliers;
import com.iafenvoy.dragonmounts.Static;
import com.iafenvoy.dragonmounts.config.DMCommonConfig;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.habitats.Habitat;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class HatchableEggBlockEntity extends BlockEntity implements Nameable {
    public static final int MIN_HABITAT_POINTS = 2;
    private Supplier<DragonBreed> breed = () -> null;
    private Text customName;
    private int hatchTick;

    public HatchableEggBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(DMBlocks.EGG_BLOCK_ENTITY, pPos, pBlockState);
    }

    @Override
    @SuppressWarnings("ConstantConditions") // level exists if we have a breed
    protected void writeNbt(NbtCompound tag) {
        super.writeNbt(tag);
        if (this.hasBreed())
            tag.putString(HatchableEggBlock.NBT_BREED, this.getBreed().id(this.getWorld().getRegistryManager()).toString());
        if (this.getCustomName() != null)
            tag.putString(HatchableEggBlock.NBT_NAME, Text.Serializer.toJson(this.customName));
        tag.putInt(HatchableEggBlock.NBT_HATCH_STAGE, this.hatchTick);
    }

    /*
     * sometimes, this is called before the BE is given a Level to work with.
     */
    @Override
    @SuppressWarnings("ConstantConditions") // level exists at memoize
    public void readNbt(NbtCompound pTag) {
        super.readNbt(pTag);
        this.setBreed(Suppliers.memoize(() -> BreedRegistry.get(pTag.getString(HatchableEggBlock.NBT_BREED), this.getWorld().getRegistryManager())));
        String name = pTag.getString(HatchableEggBlock.NBT_NAME);
        if (!name.isBlank()) this.setCustomName(Text.Serializer.fromJson(name));
        if (this.getWorld() != null && this.getWorld().isClient) // client needs to be aware of new changes
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
        this.hatchTick = pTag.getInt(HatchableEggBlock.NBT_HATCH_STAGE);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = super.toInitialChunkDataNbt();
        this.writeNbt(tag);
        return tag;
    }

    /**
     * This should be guarded with {@link HatchableEggBlockEntity#hasBreed()}!!
     * There is no way proper way to resolve a random breed in the mess that is
     * BlockEntity's...
     * <br>
     * When this is called, and breed.get() is null, a random breed is assigned instead.
     */
    @Nullable
    public DragonBreed getBreed() {
        return this.breed.get();
    }

    public void setBreed(Supplier<DragonBreed> breed) {
        this.breed = breed;
        this.markDirty();
        if (this.world instanceof ServerWorld serverWorld && this.breed.get() != null) {
            PacketByteBuf buf = PacketByteBufs.create().writeBlockPos(this.pos).writeString(this.breed.get().id(serverWorld.getRegistryManager()).toString());
            for (ServerPlayerEntity player : serverWorld.getServer().getPlayerManager().getPlayerList())
                ServerPlayNetworking.send(player, Static.DRAGON_EGG_TYPE_SYNC, buf);
        }
    }

    public boolean hasBreed() {
        return this.breed.get() != null;
    }

    @Override
    public Text getCustomName() {
        return this.customName;
    }

    @Override
    @SuppressWarnings("ConstantConditions") // level exists at this point
    public Text getName() {
        return this.customName != null ? this.customName : Text.translatable(DMBlocks.EGG_BLOCK.getTranslationKey(), Text.translatable(DragonBreed.getTranslationKey(this.getBreed().id(this.getWorld().getRegistryManager()).toString())));
    }

    public void setCustomName(Text name) {
        this.customName = name;
    }

    @SuppressWarnings({"ConstantConditions", "unused"}) // guarded
    public void tick(World world, BlockPos pos, BlockState state) {
        if (!world.isClient && !this.hasBreed()) {// at this point we may not receive a breed; resolve a random one.
            DragonBreed newBreed = BreedRegistry.getRandom(this.getWorld().getRegistryManager(), this.getWorld().getRandom());
            this.setBreed(() -> newBreed);
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
        }
        this.hatchTick++;
        if (this.breed.get() == null) return;
        if (world instanceof ServerWorld serverWorld && !DMCommonConfig.INSTANCE.COMMON.randomTickHatch.getValue() && this.hatchTick >= DMCommonConfig.INSTANCE.COMMON.getHatchTime(this.breed.get().id(this.getWorld().getRegistryManager()).toString()) / 4)
            DMBlocks.EGG_BLOCK.randomTick(state, serverWorld, pos, world.random);
        if (state.get(HatchableEggBlock.HATCHING))
            for (int i = 0; i < 5; i++) {
                BlockPos p = this.getPos();
                Random random = new LocalRandom(System.currentTimeMillis());
                double px = pos.getX() + random.nextDouble();
                double py = pos.getY() + random.nextDouble();
                double pz = pos.getZ() + random.nextDouble();
                DustParticleEffect particle = HatchableEggBlock.dustParticleFor(this.breed.get(), random);
                HatchableEggBlockEntity.this.getWorld().addParticle(particle, px, py, pz, 0, 0, 0);
            }
    }

    @SuppressWarnings("ConstantConditions") // level exists at this point
    public void updateHabitat() {
        DragonBreed winner = null;
        int prevPoints = 0;
        for (DragonBreed breed : BreedRegistry.registry(this.getWorld().getRegistryManager())) {
            int points = 0;
            for (Habitat habitat : breed.habitats()) points += habitat.getHabitatPoints(this.world, this.getPos());
            if (points > MIN_HABITAT_POINTS && points > prevPoints) {
                winner = breed;
                prevPoints = points;
            }
        }
        if (winner != null && winner != this.getBreed())
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
    }
}
