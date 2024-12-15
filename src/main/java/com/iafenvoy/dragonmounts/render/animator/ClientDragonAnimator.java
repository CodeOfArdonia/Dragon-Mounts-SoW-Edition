package com.iafenvoy.dragonmounts.render.animator;

import com.iafenvoy.dragonmounts.DMConstants;
import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.dragon.breed.DragonBreed;
import com.iafenvoy.dragonmounts.render.model.DragonModel;
import com.iafenvoy.dragonmounts.render.util.ModelPartAccess;
import com.iafenvoy.dragonmounts.util.DMMath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.function.Supplier;

/**
 * Animation control class to put useless reptiles in motion.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
@SuppressWarnings("DataFlowIssue")
public class ClientDragonAnimator extends DragonAnimator {
    // timing vars
    private float animBase;
    private float cycleOfs;
    private float anim;
    private float ground;
    private float flutter;
    private float walk;
    private float jaw;

    // model flags
    private boolean wingsDown;

    // animation parameters
    private final float[] wingArm = new float[3];
    private final float[] wingForearm = new float[3];
    private final float[] wingArmFlutter = new float[3];
    private final float[] wingForearmFlutter = new float[3];
    private final float[] wingArmGlide = new float[3];
    private final float[] wingForearmGlide = new float[3];
    private final float[] wingArmGround = new float[3];
    private final float[] wingForearmGround = new float[3];

    // final X rotation angles for ground
    private final float[] xGround = {0, 0, 0, 0};

    // X rotation angles for ground
    // 1st dim - front, hind
    // 2nd dim - thigh, crus, foot, toe
    private final float[][] xGroundStand = {
            {0.8f, -1.5f, 1.3f, 0},
            {-0.3f, 1.5f, -0.2f, 0},
    };
    private final float[][] xGroundSit = {
            {0.3f, -1.8f, 1.8f, 0},
            {-0.8f, 1.8f, -0.9f, 0},
    };

    // X rotation angles for walking
    // 1st dim - animation keyframe
    // 2nd dim - front, hind
    // 3rd dim - thigh, crus, foot, toe
    private final float[][][] xGroundWalk = {{
            {0.4f, -1.4f, 1.3f, 0},    // move down and forward
            {0.1f, 1.2f, -0.5f, 0}     // move back
    }, {
            {1.2f, -1.6f, 1.3f, 0},    // move back
            {-0.3f, 2.1f, -0.9f, 0.6f} // move up and forward
    }, {
            {0.9f, -2.1f, 1.8f, 0.6f}, // move up and forward
            {-0.7f, 1.4f, -0.2f, 0}    // move down and forward
    }};

    // final X rotation angles for walking
    private final float[] xGroundWalk2 = {0, 0, 0, 0};

    // Y rotation angles for ground, thigh only
    private final float[] yGroundStand = {-0.25f, 0.25f};
    private final float[] yGroundSit = {0.1f, 0.35f};
    private final float[] yGroundWalk = {-0.1f, 0.1f};

    // X rotation angles for air
    // 1st dim - front, hind
    // 2nd dim - thigh, crus, foot, toe
    private final float[][] xAirAll = {{0, 0, 0, 0}, {0, 0, 0, 0}};

    // Y rotation angles for air, thigh only
    private final float[] yAirAll = {-0.1f, 0.1f};

    public ClientDragonAnimator(TameableDragon dragon) {
        super(dragon);
    }

    /**
     * Applies the animations on the model. Called every frame before the model
     * is rendered.
     *
     * @param model model to animate
     */
    public void animate(DragonModel model) {
        this.anim = this.animTimer.get(this.partialTicks);
        this.ground = this.groundTimer.get(this.partialTicks);
        this.flutter = this.flutterTimer.get(this.partialTicks);
        this.walk = this.walkTimer.get(this.partialTicks);
        this.sit = this.sitTimer.get(this.partialTicks);
        this.jaw = this.jawTimer.get(this.partialTicks);
        this.speed = this.speedTimer.get(this.partialTicks);

        this.animBase = this.anim * ((float) Math.PI) * 2;
        this.cycleOfs = MathHelper.sin(this.animBase - 1) + 1;

        // check if the wings are moving down and trigger the event
        boolean newWingsDown = this.cycleOfs > 1;
        if (newWingsDown && !this.wingsDown && this.flutter != 0) this.dragon.onWingsDown(this.speed);
        this.wingsDown = newWingsDown;

        // update flags
        model.back.visible = !this.dragon.isSaddled();

        this.cycleOfs = (this.cycleOfs * this.cycleOfs + this.cycleOfs * 2) * 0.05f;

        // reduce up/down amplitude
        this.cycleOfs *= MathHelper.clampedLerp(0.5f, 1, this.flutter);
        this.cycleOfs *= MathHelper.clampedLerp(1, 0.5f, this.ground);

        // animate body parts
        this.animHeadAndNeck(model);
        this.animTail(model);
        this.animWings(model);
        this.animLegs(model);
    }

    protected void animHeadAndNeck(DragonModel model) {
        model.neck.setPivot(0, 14, -8);
        model.neck.setAngles(0, 0, 0);

        float health = this.dragon.getHealthFraction();
        float neckSize;

        for (int i = 0; i < model.neckProxy.length; i++) {
            float vertMulti = (i + 1) / (float) model.neckProxy.length;

            float baseRotX = MathHelper.cos((float) i * 0.45f + this.animBase) * 0.15f;
            baseRotX *= MathHelper.clampedLerp(0.2f, 1, this.flutter);
            baseRotX *= MathHelper.clampedLerp(1, 0.2f, this.sit);
            float ofsRotX = MathHelper.sin(vertMulti * ((float) Math.PI) * 0.9f) * 0.75f;

            // basic up/down movement
            model.neck.pitch = baseRotX;
            // reduce rotation when on ground
            model.neck.pitch *= terpSmoothStep(1, 0.5f, this.walk);
            // flex neck down when hovering
            model.neck.pitch += (1 - this.speed) * vertMulti;
            // lower neck on low health
            model.neck.pitch -= MathHelper.clampedLerp(0, ofsRotX, this.ground * health);
            // use looking yaw
            model.neck.yaw = (float) Math.toRadians(this.lookYaw) * vertMulti * this.speed;

            // update scale
            float v = MathHelper.clampedLerp(1.6f, 1, vertMulti);
            ((ModelPartAccess) (Object) model.neck).setRenderScale(v, v, 0.6f);

            // hide the first and every second scale
            model.neckScale.visible = i % 2 != 0 || i == 0;

            // update proxy
            model.neckProxy[i].update();

            // move next proxy behind the current one
            neckSize = DragonModel.NECK_SIZE * ((ModelPartAccess) (Object) model.neck).getZScale() - 1.4f;
            model.neck.pivotX -= MathHelper.sin(model.neck.yaw) * MathHelper.cos(model.neck.pitch) * neckSize;
            model.neck.pivotY += MathHelper.sin(model.neck.pitch) * neckSize;
            model.neck.pivotZ -= MathHelper.cos(model.neck.yaw) * MathHelper.cos(model.neck.pitch) * neckSize;
        }

        model.head.pitch = (float) Math.toRadians(this.lookPitch) + (1 - this.speed);
        model.head.yaw = model.neck.yaw;
        model.head.roll = model.neck.roll * 0.2f;

        model.head.pivotX = model.neck.pivotX;
        model.head.pivotY = model.neck.pivotY;
        model.head.pivotZ = model.neck.pivotZ;

        model.jaw.pitch = this.jaw * 0.75f;
        model.jaw.pitch += (1 - MathHelper.sin(this.animBase)) * 0.1f * this.flutter;

        if (this.dragon.isAttacking()) {
            Random random = this.dragon.getRandom();
            final Supplier<Double> R = () -> random.nextDouble() / 2 - 0.25;
            Vec3d headOffset = new Vec3d(model.head.pivotX, model.head.pivotY, model.head.pivotZ);
            Vec3d headVec = DMMath.getRotationVectorUnit((float) Math.toDegrees(Math.atan(headOffset.y / headOffset.z)), this.dragon.getYaw()).multiply(headOffset.length());
            Vec3d pos = this.dragon.getPos().add(headVec.multiply(this.dragon.getScaleFactor() / 8));
            if (this.dragon.isOnGround()) pos = pos.add(0, 2.5, 0);
            Vec3d unit = DMMath.getRotationVectorUnit(this.dragon.getPitch() + model.head.pitch, this.dragon.getYaw() + model.head.yaw);
            DragonBreed breed = this.dragon.getBreed();
            DMConstants.shouldForceParticleSpeed = true;
            for (int i = 0; i < 5; i++)
                MinecraftClient.getInstance().world.addParticle(breed.dustParticleFor(random, this.dragon.getScaleFactor() * 3), pos.x, pos.y, pos.z, unit.x + R.get(), unit.y + R.get(), unit.z + R.get());
            DMConstants.shouldForceParticleSpeed = false;
        }
    }

    protected void animWings(DragonModel model) {
        // move wings slower while sitting
        float aSpeed = this.sit > 0 ? 0.6f : 1;

        // animation speeds
        float a1 = this.animBase * aSpeed * 0.35f;
        float a2 = this.animBase * aSpeed * 0.5f;
        float a3 = this.animBase * aSpeed * 0.75f;

        if (this.ground < 1) {
            // fluttering
            this.wingArmFlutter[0] = 0.125f - MathHelper.cos(this.animBase) * 0.2f;
            this.wingArmFlutter[1] = 0.25f;
            this.wingArmFlutter[2] = (MathHelper.sin(this.animBase) + 0.125f) * 0.8f;

            this.wingForearmFlutter[0] = 0;
            this.wingForearmFlutter[1] = -this.wingArmFlutter[1] * 2;
            this.wingForearmFlutter[2] = -(MathHelper.sin(this.animBase + 2) + 0.5f) * 0.75f;

            // gliding
            this.wingArmGlide[0] = -0.25f - MathHelper.cos(this.animBase * 2) * MathHelper.cos(this.animBase * 1.5f) * 0.04f;
            this.wingArmGlide[1] = 0.25f;
            this.wingArmGlide[2] = 0.35f + MathHelper.sin(this.animBase) * 0.05f;

            this.wingForearmGlide[0] = 0;
            this.wingForearmGlide[1] = -this.wingArmGlide[1] * 2;
            this.wingForearmGlide[2] = -0.25f + (MathHelper.sin(this.animBase + 2) + 0.5f) * 0.05f;
        }

        if (this.ground > 0) {
            // standing
            this.wingArmGround[0] = 0;
            this.wingArmGround[1] = 1.4f - MathHelper.sin(a1) * MathHelper.sin(a2) * 0.02f;
            this.wingArmGround[2] = 0.8f + MathHelper.sin(a2) * MathHelper.sin(a3) * 0.05f;

            // walking
            this.wingArmGround[1] += MathHelper.sin(this.moveTime * 0.5f) * 0.02f * this.walk;
            this.wingArmGround[2] += MathHelper.cos(this.moveTime * 0.5f) * 0.05f * this.walk;

            this.wingForearmGround[0] = 0;
            this.wingForearmGround[1] = -this.wingArmGround[1] * 2;
            this.wingForearmGround[2] = 0;
        }

        // interpolate between fluttering and gliding
        slerpArrays(this.wingArmGlide, this.wingArmFlutter, this.wingArm, this.flutter);
        slerpArrays(this.wingForearmGlide, this.wingForearmFlutter, this.wingForearm, this.flutter);

        // interpolate between flying and grounded
        slerpArrays(this.wingArm, this.wingArmGround, this.wingArm, this.ground);
        slerpArrays(this.wingForearm, this.wingForearmGround, this.wingForearm, this.ground);

        // apply angles
        mirrorRotate(model.wingArms[0], model.wingArms[1], this.wingArm[0], this.wingArm[1], this.wingArm[2]);
//        model.wingArm.xRot += 1 - speed;

        mirrorRotate(model.wingForearms[0], model.wingForearms[1], this.wingForearm[0], this.wingForearm[1], this.wingForearm[2]);


        // interpolate between folded and unfolded wing angles
        float[] yFold = new float[]{2.7f, 2.8f, 2.9f, 3.0f};
        float[] yUnfold = new float[]{0.1f, 0.9f, 1.7f, 2.5f};

        // set wing finger angles
        float rotX = 0;
        float rotYOfs = MathHelper.sin(a1) * MathHelper.sin(a2) * 0.03f;
        float rotYMulti = 1;

        for (int i = 0; i < model.wingFingers[0].length; i++) {
            mirrorRotate(model.wingFingers[0][i],
                    model.wingFingers[1][i],
                    rotX += 0.005f,
                    terpSmoothStep(yUnfold[i], yFold[i] + rotYOfs * rotYMulti, this.ground),
                    0);

            rotYMulti -= 0.2f;
        }
    }

    @SuppressWarnings("UnusedAssignment")
    protected void animTail(DragonModel model) {
        model.tail.pivotX = 0;
        model.tail.pivotY = 16;
        model.tail.pivotZ = 62;

        model.tail.pitch = 0;
        model.tail.yaw = 0;
        model.tail.roll = 0;

        float rotXStand = 0;
        float rotYStand = 0;
        float rotXSit = 0;
        float rotYSit = 0;
        float rotXAir = 0;
        float rotYAir = 0;

        for (int i = 0; i < model.tailProxy.length; i++) {
            float vertMulti = (i + 1) / (float) model.tailProxy.length;

            // idle
            float amp = 0.1f + i / (model.tailProxy.length * 2f);

            rotXStand = (i - model.tailProxy.length * 0.6f) * -amp * 0.4f;
            rotXStand += (MathHelper.sin(this.animBase * 0.2f) * MathHelper.sin(this.animBase * 0.37f) * 0.4f * amp - 0.1f) * (1 - this.sit);
            rotXSit = rotXStand * 0.8f;

            rotYStand = (rotYStand + MathHelper.sin(i * 0.45f + this.animBase * 0.5f)) * amp * 0.4f;
            rotYSit = MathHelper.sin(vertMulti * ((float) Math.PI)) * ((float) Math.PI) * 1.2f - 0.5f; // curl to the left

            rotXAir -= MathHelper.sin(i * 0.45f + this.animBase) * 0.04f * MathHelper.clampedLerp(0.3f, 1, this.flutter);

            // interpolate between sitting and standing
            model.tail.pitch = MathHelper.clampedLerp(rotXStand, rotXSit, this.sit);
            model.tail.yaw = MathHelper.clampedLerp(rotYStand, rotYSit, this.sit);

            // interpolate between flying and grounded
            model.tail.pitch = MathHelper.clampedLerp(rotXAir, model.tail.pitch, this.ground);
            model.tail.yaw = MathHelper.clampedLerp(rotYAir, model.tail.yaw, this.ground);

            // body movement
            float angleLimit = 160 * vertMulti;
            float yawOfs = MathHelper.clamp(this.yawTrail.get(this.partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
            float pitchOfs = MathHelper.clamp(this.pitchTrail.get(this.partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);

            model.tail.pitch += (float) Math.toRadians(pitchOfs);
            model.tail.pitch -= (1 - this.speed) * vertMulti * 2;
            model.tail.yaw += (float) Math.toRadians(180 - yawOfs);

            if (model.tailHornRight != null) {
                // display horns near the tip
                boolean atIndex = i > model.tailProxy.length - 7 && i < model.tailProxy.length - 3;
                model.tailHornLeft.visible = model.tailHornRight.visible = atIndex;
            }

            // update scale
            float neckScale = MathHelper.clampedLerp(1.5f, 0.3f, vertMulti);
            ((ModelPartAccess) (Object) model.tail).setRenderScale(neckScale, neckScale, neckScale);

            // update proxy
            model.tailProxy[i].update();

            // move next proxy behind the current one
            float tailSize = DragonModel.TAIL_SIZE * ((ModelPartAccess) (Object) model.tail).getZScale() - 0.7f;
            model.tail.pivotY += MathHelper.sin(model.tail.pitch) * tailSize;
            model.tail.pivotZ -= MathHelper.cos(model.tail.yaw) * MathHelper.cos(model.tail.pitch) * tailSize;
            model.tail.pivotX -= MathHelper.sin(model.tail.yaw) * MathHelper.cos(model.tail.pitch) * tailSize;
        }
    }

    protected void animLegs(DragonModel model) {
        // dangling legs for flying
        if (this.ground < 1) {
            float footAirOfs = this.cycleOfs * 0.1f;
            float footAirX = 0.75f + this.cycleOfs * 0.1f;

            this.xAirAll[0][0] = 1.3f + footAirOfs;
            this.xAirAll[0][1] = -(0.7f * this.speed + 0.1f + footAirOfs);
            this.xAirAll[0][2] = footAirX;
            this.xAirAll[0][3] = footAirX * 0.5f;

            this.xAirAll[1][0] = footAirOfs + 0.6f;
            this.xAirAll[1][1] = footAirOfs + 0.8f;
            this.xAirAll[1][2] = footAirX;
            this.xAirAll[1][3] = footAirX * 0.5f;
        }

        // 0 - front leg, right side
        // 1 - hind leg, right side
        // 2 - front leg, left side
        // 3 - hind leg, left side
        for (int i = 0; i < model.legs.length; i++) {
            ModelPart thigh = model.legs[i][0];
            ModelPart crus = model.legs[i][1];
            ModelPart foot = model.legs[i][2];
            ModelPart toe = model.legs[i][3];

            thigh.pivotZ = (i % 2 == 0) ? 4 : 46;

            // final X rotation angles for air
            float[] xAir = this.xAirAll[i % 2];

            // interpolate between sitting and standing
            slerpArrays(this.xGroundStand[i % 2], this.xGroundSit[i % 2], this.xGround, this.sit);

            // align the toes so they're always horizontal on the ground
            this.xGround[3] = -(this.xGround[0] + this.xGround[1] + this.xGround[2]);

            // apply walking cycle
            if (this.walk > 0) {
                // interpolate between the keyframes, based on the cycle
                splineArrays(this.moveTime * 0.2f, i > 1, this.xGroundWalk2,
                        this.xGroundWalk[0][i % 2], this.xGroundWalk[1][i % 2], this.xGroundWalk[2][i % 2]);
                // align the toes so they're always horizontal on the ground
                this.xGroundWalk2[3] -= this.xGroundWalk2[0] + this.xGroundWalk2[1] + this.xGroundWalk2[2];

                slerpArrays(this.xGround, this.xGroundWalk2, this.xGround, this.walk);
            }

            float yAir = this.yAirAll[i % 2];
            float yGround;

            // interpolate between sitting and standing
            yGround = terpSmoothStep(this.yGroundStand[i % 2], this.yGroundSit[i % 2], this.sit);

            // interpolate between standing and walking
            yGround = terpSmoothStep(yGround, this.yGroundWalk[i % 2], this.walk);

            // interpolate between flying and grounded
            thigh.yaw = terpSmoothStep(yAir, yGround, this.ground);
            thigh.pitch = terpSmoothStep(xAir[0], this.xGround[0], this.ground);
            crus.pitch = terpSmoothStep(xAir[1], this.xGround[1], this.ground);
            foot.pitch = terpSmoothStep(xAir[2], this.xGround[2], this.ground);
            toe.pitch = terpSmoothStep(xAir[3], this.xGround[3], this.ground);

            if (i > 1) thigh.yaw *= -1;
        }
    }

    private static void mirrorRotate(ModelPart rightLimb, ModelPart leftLimb, float xRot, float yRot, float zRot) {
        rightLimb.pitch = xRot;
        rightLimb.yaw = yRot;
        rightLimb.roll = zRot;
        leftLimb.pitch = xRot;
        leftLimb.yaw = -yRot;
        leftLimb.roll = -zRot;
    }

    private static void slerpArrays(float[] a, float[] b, float[] c, float x) {
        if (a.length != b.length || b.length != c.length) {
            throw new IllegalArgumentException();
        }

        if (x <= 0) {
            System.arraycopy(a, 0, c, 0, a.length);
            return;
        }
        if (x >= 1) {
            System.arraycopy(b, 0, c, 0, a.length);
            return;
        }

        for (int i = 0; i < c.length; i++)
            c[i] = terpSmoothStep(a[i], b[i], x);
    }

    private static float terpSmoothStep(float a, float b, float x) {
        if (x <= 0) return a;
        if (x >= 1) return b;
        x = x * x * (3 - 2 * x);
        return a * (1 - x) + b * x;
    }

    private static void splineArrays(float x, boolean shift, float[] result, float[]... nodes) {
        int i1 = (int) x % nodes.length;
        int i2 = (i1 + 1) % nodes.length;
        int i3 = (i1 + 2) % nodes.length;

        float[] a1 = nodes[i1];
        float[] a2 = nodes[i2];
        float[] a3 = nodes[i3];

        float xn = x % nodes.length - i1;

        if (shift) terpCatmullRomSpline(xn, result, a2, a3, a1, a2);
        else terpCatmullRomSpline(xn, result, a1, a2, a3, a1);
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
        if (nspans < 1) throw new IllegalArgumentException("Spline has too few knots");

        x = MathHelper.clamp(x, 0, 0.9999f) * nspans;

        int span = (int) x;
        if (span >= nknots - 3) span = nknots - 3;

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
