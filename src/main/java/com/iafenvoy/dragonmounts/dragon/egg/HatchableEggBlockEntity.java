package com.iafenvoy.dragonmounts.dragon.egg;

import com.google.common.base.Suppliers;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.habitats.Habitat;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class HatchableEggBlockEntity extends BlockEntity implements Nameable {
    public static final int MIN_HABITAT_POINTS = 2;
    public static final int BREED_TRANSITION_TIME = 200;
    private final TransitionHandler transitioner = new TransitionHandler();
    private Supplier<DragonBreed> breed = () -> null;
    private Text customName;

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
        if (this.getTransition().isRunning()) {
            NbtCompound transitionTag = new NbtCompound();
            this.getTransition().save(transitionTag);
            tag.put(TransitionHandler.NBT_TRANSITIONER, transitionTag);
        }
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
        NbtCompound transitioner = pTag.getCompound(TransitionHandler.NBT_TRANSITIONER);
        if (!transitioner.isEmpty()) this.getTransition().load(transitioner);
        if (this.getWorld() != null && this.getWorld().isClient()) // client needs to be aware of new changes
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound tag = super.toInitialChunkDataNbt();
        this.writeNbt(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
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

    public TransitionHandler getTransition() {
        return this.transitioner;
    }

    @SuppressWarnings({"ConstantConditions", "unused"}) // guarded
    public void tick(World pLevel, BlockPos pPos, BlockState pState) {
        if (!pLevel.isClient() && !this.hasBreed()) {// at this point we may not receive a breed; resolve a random one.
            DragonBreed newBreed = BreedRegistry.getRandom(this.getWorld().getRegistryManager(), this.getWorld().getRandom());
            this.setBreed(() -> newBreed);
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
        }
        this.getTransition().tick(this.getWorld().getRandom());
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
        if (winner != null && winner != this.getBreed()) {
            this.getTransition().begin(winner);
            this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
        }
    }

    @SuppressWarnings("ConstantConditions") // level exists at this point
    public class TransitionHandler {
        private static final String NBT_TRANSITIONER = "TransitionerTag";
        private static final String NBT_TRANSITION_BREED = "TransitionBreed";
        private static final String NBT_TRANSITION_TIME = "TransitionTime";

        public Supplier<DragonBreed> transitioningBreed = () -> null;
        public int transitionTime;

        public void tick(Random random) {
            if (this.isRunning()) {
                if (this.transitioningBreed.get() == null) {// invalid breed id, etc.
                    this.transitionTime = 0;
                    return;
                }
                if (--this.transitionTime == 0) {
                    HatchableEggBlockEntity.this.setBreed(this.transitioningBreed);
                    HatchableEggBlockEntity.this.getWorld().updateListeners(HatchableEggBlockEntity.this.getPos(), HatchableEggBlockEntity.this.getCachedState(), HatchableEggBlockEntity.this.getCachedState(), Block.REDRAW_ON_MAIN_THREAD);
                }
                if (HatchableEggBlockEntity.this.getWorld().isClient) {
                    for (var i = 0; i < (BREED_TRANSITION_TIME - this.transitionTime) * 0.25; i++) {
                        BlockPos pos = HatchableEggBlockEntity.this.getPos();
                        double px = pos.getX() + random.nextDouble();
                        double py = pos.getY() + random.nextDouble();
                        double pz = pos.getZ() + random.nextDouble();
                        DustParticleEffect particle = HatchableEggBlock.dustParticleFor(this.transitioningBreed.get(), random);
                        HatchableEggBlockEntity.this.getWorld().addParticle(particle, px, py, pz, 0, 0, 0);
                    }
                }
            }
        }

        public void startFrom(Supplier<DragonBreed> transitioningBreed, int transitionTime) {
            this.transitioningBreed = transitioningBreed;
            this.transitionTime = transitionTime;
        }

        public void begin(DragonBreed transitioningBreed) {
            this.startFrom(() -> transitioningBreed, BREED_TRANSITION_TIME);
        }

        public boolean isRunning() {
            return this.transitionTime > 0;
        }

        public void save(NbtCompound tag) {
            tag.putString(NBT_TRANSITION_BREED, this.transitioningBreed.get().id(HatchableEggBlockEntity.this.getWorld().getRegistryManager()).toString());
            tag.putInt(NBT_TRANSITION_TIME, this.transitionTime);
        }

        public void load(NbtCompound tag) {
            this.startFrom(Suppliers.memoize(() -> BreedRegistry.get(tag.getString(NBT_TRANSITION_BREED), HatchableEggBlockEntity.this.getWorld().getRegistryManager())), tag.getInt(NBT_TRANSITION_TIME));
        }
    }
}
