package com.iafenvoy.dragonmounts.dragon.egg;

import com.iafenvoy.dragonmounts.DMLConfig;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import com.iafenvoy.dragonmounts.registry.DMEntities;
import com.iafenvoy.uranus.ServerHelper;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static net.minecraft.state.property.Properties.WATERLOGGED;

@SuppressWarnings("deprecation")
public class HatchableEggBlock extends DragonEggBlock implements BlockEntityProvider, Waterloggable {
    public static final IntProperty HATCH_STAGE = IntProperty.of("hatch_stage", 0, 3);
    public static final BooleanProperty HATCHING = BooleanProperty.of("hatching");

    public static final float DEFAULT_HATCH_CHANCE = 0.1f;

    public static final String NBT_HATCH_STAGE = "hatch_stage";
    public static final String NBT_BREED = TameableDragon.NBT_BREED;
    public static final String NBT_NAME = "CustomName";

    public HatchableEggBlock() {
        super(Settings.create().mapColor(MapColor.BLACK).strength(0f, 9f).luminance(s -> 1).nonOpaque());
        this.setDefaultState(this.getDefaultState()
                .with(HATCH_STAGE, 0)
                .with(HATCHING, false)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HATCH_STAGE, HATCHING, WATERLOGGED);
    }

    public static void populateTab(FabricItemGroupEntries registrar) {
        if (MinecraftClient.getInstance().world != null) {
            DynamicRegistryManager reg = MinecraftClient.getInstance().world.getRegistryManager();
            for (DragonBreed breed : BreedRegistry.registry(reg))
                registrar.add(Item.create(breed, reg));
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static HatchableEggBlockEntity place(ServerWorld world, BlockPos pos, BlockState state, DragonBreed breed) {
        world.setBlockState(pos, state, Block.NOTIFY_ALL);
        // Forcibly add new BlockEntity, so we can set the specific breed.
        HatchableEggBlockEntity data = ((HatchableEggBlockEntity) ((HatchableEggBlock) state.getBlock()).createBlockEntity(pos, state));
        data.setBreed(() -> breed);
        world.addBlockEntity(data);
        return data;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pPos, BlockState pState) {
        return new HatchableEggBlockEntity(pPos, pState);
    }

    @Override
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        DragonBreed breed = world.getBlockEntity(pos) instanceof HatchableEggBlockEntity e && e.hasBreed() ?
                e.getBreed() :
                BreedRegistry.getRandom(world.getRegistryManager(), world.getRandom());
        if (breed != null)
            dropStack(world, pos, Item.create(breed, world.getRegistryManager()));
        super.onStacksDropped(state, world, pos, tool, dropExperience);
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        return super.getDroppedStacks(state, builder);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World _level, BlockState _state, BlockEntityType<T> type) {
        return type != DMBlocks.EGG_BLOCK_ENTITY ? null :
                cast(((level, pos, state, be) -> be.tick(level, pos, state)));
    }

    @SuppressWarnings("unchecked")
    private static <F extends BlockEntityTicker<HatchableEggBlockEntity>, T> T cast(F from) {
        return (T) from;
    }

    @Override
    public ActionResult onUse(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand, BlockHitResult pHit) {
        if (!pState.get(HATCHING)) {
            if (!pLevel.isClient) {
                pLevel.setBlockState(pPos, pState.with(HATCHING, true), Block.NOTIFY_ALL);
                return ActionResult.CONSUME;
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos at, PlayerEntity pPlayer) {
        if (world.getBlockEntity(at) instanceof HatchableEggBlockEntity e && e.getBreed() != null && e.getBreed().id(world.getRegistryManager()).getPath().equals("end") && !state.get(HATCHING))
            teleport(state, world, at); // retain original dragon egg teleport behavior
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    //TODO
//    @Override
//    public boolean onDestroyedByPlayer(BlockState state, World level, BlockPos at, PlayerEntity player, boolean willHarvest, FluidState fluid) {
//        if (!player.getAbilities().creativeMode
//                && level.getBlockEntity(at) instanceof HatchableEggBlockEntity e
//                && e.hasBreed()
//                && e.getBreed().id(level.getRegistryManager()).getPath().equals("end")
//                && !state.get(HATCHING))
//            return false; // retain original dragon egg teleport behavior; DON'T destroy!
//        return super.onDestroyedByPlayer(state, level, at, player, willHarvest, fluid);
//    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable BlockView level, List<Text> tooltips, TooltipContext context) {
        super.appendTooltip(stack, level, tooltips, context);

        NbtCompound tag = stack.getSubNbt(BlockItem.BLOCK_STATE_TAG_KEY);
        String stage = tag != null ? tag.getString(NBT_HATCH_STAGE) : "0";
        tooltips.add(Text.translatable(this.getTranslationKey() + ".hatch_stage." + stage).formatted(Formatting.GRAY));

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.getAbilities().creativeMode) {
            tooltips.add(ScreenTexts.EMPTY);
            tooltips.add(Text.translatable(this.getTranslationKey() + ".desc1").formatted(Formatting.GRAY));
            tooltips.add(ScreenTexts.space().append(Text.translatable(this.getTranslationKey() + ".desc2")).formatted(Formatting.BLUE));
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos currentPos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED))
            world.scheduleFluidTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, currentPos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Original logic trashes BlockEntity data. We need it, so do it ourselves.
        if (canFallThrough(world.getBlockState(pos.down())) && pos.getY() >= world.getBottomY()) {
            NbtCompound tag = null;
            if (world.getBlockEntity(pos) instanceof HatchableEggBlockEntity e) tag = e.createNbt();

            FallingBlockEntity entity = FallingBlockEntity.spawnFromBlock(world, pos, state); // this deletes the block. We need to cache the data first and then apply it.
            if (tag != null) entity.blockEntityData = tag;
            this.configureFallingBlockEntity(entity);
        }
    }

    @Override
    public boolean hasRandomTicks(BlockState pState) {
        return pState.get(HATCHING);
    }

    @Override // will only tick when HATCHING, according to isRandomlyTicking
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!(world.getBlockEntity(pos) instanceof HatchableEggBlockEntity data) || data.getBreed() == null) return;
        int hatchStage = state.get(HATCH_STAGE);
        boolean finalStage = hatchStage == 3;

        if (random.nextFloat() < data.getBreed().hatchChance()) {
            if (finalStage)
                this.hatch(world, pos);
            else {
                this.crack(world, pos);
                world.setBlockState(pos, state.with(HATCH_STAGE, hatchStage + 1), Block.NOTIFY_ALL);
            }
            return;
        }

        if (finalStage) // too far gone to change habitats now!
            this.crack(world, pos); // being closer to hatching creates more struggles to escape
        else if (DMLConfig.updateHabitats() && !data.getTransition().isRunning())
            data.updateHabitat();
    }

    @Override
    public void randomDisplayTick(BlockState pState, World world, BlockPos pPos, Random random) {
        if (pState.get(HATCHING) && world.getBlockEntity(pPos) instanceof HatchableEggBlockEntity e && e.hasBreed())
            for (int i = 0; i < random.nextBetween(4, 7); i++)
                this.addHatchingParticles(e.getBreed(), world, pPos, random);
    }

    private void crack(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.85f, 0.95f + world.getRandom().nextFloat() * 0.2f);
    }

    @SuppressWarnings("ConstantConditions") // creation of dragon is never null
    private void hatch(ServerWorld level, BlockPos pos) {
        var data = (HatchableEggBlockEntity) level.getBlockEntity(pos);
        var baby = DMEntities.DRAGON.create(level);

        level.playSound(null, pos, SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 1.2f, 0.95f + level.getRandom().nextFloat() * 0.2f);
        level.removeBlock(pos, false); // remove block AFTER data is cached

        baby.setBreed(data.getBreed());
        baby.setBaby(true);
        baby.setPos(pos.getX(), pos.getY(), pos.getZ());
        baby.setCustomName(data.getCustomName());
        level.spawnEntity(baby);
    }

    public void addHatchingParticles(DragonBreed breed, World level, BlockPos pos, Random random) {
        double px = pos.getX() + random.nextDouble();
        double py = pos.getY() + random.nextDouble();
        double pz = pos.getZ() + random.nextDouble();
        double ox = 0;
        double oy = 0;
        double oz = 0;

        var particle = getHatchingParticles(breed, random);
        if (particle.getType() == ParticleTypes.DUST) py = pos.getY() + (random.nextDouble() - 0.5) + 1;
        else if (particle.getType() == ParticleTypes.PORTAL) {
            ox = (random.nextDouble() - 0.5) * 2;
            oy = (random.nextDouble() - 0.5) * 2;
            oz = (random.nextDouble() - 0.5) * 2;
        }

        level.addParticle(particle, px, py, pz, ox, oy, oz);
    }

    public static ParticleEffect getHatchingParticles(DragonBreed breed, Random random) {
        return breed.hatchParticles().orElseGet(() -> dustParticleFor(breed, random));
    }

    public static DustParticleEffect dustParticleFor(DragonBreed breed, Random random) {
        return new DustParticleEffect(Vec3d.unpackRgb(random.nextDouble() < 0.75 ? breed.primaryColor() : breed.secondaryColor()).toVector3f(), 1);
    }

    // taken from DragonEggBlock#teleport
    private static void teleport(BlockState state, World world, BlockPos pos) {
        WorldBorder worldBorder = world.getWorldBorder();

        for (int i = 0; i < 1000; ++i) {// excessive?
            BlockPos teleportPos = pos.add(world.random.nextInt(16) - world.random.nextInt(16), world.random.nextInt(8) - world.random.nextInt(8), world.random.nextInt(16) - world.random.nextInt(16));
            if (world.getBlockState(teleportPos).isAir() && worldBorder.contains(teleportPos)) {
                if (world.isClient) {
                    for (int j = 0; j < 128; ++j) {
                        double d0 = world.random.nextDouble();
                        float f = (world.random.nextFloat() - 0.5F) * 0.2F;
                        float f1 = (world.random.nextFloat() - 0.5F) * 0.2F;
                        float f2 = (world.random.nextFloat() - 0.5F) * 0.2F;
                        double d1 = MathHelper.lerp(d0, teleportPos.getX(), pos.getX()) + (world.random.nextDouble() - 0.5D) + 0.5D;
                        double d2 = MathHelper.lerp(d0, teleportPos.getY(), pos.getY()) + world.random.nextDouble() - 0.5D;
                        double d3 = MathHelper.lerp(d0, teleportPos.getZ(), pos.getZ()) + (world.random.nextDouble() - 0.5D) + 0.5D;
                        world.addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
                    }
                } else {
                    // Original Dragon Egg does not have a BlockEntity to account for,
                    // so our own teleport will now restore the block data.
                    NbtCompound data = Objects.requireNonNull(world.getBlockEntity(pos)).createNbt();
                    world.removeBlock(pos, false);
                    world.setBlockState(teleportPos, state, Block.NOTIFY_LISTENERS);
                    Objects.requireNonNull(world.getBlockEntity(teleportPos)).readNbt(data);
                }
                return;
            }
        }
    }

    public static class Item extends BlockItem {
        // "BlockStateTag"
        //      -- "hatch_stage"
        // "BlockEntityTag"
        //      -- "Breed"
        //      -- "CustomName"
        //      -- "TransitionerTag"
        //              -- "TransitionBreed"
        //              -- "TransitionTime"

        public Item() {
            super(DMBlocks.EGG_BLOCK, new Settings().rarity(Rarity.EPIC));
        }

        @Override
        public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, world, entity, slot, selected);
            ensureExistingBreedType(stack);
        }

        @Override
        public Text getName(ItemStack stack) {
            var tag = BlockItem.getBlockEntityNbt(stack);
            if (tag != null)
                return Text.translatable(String.join(".", this.getTranslationKey(), tag.getString(NBT_BREED).replace(':', '.')));
            return super.getName(stack);
        }

        @Override
        public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
            if (player.getAbilities().creativeMode && target instanceof TameableDragon dragon) {
                var tag = BlockItem.getBlockEntityNbt(stack);
                if (tag != null) {
                    dragon.setBreed(BreedRegistry.get(tag.getString(TameableDragon.NBT_BREED), player.getWorld().getRegistryManager()));
                    return ActionResult.success(player.getWorld().isClient);
                }
            }
            return super.useOnEntity(stack, player, target, hand);
        }

        @Override
        public ActionResult place(ItemPlacementContext pContext) {
            var result = super.place(pContext);
            if (result.isAccepted() && pContext.getStack().hasCustomName() && pContext.getWorld().getBlockEntity(pContext.getBlockPos()) instanceof HatchableEggBlockEntity e)
                e.setCustomName(pContext.getStack().getName());
            return result;
        }

        private static void ensureExistingBreedType(ItemStack stack) {
            if (ServerHelper.server == null) return;
            if (!stack.hasNbt()) stack.setNbt(new NbtCompound());
            NbtCompound blockEntityData = stack.getOrCreateSubNbt(BlockItem.BLOCK_ENTITY_TAG_KEY);
            String breed = blockEntityData.getString(NBT_BREED);
            Registry<DragonBreed> reg = BreedRegistry.registry(ServerHelper.server.getRegistryManager());
            if (breed.isEmpty() || !reg.containsId(new Identifier(breed))) {// this item doesn't contain a breed yet?
                breed = reg.getRandom(Random.create()).orElseThrow().registryKey().getValue().toString();
                blockEntityData.putString(NBT_BREED, breed); // assign one ourselves then.
            }
        }

        public static ItemStack create(DragonBreed breed, DynamicRegistryManager reg) {
            NbtCompound tag = new NbtCompound();
            tag.putString(TameableDragon.NBT_BREED, breed.id(reg).toString());
            ItemStack stack = new ItemStack(DMBlocks.EGG_BLOCK);
            BlockItem.setBlockEntityNbt(stack, DMBlocks.EGG_BLOCK_ENTITY, tag);
            return stack;
        }
    }
}
