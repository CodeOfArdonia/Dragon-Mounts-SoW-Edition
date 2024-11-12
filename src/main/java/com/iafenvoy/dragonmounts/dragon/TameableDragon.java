package com.iafenvoy.dragonmounts.dragon;

import com.iafenvoy.dragonmounts.config.DMConfig;
import com.iafenvoy.dragonmounts.DragonMounts;
import com.iafenvoy.dragonmounts.abilities.Ability;
import com.iafenvoy.dragonmounts.client.DragonAnimator;
import com.iafenvoy.dragonmounts.client.MountCameraManager;
import com.iafenvoy.dragonmounts.client.MountControlsMessenger;
import com.iafenvoy.dragonmounts.data.CrossBreedingManager;
import com.iafenvoy.dragonmounts.dragon.ai.DragonBodyController;
import com.iafenvoy.dragonmounts.dragon.ai.DragonBreedGoal;
import com.iafenvoy.dragonmounts.dragon.ai.DragonFollowOwnerGoal;
import com.iafenvoy.dragonmounts.dragon.ai.DragonMoveController;
import com.iafenvoy.dragonmounts.dragon.breed.BreedRegistry;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlock;
import com.iafenvoy.dragonmounts.dragon.egg.HatchableEggBlockEntity;
import com.iafenvoy.dragonmounts.registry.DMBlocks;
import com.iafenvoy.dragonmounts.registry.DMEntities;
import com.iafenvoy.dragonmounts.registry.DMKeyBindings;
import com.iafenvoy.dragonmounts.registry.DMSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SaddleItem;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.entity.attribute.EntityAttributes.*;

/**
 * Here be dragons.
 * <p>
 * Let the legacy live on.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 * @author Kay9
 */
@SuppressWarnings({"deprecation", "SameReturnValue"})
public class TameableDragon extends TameableEntity implements Saddleable, Flutterer, Mount {
    // base attributes
    public static final double BASE_SPEED_GROUND = 0.3; // actual speed varies from ground friction
    public static final double BASE_SPEED_FLYING = 0.32;
    public static final double BASE_DAMAGE = 8;
    public static final double BASE_HEALTH = 60;
    public static final double BASE_FOLLOW_RANGE = 16;
    public static final int BASE_KB_RESISTANCE = 1;
    public static final float BASE_WIDTH = 2.75f; // adult sizes
    public static final float BASE_HEIGHT = 2.75f;
    public static final int BASE_GROWTH_TIME = 72000;
    public static final float BASE_SIZE_MODIFIER = 1.0f;
    // data value IDs
    private static final TrackedData<String> DATA_BREED = DataTracker.registerData(TameableDragon.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> DATA_SADDLED = DataTracker.registerData(TameableDragon.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> DATA_AGE = DataTracker.registerData(TameableDragon.class, TrackedDataHandlerRegistry.INTEGER);
    // data NBT IDs
    public static final String NBT_BREED = "Breed";
    private static final String NBT_SADDLED = "Saddle";
    private static final String NBT_REPRO_COUNT = "ReproCount";
    // other constants
    public static final int AGE_UPDATE_INTERVAL = 100; // every 5 seconds
    public static final UUID SCALE_MODIFIER_UUID = UUID.fromString("856d4ba4-9ffe-4a52-8606-890bb9be538b"); // just a random uuid I took online
    public static final int GROUND_CLEARENCE_THRESHOLD = 3; // height in blocks (multiplied by scale of dragon)
    // server/client delegates
    private final DragonAnimator animator;
    private final List<Ability> abilities = new ArrayList<>();
    private DragonBreed breed;
    private int reproCount;
    private float ageProgress = 1; // default to adult
    private boolean flying;
    private boolean nearGround;
    private final MobNavigation groundNavigation;
    private final BirdNavigation flyingNavigation;

    public TameableDragon(EntityType<? extends TameableDragon> type, World level) {
        super(type, level);
        this.ignoreCameraFrustum = true;
        this.moveControl = new DragonMoveController(this);
        this.animator = level.isClient ? new DragonAnimator(this) : null;
        this.flyingNavigation = new BirdNavigation(this, level);
        this.groundNavigation = new MobNavigation(this, level);
        this.flyingNavigation.setCanSwim(true);
        this.groundNavigation.setCanSwim(true);
        this.navigation = this.groundNavigation;
    }

    @Override
    @NotNull
    public BodyControl createBodyControl() {
        return new DragonBodyController(this);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(GENERIC_MOVEMENT_SPEED, BASE_SPEED_GROUND)
                .add(GENERIC_MAX_HEALTH, BASE_HEALTH)
                .add(GENERIC_FOLLOW_RANGE, BASE_FOLLOW_RANGE)
                .add(GENERIC_KNOCKBACK_RESISTANCE, BASE_KB_RESISTANCE)
                .add(GENERIC_ATTACK_DAMAGE, BASE_DAMAGE)
                .add(GENERIC_FLYING_SPEED, BASE_SPEED_FLYING);
    }

    @Override
    protected void initGoals() {// TODO: Much Smarter AI and features
//        goalSelector.addGoal(1, new DragonLandGoal(this));
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new SitGoal(this));
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1, true));
//        goalSelector.addGoal(4, new DragonBabuFollowParent(this, 10));
        this.goalSelector.add(5, new DragonFollowOwnerGoal(this, 1f, 10f, 3.5f, 32f));
        this.goalSelector.add(5, new DragonBreedGoal(this));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.85f));
        this.goalSelector.add(7, new LookAtEntityGoal(this, LivingEntity.class, 16f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(0, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(1, new AttackWithOwnerGoal(this));
        this.targetSelector.add(2, new RevengeGoal(this));
        this.targetSelector.add(3, new UntamedActiveTargetGoal<>(this, AnimalEntity.class, false, e -> !(e instanceof TameableDragon)));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DATA_BREED, "");
        this.dataTracker.startTracking(DATA_SADDLED, false);
        this.dataTracker.startTracking(DATA_AGE, 0); // default to adult stage
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (DATA_BREED.equals(data)) {
            this.setBreed(BreedRegistry.get(this.dataTracker.get(DATA_BREED), this.getWorld().getRegistryManager()));
            this.updateAgeProperties();
        } else if (TAMEABLE_FLAGS.equals(data)) this.calculateDimensions();
        else if (DATA_AGE.equals(data)) this.updateAgeProperties();
        else super.onTrackedDataSet(data);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound compound) {
        super.writeCustomDataToNbt(compound);
        compound.putBoolean(NBT_SADDLED, this.isSaddled());
        compound.putInt(NBT_REPRO_COUNT, this.reproCount);
        if (this.getBreed() != null) {// breed is not read by the time the packet is being sent...
            compound.putString(NBT_BREED, this.getBreed().id(this.getWorld().getRegistryManager()).toString());
            for (Ability ability : this.getAbilities()) ability.write(this, compound);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound compound) {
        // read and set breed first before reading everything else so things can override correctly,
        // e.g. attributes.
        DragonBreed breed = BreedRegistry.get(compound.getString(NBT_BREED), this.getWorld().getRegistryManager());
        if (breed != null) this.setBreed(breed);
        super.readCustomDataFromNbt(compound);
        this.setSaddled(compound.getBoolean(NBT_SADDLED));
        this.reproCount = compound.getInt(NBT_REPRO_COUNT);
        for (Ability ability : this.getAbilities()) ability.read(this, compound);
        // set sync age data after we read it in AgeableMob
        this.dataTracker.set(DATA_AGE, this.getBreedingAge());
    }

    public void setBreed(DragonBreed dragonBreed) {
        if (this.breed != dragonBreed) {// prevent loops, unnecessary work, etc.
            if (this.breed != null) this.breed.close(this);
            this.breed = dragonBreed;
            this.breed.initialize(this);
            this.getDataTracker().set(DATA_BREED, this.breed.id(this.getWorld().getRegistryManager()).toString());
        }
    }

    /**
     * Since a breed type cannot be passed into the constructor (due to the dynamic nature of breeds)
     * and sometimes a breed type cannot be deserialized in time, there's always the possibility of
     * a nullable breed.
     */
    @Nullable
    public DragonBreed getBreed() {
        return this.breed;
    }

    /**
     * For ease of use when we aren't guaranteed on the breed
     */
    public Optional<DragonBreed> getBreedOptionally() {
        return Optional.ofNullable(this.breed);
    }

    public List<Ability> getAbilities() {
        return this.abilities;
    }

    /**
     * Returns true if the dragon is saddled.
     */
    public boolean isSaddled() {
        return this.dataTracker.get(DATA_SADDLED);
    }

    @Override
    public boolean canBeSaddled() {
        return this.isAlive() && !this.isHatchling() && this.isTamed();
    }

    @Override
    public void saddle(@Nullable SoundCategory source) {
        this.setSaddled(true);
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_HORSE_SADDLE, this.getSoundCategory(), 1, 1);
    }

    /**
     * Set or remove the saddle of the dragon.
     */
    public void setSaddled(boolean saddled) {
        this.dataTracker.set(DATA_SADDLED, saddled);
    }

    public void addReproCount() {
        this.reproCount++;
    }

    public boolean canFly() {
        // hatchling's can't fly
        return !this.isHatchling();
    }

    public boolean shouldFly() {
        if (this.isInAir()) return !this.isOnGround(); // more natural landings
        return this.canFly() && !this.isTouchingWater() && !this.isNearGround();
    }

    /**
     * Returns true if the entity is flying.
     */
    public boolean isInAir() {
        return this.flying;
    }

    /**
     * Set the flying flag of the entity.
     */
    public void setFlying(boolean flying) {
        this.flying = flying;
    }

    public boolean isNearGround() {
        return this.nearGround;
    }

    public void setNavigation(boolean flying) {
        this.navigation = flying ?
                this.flyingNavigation :
                this.groundNavigation;
    }

    @Override
    public void tick() {
        if (this.isServer() && this.breed == null) // if we don't have a breed at this point, we should assume we aren't getting one, so assign a random one.
            this.setBreed(BreedRegistry.getRandom(this.getWorld().getRegistryManager(), this.getRandom()));
        super.tick();
        if (this.isServer()) {
            // periodically sync age data back to client
            if (!this.isAdult() && this.age % AGE_UPDATE_INTERVAL == 0)
                this.dataTracker.set(DATA_AGE, this.breedingAge);
            // heal randomly
            if (this.isAlive() && this.getRandom().nextFloat() < 0.001) this.heal(1f);
        } else {
            // update animations on the client
            this.animator.tick();
            // because vanilla age does not increment on client...
            int age = this.getBreedingAge();
            if (age < 0) this.setBreedingAge(++age);
            else if (age > 0) this.setBreedingAge(--age);
        }
        // update nearGround state when moving for flight and animation logic
        this.nearGround = this.isOnGround() || !this.getWorld().isSpaceEmpty(this, new Box(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY() - (GROUND_CLEARENCE_THRESHOLD * this.getScaleFactor()), this.getZ()));
        // update flying state based on the distance to the ground
        boolean flying = this.shouldFly();
        if (flying != this.isInAir()) {
            this.setFlying(flying);
            // update pathfinding method
            if (this.isServer()) this.setNavigation(flying);
        }
        this.updateAgeProgress();
        for (Ability ability : this.getAbilities()) ability.tick(this);
    }

    @Override
    public void travel(Vec3d vec3) {
        if (this.isInAir()) {
            if (this.isLogicalSideForUpdatingMovement()) {
                // Move relative to yaw - handled in the move controller or by driver
                this.updateVelocity(this.getMovementSpeed(), vec3);
                this.move(MovementType.SELF, this.getVelocity());
                if (this.getVelocity().lengthSquared() < 0.1) // we're not actually going anywhere, bob up and down.
                    this.setVelocity(this.getVelocity().add(0, Math.sin(this.age / 4f) * 0.03, 0));
                this.setVelocity(this.getVelocity().multiply(0.9f)); // smoothly slow down
            }
            this.updateLimbs(true);
        } else super.travel(vec3);
    }

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity driver, Vec3d move) {
        double moveSideways = move.x;
        double moveY = move.y;
        double moveForward = Math.min(Math.abs(driver.forwardSpeed) + Math.abs(driver.sidewaysSpeed), 1);
        if (this.isInAir() && this.hasLocalDriver()) {
            moveForward = moveForward > 0 ? moveForward : 0;
            if (driver.jumping) moveY = 1;
            else if (DMKeyBindings.FLIGHT_DESCENT_KEY.isPressed()) moveY = -1;
            else {
                if (moveForward > 0) {
                    if (DMConfig.CLIENT.cameraDrivenFlight) moveY = -driver.getPitch() / 90; // normalize from -1 to 1
                }
            }
        }
        // mimic dogshit implementation of AI movement vectors
        // the way this works is that it will mimic how setSpeed in Mob works:
        // it sets the normal speed variable,
        // and then sets the walk forward variable to the same value.
        // so if speed is 0.3, walk forward will also be 0.3 instead of 1.0.
        // so when moveRelative calculates movespeed, (walkforward * speed) we get 0.15.
        // so I guess we should do it to.
        float speed = this.getSaddledSpeed(driver);
        return new Vec3d(moveSideways * speed, moveY * speed, moveForward * speed);
    }

    @Override
    protected void tickControlled(PlayerEntity driver, Vec3d move) {
        // rotate head to match driver.
        float yaw = driver.headYaw;
        if (move.z > 0) // rotate in the direction of the drivers controls
            yaw += (float) MathHelper.atan2(driver.forwardSpeed, driver.sidewaysSpeed) * (180f / (float) Math.PI) - 90;
        this.headYaw = yaw;
        this.setPitch(driver.getPitch() * 0.68f);
        // rotate body towards the head
        this.setYaw(MathHelper.clampAngle(this.headYaw, this.getYaw(), 4));
        if (this.isLogicalSideForUpdatingMovement())
            if (!this.isInAir() && this.canFly() && driver.jumping)
                this.liftOff();
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity driver) {
        return (float) this.getAttributeValue(this.isInAir() ? GENERIC_FLYING_SPEED : GENERIC_MOVEMENT_SPEED);
    }

    @Override
    @SuppressWarnings("ConstantConditions") // I bet the breed exists at this point...
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        ActionResult stackResult = stack.useOnEntity(player, this, hand);
        if (stackResult.isAccepted()) return stackResult;
        // tame
        if (!this.isTamed()) {
            if (this.isServer() && this.getBreed().tamingItems().contains(stack.getItem().getRegistryEntry())) {
                stack.decrement(1);
                this.tamedFor(player, this.getRandom().nextInt(5) == 0);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS; // pass regardless. We don't want to perform breeding, age ups, etc. on untamed.
        }
        // heal
        if (this.getHealthFraction() < 1 && this.isFoodItem(stack)) {
            //noinspection ConstantConditions
            this.heal(stack.getItem().getFoodComponent().getHunger());
            this.playSound(this.getEatSound(stack), 0.7f, 1);
            stack.decrement(1);
            return ActionResult.success(this.getWorld().isClient);
        }
        // saddle up!
        if (this.isTamedFor(player) && this.canBeSaddled() && !this.isSaddled() && stack.getItem() instanceof SaddleItem) {
            stack.decrement(1);
            this.saddle(this.getSoundCategory());
            return ActionResult.success(this.getWorld().isClient);
        }
        // give the saddle back!
        if (this.isTamedFor(player) && this.isSaddled() && stack.isOf(Items.SHEARS)) {//TODO:Tags
            this.dropItem(Items.SADDLE);
            player.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1f, 1f);
            this.setSaddled(false);
            this.emitGameEvent(GameEvent.SHEAR, player);
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
            return ActionResult.success(this.getWorld().isClient);
        }
        // sit!
        if (this.isTamedFor(player) && (player.shouldCancelInteraction() || stack.isOf(Items.BONE))) {// "bone sitting" for legacy reasons
            if (this.isServer()) {
                this.navigation.stop();
                this.setSitting(!this.isSitting());
                if (this.isSitting()) this.setTarget(null);
            }
            return ActionResult.success(this.getWorld().isClient);
        }

        // ride on
        if (this.isTamedFor(player) && this.isSaddled() && !this.isHatchling() && !this.isBreedingItem(stack)) {
            if (this.isServer()) {
                player.startRiding(this);
                this.navigation.stop();
                this.setTarget(null);
            }
            this.setSitting(false);
            this.setInSittingPose(false);
            return ActionResult.success(this.getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    public void liftOff() {
        if (this.canFly()) this.jump();
    }

    @Override
    protected float getJumpVelocity() {
        // stronger jumps for easier lift-offs
        return super.getJumpVelocity() * (this.canFly() ? 3 : 1);
    }

    @Override
    public boolean handleFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return !this.canFly() && super.handleFallDamage(pFallDistance, pMultiplier, pSource);
    }

    @Override
    protected void updatePostDeath() {
        // unmount any riding entities
        this.removeAllPassengers();
        // freeze at place
        this.setVelocity(Vec3d.ZERO);
        this.setYaw(this.prevYaw);
        this.setHeadYaw(this.prevHeadYaw);
        if (this.deathTime >= this.getMaxDeathTime())
            this.remove(RemovalReason.KILLED); // actually delete entity after the time is up
        this.deathTime++;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getBreedOptionally().flatMap(DragonBreed::ambientSound).map(RegistryEntry::value).orElse(DMSounds.DRAGON_AMBIENT_SOUND);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.ENTITY_ENDER_DRAGON_HURT;
    }

    public SoundEvent getStepSound() {
        return DMSounds.DRAGON_STEP_SOUND;
    }

    /**
     * Returns the sound this mob makes on death.
     */
    @Override
    protected SoundEvent getDeathSound() {
        return DMSounds.DRAGON_DEATH_SOUND;
    }

    @Override
    public SoundEvent getEatSound(ItemStack itemStackIn) {
        return SoundEvents.ENTITY_GENERIC_EAT;
    }

    public SoundEvent getAttackSound() {
        return SoundEvents.ENTITY_GENERIC_EAT;
    }

    public SoundEvent getWingsSound() {
        return SoundEvents.ENTITY_ENDER_DRAGON_FLAP;
    }

    /**
     * Plays step sound at given x, y, z for the entity
     */
    @Override
    protected void playStepSound(BlockPos entityPos, BlockState state) {
        if (this.isTouchingWater()) return;
        if (this.isHatchling()) {
            super.playStepSound(entityPos, state);
            return;
        }
        // override sound type if the top block is snowy
        BlockSoundGroup soundType = state.getSoundGroup();
        if (this.getWorld().getBlockState(entityPos.up()).getBlock() == Blocks.SNOW)
            soundType = Blocks.SNOW.getSoundGroup(state);
        // play stomping for bigger dragons
        this.playSound(this.getStepSound(), soundType.getVolume(), soundType.getPitch() * this.getSoundPitch());
    }

    @Override
    public void playAmbientSound() {
        if (this.getBreed() != null) // EntityType likes to invoke this before deserializing, so let's guard it.
            super.playAmbientSound();
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 240;
    }

    @Override
    protected float getSoundVolume() {
        return this.getScaleFactor();
    }

    @Override
    public float getSoundPitch() {
        return 2 - this.getScaleFactor();
    }

    @Override
    protected Text getDefaultName() {
        if (this.getBreed() != null)
            return Text.translatable(DragonBreed.getTranslationKey(this.getBreed().id(this.getWorld().getRegistryManager()).toString()));
        return super.getDefaultName();
    }

    public boolean isFoodItem(ItemStack stack) {
        FoodComponent food = stack.getItem().getFoodComponent();
        return food != null && food.isMeat();
    }

    // the "food" that enables breeding mode
    @Override
    @SuppressWarnings("ConstantConditions") // I bet the breed exists at this point...
    public boolean isBreedingItem(ItemStack stack) {
        return this.getBreed().breedingItems().contains(stack.getItem().getRegistryEntry());
    }

    public void tamedFor(PlayerEntity player, boolean successful) {
        if (successful) {
            this.setTamed(true);
            this.navigation.stop();
            this.setTarget(null);
            this.setOwnerUuid(player.getUuid());
            this.getWorld().sendEntityStatus(this, (byte) 7);
        } else
            this.getWorld().sendEntityStatus(this, (byte) 6);
    }

    public boolean isTamedFor(PlayerEntity player) {
        return this.isTamed() && this.isOwner(player);
    }

    /**
     * Returns the height of the eyes. Used for looking at other entities.
     */
    @Override
    protected float getActiveEyeHeight(EntityPose poseIn, EntityDimensions sizeIn) {
        return sizeIn.height * 1.2f;
    }

    /**
     * Returns the Y offset from the entity's position for any entity riding this one.
     */
    @Override
    public double getMountedHeightOffset() {
        return this.getHeight() - 0.175;
    }

    /**
     * Returns render size modifier
     * <p>
     * 0.33 is the value representing the size for baby dragons.
     * 1.0 is the value representing the size for adult dragons.
     * We are essentially scaling linearly from baby size to adult size, base on ageProgress
     * This value can be manipulated using the breed's size modifier
     */
    @Override
    public float getScaleFactor() {
        float mod = this.getBreed() == null ? 1f : this.getBreed().sizeModifier();
        return (0.33f + (0.67f * this.getAgeProgress())) * mod;
    }

    /**
     * Determines if an entity can be despawned, used on idle far away entities
     */
    @Override
    public boolean canImmediatelyDespawn(double distanceToClosestPlayer) {
        return false;
    }

    /**
     * returns true if this entity is by a ladder, false otherwise
     */
    @Override
    public boolean isClimbing() {
        // this better doesn't happen...
        return false;
    }

    @Override
    protected void dropEquipment(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropEquipment(source, looting, recentlyHitIn);
        if (this.isSaddled()) this.dropItem(Items.SADDLE);
    }

    @Override
    protected Identifier getLootTableId() {
        if (this.getBreed() == null) return LootTables.EMPTY;
        return this.getBreed().deathLoot();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public boolean tryAttack(Entity entityIn) {
        boolean attacked = entityIn.damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeInstance(GENERIC_ATTACK_DAMAGE).getValue());
        if (attacked) this.applyDamageEffects(this, entityIn);
        return attacked;
    }

    public void onWingsDown(float speed) {
        if (!this.isTouchingWater()) {
            // play wing sounds
            float pitch = (1 - speed);
            float volume = 0.3f + (1 - speed) * 0.2f;
            pitch *= this.getSoundPitch();
            volume *= this.getSoundVolume();
            this.getWorld().playSound(this.getX(), this.getY(), this.getZ(), this.getWingsSound(), SoundCategory.VOICE, volume, pitch, true);
        }
    }

    @Override
    public void swingHand(Hand hand) {
        // play eating sound
        this.playSound(this.getAttackSound(), 1, 0.7f);
        super.swingHand(hand);
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean damage(DamageSource src, float par2) {
        if (src.isOf(DamageTypes.IN_WALL)) return false;
        if (this.isInvulnerableTo(src)) return false;
        // don't just sit there!
        this.setSitting(false);
        return super.damage(src, par2);
    }

    /**
     * Returns true if the mob is currently able to mate with the specified mob.
     */
    @Override
    public boolean canBreedWith(AnimalEntity mate) {
        if (mate == this) return false; // No. Just... no.
        if (!(mate instanceof TameableDragon dragonMate)) return false;
        if (!this.canReproduce()) return false;
        if (!dragonMate.canReproduce()) return false;
        return this.isInLove() && mate.isInLove();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canReproduce() {
        if (!this.isTamed() || this.getBreed() == null) return false;
        int limit = this.getBreed().getReproductionLimit();
        return this.reproCount < limit || limit == -1;
    }

    @Override
    @SuppressWarnings("ConstantConditions") // breed nullability is checked in canReproduce
    public void breed(ServerWorld level, AnimalEntity animal) {
        if (!(animal instanceof TameableDragon mate)) {
            DragonMounts.LOGGER.warn("Tried to mate with non-dragon? Hello? {}", animal);
            return;
        }
        // pick a breed to inherit from, and place hatching.
        BlockState state = DMBlocks.EGG_BLOCK.getDefaultState().with(HatchableEggBlock.HATCHING, true);
        DragonBreed offSpringBreed = CrossBreedingManager.INSTANCE.getCrossBreed(this.getBreed(), mate.getBreed(), level.getRegistryManager());
        if (offSpringBreed == null) offSpringBreed = this.getRandom().nextBoolean() ? this.getBreed() : mate.getBreed();
        HatchableEggBlockEntity egg = HatchableEggBlock.place(level, this.getBlockPos(), state, offSpringBreed);
        // mix the custom names in case both parents have one
        if (this.hasCustomName() && animal.hasCustomName()) {
            String p1Name = this.getCustomName().getString();
            String p2Name = animal.getCustomName().getString();
            String babyName;
            if (p1Name.contains(" ") || p2Name.contains(" ")) {
                // combine two words with space
                // "Tempor Invidunt Dolore" + "Magna"
                // = "Tempor Magna" or "Magna Tempor"
                String[] p1Names = p1Name.split(" ");
                String[] p2Names = p2Name.split(" ");
                p1Name = StringUtils.capitalize(p1Names[this.getRandom().nextInt(p1Names.length)]);
                p2Name = StringUtils.capitalize(p2Names[this.getRandom().nextInt(p2Names.length)]);
                babyName = this.getRandom().nextBoolean() ? p1Name + " " + p2Name : p2Name + " " + p1Name;
            } else {
                // scramble two words
                // "Eirmod" + "Voluptua"
                // = "Eirvolu" or "Volueir" or "Modptua" or "Ptuamod" or ...
                if (this.getRandom().nextBoolean()) p1Name = p1Name.substring(0, (p1Name.length() - 1) / 2);
                else p1Name = p1Name.substring((p1Name.length() - 1) / 2);
                if (this.getRandom().nextBoolean()) p2Name = p2Name.substring(0, (p2Name.length() - 1) / 2);
                else p2Name = p2Name.substring((p2Name.length() - 1) / 2);
                p2Name = StringUtils.capitalize(p2Name);
                babyName = this.getRandom().nextBoolean() ? p1Name + p2Name : p2Name + p1Name;
            }
            if (egg != null) egg.setCustomName(Text.literal(babyName));
        }
        // increase reproduction counter
        this.addReproCount();
        mate.addReproCount();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public PassiveEntity createChild(ServerWorld level, PassiveEntity mob) {
        TameableDragon offspring = DMEntities.DRAGON.create(level);
        if (this.getBreed() != null) offspring.setBreed(this.getBreed());
        return offspring;
    }

    @Override
    public boolean canAttackWithOwner(LivingEntity target, LivingEntity owner) {
        return !(target instanceof TameableEntity tameable) || !Objects.equals(tameable.getOwner(), owner);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !this.isHatchling() && !this.hasControllingPassenger() && super.canTarget(target);
    }

    /**
     * For vehicles, the first passenger is generally considered the controller and "drives" the vehicle. For example,
     * Pigs, Horses, and Boats are generally "steered" by the controlling passenger.
     */
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity driver && this.isOwner(driver) ? driver : null;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (passenger instanceof PlayerEntity) {
            passenger.setYaw(this.getYaw());
            passenger.setPitch(this.getPitch());
        }
        if (this.hasLocalDriver()) {
            MountControlsMessenger.sendControlsMessage();
            MountCameraManager.onDragonMount();
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        if (this.hasLocalDriver()) MountCameraManager.onDragonDismount();
        super.removePassenger(passenger);
    }

    @Override
    protected void updatePassengerPosition(Entity ridden, PositionUpdater pCallback) {
        if (this.hasPassenger(ridden)) {
            Vec3d rePos = new Vec3d(0, this.getMountedHeightOffset() + ridden.getHeightOffset(), this.getScaleFactor())
                    .rotateY((float) Math.toRadians(-this.bodyYaw))
                    .add(this.getPos());
            pCallback.accept(ridden, rePos.x, rePos.y, rePos.z);
            // fix rider rotation
            if (this.getFirstPassenger() instanceof LivingEntity) {
                ridden.prevPitch = ridden.getPitch();
                ridden.prevYaw = ridden.getYaw();
                ridden.setBodyYaw(this.bodyYaw);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        Entity srcEnt = src.getAttacker();
        if (srcEnt != null && (srcEnt == this || this.hasPassenger(srcEnt))) return true;
        if (this.getBreed() != null) return this.getBreed().immunities().contains(src.getTypeRegistryEntry());
        return super.isInvulnerableTo(src);
    }

    /**
     * Returns the entity's health relative to the maximum health.
     *
     * @return health normalized between 0 and 1
     */
    public float getHealthFraction() {
        return this.getHealth() / this.getMaxHealth();
    }

    public int getMaxDeathTime() {
        return 120;
    }

    /**
     * Public wrapper for protected final setScale(), used by DragonLifeStageHelper.
     */
    @Override
    public void calculateDimensions() {
        double posXTmp = this.getX();
        double posYTmp = this.getY();
        double posZTmp = this.getZ();
        boolean onGroundTmp = this.isOnGround();
        super.calculateDimensions();
        // workaround for a vanilla bug; the position is apparently not set correcty
        // after changing the entity size, causing asynchronous server/client positioning
        this.setPosition(posXTmp, posYTmp, posZTmp);
        // otherwise, setScale stops the dragon from landing while it is growing
        this.setOnGround(onGroundTmp);
    }

    @Override
    public EntityDimensions getDimensions(EntityPose poseIn) {
        float height = this.isInSittingPose() ? 2.15f : BASE_HEIGHT;
        float scale = this.getScaleFactor();
        return new EntityDimensions(BASE_WIDTH * scale, height * scale, false);
    }

    @Override
    public int getBreedingAge() {
        return this.breedingAge;
    }

    public void updateAgeProgress() {
        // no reason to recalculate this value several times per tick/frame...
        float growth = -BASE_GROWTH_TIME;
        if (this.getBreed() != null) growth = -this.getBreed().growthTime();
        float min = Math.min(this.getBreedingAge(), 0);
        this.ageProgress = 1 - (min / growth);
    }

    public float getAgeProgress() {
        return this.ageProgress;
    }

    /**
     * Updates properties/attributes/traits of dragons based on the current age scale.
     * Also syncs the current age to the client.
     * Called at an interval (of ticks) described by {@link TameableDragon#AGE_UPDATE_INTERVAL}
     */
    @SuppressWarnings("ConstantConditions")
    private void updateAgeProperties() {
        this.setBreedingAge(this.dataTracker.get(DATA_AGE));
        this.updateAgeProgress();
        this.calculateDimensions();
        this.setStepHeight(Math.max(2 * this.getAgeProgress(), 1));
        // update attributes and health only on the server
        if (this.isServer()) {
            // health does not update on modifier application, so have to store the health frac first
            float healthFrac = this.getHealthFraction();
            // negate modifier value since the operation is as follows: base_value += modifier * base_value
            double modValue = -(1d - Math.max(this.getAgeProgress(), 0.1));
            EntityAttributeModifier mod = new EntityAttributeModifier(SCALE_MODIFIER_UUID, "Dragon size modifier", modValue, EntityAttributeModifier.Operation.MULTIPLY_BASE);
            for (EntityAttribute attribute : new EntityAttribute[]{GENERIC_MAX_HEALTH, GENERIC_ATTACK_DAMAGE,}) {// avoid duped code
                EntityAttributeInstance instance = this.getAttributeInstance(attribute);
                instance.removeModifier(mod);
                instance.addTemporaryModifier(mod);
            }
            // restore health fraction
            this.setHealth(healthFrac * this.getMaxHealth());
        }
    }

    public boolean isHatchling() {
        return this.getAgeProgress() < 0.5f;
    }

    public boolean isJuvenile() {
        return this.getAgeProgress() >= 0.5f && this.getAgeProgress() < 1f;
    }

    public boolean isAdult() {
        return this.getAgeProgress() >= 1f;
    }

    @Override
    public boolean isBaby() {
        return !this.isAdult();
    }

    @Override
    public void setBaby(boolean baby) {
        int growth = -BASE_GROWTH_TIME;
        if (this.getBreed() != null) growth = -this.getBreed().growthTime();
        this.setBreedingAge(baby ? growth : 0);
        this.dataTracker.set(DATA_AGE, this.breedingAge);
    }

    @Override
    public void growUp(int p_146741_, boolean p_146742_) {
        super.growUp(p_146741_, p_146742_);
        this.dataTracker.set(DATA_AGE, this.getBreedingAge());
    }

    // simple helper method to determine if we're on the server thread.
    public boolean isServer() {
        return !this.getWorld().isClient;
    }

    public DragonAnimator getAnimator() {
        return this.animator;
    }

    @Override
    public boolean canBreatheInWater() {
        if (this.getBreed() == null) return super.canBreatheInWater();
        return this.getBreed().immunities().contains(this.getDamageSources().drown().getTypeRegistryEntry());
    }

    @Override
    public boolean isFireImmune() {
        if (super.isFireImmune()) return true;
        if (this.getBreed() == null) return false;
        return this.getBreed().immunities().contains(this.getDamageSources().onFire().getTypeRegistryEntry());
    }

    @Override
    protected void applyMovementEffects(BlockPos pos) {
        super.applyMovementEffects(pos);
        for (Ability ability : this.getAbilities()) ability.onMove(this);
    }

    @Override
    public boolean isInsideWall() {
        if (this.noClip) return false;
        else {// Reduce suffocation risks. They're fat and clusmy.
            Box collider = this.getBoundingBox().contract(this.getWidth() * 0.2f);
            return BlockPos.stream(collider).anyMatch((pos) -> {
                BlockState state = this.getWorld().getBlockState(pos);
                return !state.isAir() && state.shouldSuffocate(this.getWorld(), pos) && VoxelShapes.matchesAnywhere(state.getCollisionShape(this.getWorld(), pos).offset(pos.getX(), pos.getY(), pos.getZ()), VoxelShapes.cuboid(collider), BooleanBiFunction.AND);
            });
        }
    }

    @Override
    public Vec3d getClientCameraPosVec(float p_20309_) {
        return new Vec3d(this.getX(), this.getY() + this.getHeight(), this.getZ());
    }

    public boolean hasLocalDriver() {
        return this.getControllingPassenger() instanceof PlayerEntity p && p.isMainPlayer();
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }
}