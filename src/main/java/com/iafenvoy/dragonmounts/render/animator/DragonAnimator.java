package com.iafenvoy.dragonmounts.render.animator;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.util.CircularBuffer;
import com.iafenvoy.dragonmounts.util.LerpedFloat;
import net.minecraft.util.math.MathHelper;

/**
 * Animation control class to put useless reptiles in motion.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class DragonAnimator {
    // constants
    protected final TameableDragon dragon;

    // entity parameters
    protected float partialTicks;
    protected float moveTime;
    protected float moveSpeed;
    protected float lookYaw;
    protected float lookPitch;
    protected double prevRenderYawOffset;
    protected double yawAbs;

    // timing vars
    protected float sit;
    protected float speed;

    // timing interp vars
    protected final LerpedFloat animTimer = new LerpedFloat();
    protected final LerpedFloat groundTimer = new LerpedFloat.Clamped(1, 0, 1);
    protected final LerpedFloat flutterTimer = LerpedFloat.unit();
    protected final LerpedFloat walkTimer = LerpedFloat.unit();
    protected final LerpedFloat sitTimer = LerpedFloat.unit();
    protected final LerpedFloat jawTimer = LerpedFloat.unit();
    protected final LerpedFloat speedTimer = new LerpedFloat.Clamped(1, 0, 1);

    // trails
    protected boolean initTrails = false;
    protected final CircularBuffer yTrail = new CircularBuffer(8);
    protected final CircularBuffer yawTrail = new CircularBuffer(16);
    protected final CircularBuffer pitchTrail = new CircularBuffer(16);

    // model flags
    protected boolean onGround;

    public DragonAnimator(TameableDragon dragon) {
        this.dragon = dragon;
    }

    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public void setMovement(float moveTime, float moveSpeed) {
        this.moveTime = moveTime;
        this.moveSpeed = moveSpeed;
    }

    public void setLook(float lookYaw, float lookPitch) {
        // don't twist the neck
        this.lookYaw = MathHelper.clamp(lookYaw, -120, 120);
        this.lookPitch = MathHelper.clamp(lookPitch, -90, 90);
    }

    public void tick() {
        this.setOnGround(!this.dragon.isInAir());

        // init trails
        if (!this.initTrails) {
            this.yTrail.fill((float) this.dragon.getY());
            this.yawTrail.fill(this.dragon.bodyYaw);
            this.pitchTrail.fill(this.getModelPitch());
            this.initTrails = true;
        }

        // don't move anything during death sequence
        if (this.dragon.getHealth() <= 0) {
            this.animTimer.sync();
            this.groundTimer.sync();
            this.flutterTimer.sync();
            this.walkTimer.sync();
            this.sitTimer.sync();
            this.jawTimer.sync();
            return;
        }

        float speedMax = 0.05f;
        float xD = (float) this.dragon.getX() - (float) this.dragon.prevX;
        float yD = (float) this.dragon.getY() - (float) this.dragon.prevY;
        float zD = (float) this.dragon.getZ() - (float) this.dragon.prevZ;
        float speedEnt = (xD * xD + zD * zD);
        float speedMulti = MathHelper.clamp(speedEnt / speedMax, 0, 1);

        // update main animation timer
        float animAdd = 0.035f;

        // depend timing speed on movement
        if (!this.onGround) {
            animAdd += (1 - speedMulti) * animAdd;
        }

        this.animTimer.add(animAdd);

        // update ground transition
        float groundVal = this.groundTimer.get();
        if (this.onGround) {
            groundVal *= 0.95f;
            groundVal += 0.08f;
        } else {
            groundVal -= 0.1f;
        }
        this.groundTimer.set(groundVal);

        // update flutter transition
        boolean flutterFlag = !this.onGround && (this.dragon.horizontalCollision || yD > -0.1 || speedEnt < speedMax);
        this.flutterTimer.add(flutterFlag ? 0.1f : -0.1f);

        // update walking transition
        boolean walkFlag = this.moveSpeed > 0.1 && !this.dragon.isInSittingPose();
        float walkVal = 0.1f;
        this.walkTimer.add(walkFlag ? walkVal : -walkVal);

        // update sitting transisiton
        float sitVal = this.sitTimer.get();
        sitVal += this.dragon.isInSittingPose() ? 0.1f : -0.1f;
        sitVal *= 0.95f;
        this.sitTimer.set(sitVal);

        // TODO: find better attack animation method
//        int ticksSinceLastAttack = dragon.getTicksSince
//
//        boolean jawFlag = (ticksSinceLastAttack >= 0 && ticksSinceLastAttack < JAW_OPENING_TIME_FOR_ATTACK);
//        jawTimer.add(jawFlag? 0.2f : -0.2f);

        // update speed transition
        boolean speedFlag = speedEnt > speedMax || this.dragon.isNearGround();
        float speedValue = 0.05f;
        this.speedTimer.add(speedFlag ? speedValue : -speedValue);

        // update trailers
        double yawDiff = this.dragon.bodyYaw - this.prevRenderYawOffset;
        this.prevRenderYawOffset = this.dragon.bodyYaw;

        // filter out 360 degrees wrapping
        if (yawDiff < 180 && yawDiff > -180) this.yawAbs += yawDiff;

        this.yTrail.update((float) this.dragon.getY());
        this.yawTrail.update((float) -this.yawAbs);
        this.pitchTrail.update(this.getModelPitch());
    }

    public float getModelPitch() {
        return this.getModelPitch(this.partialTicks);
    }

    public float getModelPitch(float pt) {
        float pitchMovingMax = 90;
        float pitchMoving = MathHelper.clamp(this.yTrail.get(pt, 5, 0) * 10, -pitchMovingMax, pitchMovingMax);
        float pitchHover = 60;
        return terpSmoothStep(pitchHover, pitchMoving, this.speed);
    }

    @SuppressWarnings("SameReturnValue")
    public float getModelOffsetX() {
        return 0;
    }

    public float getModelOffsetY() {
        return 1.5f + (-this.sit * 0.6f);
    }

    public float getModelOffsetZ() {
        return -1.5f;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    private static float terpSmoothStep(float a, float b, float x) {
        if (x <= 0) {
            return a;
        }
        if (x >= 1) {
            return b;
        }
        x = x * x * (3 - 2 * x);
        return a * (1 - x) + b * x;
    }

    private static final float[][] CR = {
            {-0.5f, 1.5f, -1.5f, 0.5f},
            {1.0f, -2.5f, 2.0f, -0.5f},
            {-0.5f, 0.0f, 0.5f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f}
    };

    // http://www.java-gaming.org/index.php?topic=24122.0
    private static void terpCatmullRomSpline(float x, float[] result, float[]... knots) {
        int nknots = knots.length;
        int nspans = nknots - 3;
        int knot = 0;
        if (nspans < 1) {
            throw new IllegalArgumentException("Spline has too few knots");
        }
        x = MathHelper.clamp(x, 0, 0.9999f) * nspans;

        int span = (int) x;
        if (span >= nknots - 3) {
            span = nknots - 3;
        }

        x -= span;
        knot += span;

        int dimension = result.length;
        for (int i = 0; i < dimension; i++) {
            float knot0 = knots[knot][i];
            float knot1 = knots[knot + 1][i];
            float knot2 = knots[knot + 2][i];
            float knot3 = knots[knot + 3][i];

            float c3 = CR[0][0] * knot0 + CR[0][1] * knot1 + CR[0][2] * knot2 + CR[0][3] * knot3;
            float c2 = CR[1][0] * knot0 + CR[1][1] * knot1 + CR[1][2] * knot2 + CR[1][3] * knot3;
            float c1 = CR[2][0] * knot0 + CR[2][1] * knot1 + CR[2][2] * knot2 + CR[2][3] * knot3;
            float c0 = CR[3][0] * knot0 + CR[3][1] * knot1 + CR[3][2] * knot2 + CR[3][3] * knot3;

            result[i] = ((c3 * x + c2) * x + c1) * x + c0;
        }
    }
}
