package com.iafenvoy.dragonmounts.render.model;

import com.iafenvoy.dragonmounts.dragon.TameableDragon;
import com.iafenvoy.dragonmounts.render.animator.ClientDragonAnimator;
import com.iafenvoy.dragonmounts.render.animator.DragonAnimator;
import com.iafenvoy.dragonmounts.render.util.ModelPartAccess;
import com.iafenvoy.dragonmounts.render.util.ModelPartProxy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * Generic model for all winged tetrapod dragons.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class DragonModel extends EntityModel<TameableDragon> {
    // model constants
    public static final int NECK_SIZE = 10;
    public static final int TAIL_SIZE = 10;
    public static final int VERTS_NECK = 7;
    public static final int VERTS_TAIL = 12;
    public static final int HEAD_OFS = -16;

    // model parts
    public final ModelPart head;
    public final ModelPart neck;
    public final ModelPart neckScale;
    public final ModelPart tail;
    public final ModelPart tailHornLeft;
    public final ModelPart tailHornRight;
    public final ModelPart jaw;
    public final ModelPart body;
    public final ModelPart back;

    // [0][]: right fore  [1][]: right hind  [2][]: left fore  [3][]: left hind
    // [][0]: thigh       [][1]: crus        [][2]: foot       [][3]: toe
    public final ModelPart[][] legs = new ModelPart[4][4];

    // [0]: right  [1]: left
    public final ModelPart[] wingArms;
    public final ModelPart[] wingForearms;

    // [][0]: finger 1  [][1]: finger 2  [][2]: finger 3  [][3]: finger 4
    public final ModelPart[][] wingFingers = new ModelPart[2][4];


    // model attributes
    public final ModelPartProxy[] neckProxy = new ModelPartProxy[VERTS_NECK];
    public final ModelPartProxy[] tailProxy = new ModelPartProxy[VERTS_TAIL];

    public float size;

    public DragonModel(ModelPart root) {
        super(RenderLayer::getEntityCutout);

        this.body = root.getChild("body");
        this.back = this.body.getChild("back");
        this.neck = root.getChild("neck");
        this.neckScale = this.neck.getChild("neck_scale");
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = root.getChild("tail");
        this.tailHornRight = getNullableChild(this.tail, "right_tail_spike");
        this.tailHornLeft = getNullableChild(this.tail, "left_tail_spike");

        ModelPart rightWingArm = root.getChild("right_wing_arm");
        ModelPart leftWingArm = root.getChild("left_wing_arm");
        ModelPart rightWingForearm = rightWingArm.getChild("right_wing_forearm");
        ModelPart leftWingForearm = leftWingArm.getChild("left_wing_forearm");

        this.wingArms = new ModelPart[]{rightWingArm, leftWingArm};
        this.wingForearms = new ModelPart[]{rightWingForearm, leftWingForearm};

        for (int i = 1; i < 5; i++) {
            this.wingFingers[0][i - 1] = rightWingForearm.getChild("right_wing_finger_" + i);
            this.wingFingers[1][i - 1] = leftWingForearm.getChild("left_wing_finger_" + i);
        }

        for (int i = 0; i < this.legs.length; i++) {
            String[] parts = new String[]{"thigh", "crus", "foot", "toe"};
            ModelPart parent = root;
            for (int j = 0; j < parts.length; j++)
                parent = this.legs[i][j] = parent.getChild((i < 2 ? "right_" : "left_") + (i % 2 == 0 ? "fore_" : "hind_") + parts[j]);
        }

        // initialize model proxies
        for (int i = 0; i < this.neckProxy.length; i++) this.neckProxy[i] = new ModelPartProxy(this.neck);
        for (int i = 0; i < this.tailProxy.length; i++) this.tailProxy[i] = new ModelPartProxy(this.tail);

        if (this.tailHornRight != null)
            //noinspection ConstantConditions
            this.tailHornRight.visible = this.tailHornLeft.visible = false;
    }


    public static TexturedModelData createBodyLayer(Properties properties) {
        ModelData mesh = new ModelData();
        ModelPartData root = mesh.getRoot();

        buildBody(root);
        buildNeck(root);
        buildHead(root);
        buildTail(root, properties);
        buildWings(root);
        buildLegs(root, properties);

        return TexturedModelData.of(mesh, 256, 256);
    }

    private static void buildBody(ModelPartData root) {
        ModelPartData body = root.addChild("body", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-12, 0, -16, 24, 24, 64)
                        .uv(0, 32).cuboid(-1, -6, 10, 2, 6, 12).cuboid(-1, -6, 30, 2, 6, 12),
                ModelTransform.pivot(0, 4, 8));
        body.addChild("back", ModelPartBuilder.create().uv(0, 32).cuboid(-1, -6, -10, 2, 6, 12), ModelTransform.NONE);
    }

    private static void buildNeck(ModelPartData root) {
        ModelPartData neck = root.addChild("neck", ModelPartBuilder.create().uv(112, 88).cuboid(-5, -5, -5, NECK_SIZE, NECK_SIZE, NECK_SIZE), ModelTransform.NONE);
        neck.addChild("neck_scale", ModelPartBuilder.create().uv(0, 0).cuboid(-1, -7, -3, 2, 4, 6), ModelTransform.NONE);
    }

    private static void buildHead(ModelPartData root) {
        ModelPartData head = root.addChild("head", ModelPartBuilder.create()
                        .uv(56, 88).cuboid(-6, -1, -8 + HEAD_OFS, 12, 5, 16) // upper jaw
                        .uv(0, 0).cuboid(-8, -8, 6 + HEAD_OFS, 16, 16, 16) // upper head
                        .uv(48, 0).cuboid(-5, -3, -6 + HEAD_OFS, 2, 2, 4) // nostril
                        .mirrored().cuboid(3, -3, -6 + HEAD_OFS, 2, 2, 4), // nostril
                ModelTransform.NONE);
        addHorns(head);
        head.addChild("jaw", ModelPartBuilder.create().uv(0, 88).cuboid(-6, 0, -16, 12, 4, 16), ModelTransform.pivot(0, 4, 8 + HEAD_OFS));
    }

    private static void addHorns(ModelPartData head) {
        int hornThick = 3;
        int hornLength = 12;

        float hornOfs = -(hornThick / 2f);

        float hornPosX = -5;
        float hornPosY = -8;
        float hornPosZ = 0;

        float hornRotX = 0.523599f;
        float hornRotY = -0.523599f;
        float hornRotZ = 0;

        head.addChild("horn1", ModelPartBuilder.create().uv(28, 32).cuboid(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength),
                ModelTransform.of(hornPosX, hornPosY, hornPosZ, hornRotX, hornRotY, hornRotZ));
        head.addChild("horn2", ModelPartBuilder.create().uv(28, 32).mirrored().cuboid(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength),
                ModelTransform.of(hornPosX * -1, hornPosY, hornPosZ, hornRotX, hornRotY * -1, hornRotZ));
    }

    private static void buildTail(ModelPartData root, Properties properties) {
        ModelPartData tail = root.addChild("tail", ModelPartBuilder.create().uv(152, 88).cuboid(-5, -5, -5, TAIL_SIZE, TAIL_SIZE, TAIL_SIZE), ModelTransform.NONE);
        ModelPartBuilder tailSpikeCube = ModelPartBuilder.create().uv(0, 0).cuboid(-1, -8, -3, 2, 4, 6);
        if (properties.middleTailScales())
            tail.addChild("middle_tail_scale", tailSpikeCube, ModelTransform.NONE);
        else {
            tail.addChild("left_tail_scale", tailSpikeCube, ModelTransform.rotation(0, 0, 0.785398f));
            tail.addChild("right_tail_scale", tailSpikeCube, ModelTransform.rotation(0, 0, -0.785398f));
        }

        if (properties.tailHorns()) addTailSpikes(tail);
    }

    private static void addTailSpikes(ModelPartData tail) {
        int hornThick = 3;
        int hornLength = 32;

        float hornOfs = -(hornThick / 2f);

        float hornPosX = 0;
        float hornPosY = hornOfs;
        float hornPosZ = TAIL_SIZE / 2f;

        float hornRotX = -0.261799f;
        float hornRotY = -2.53073f;
        float hornRotZ = 0;

        tail.addChild("right_tail_spike",
                ModelPartBuilder.create().uv(0, 117).cuboid(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength),
                ModelTransform.of(hornPosX, hornPosY, hornPosZ, hornRotX, hornRotY, hornRotZ));
        tail.addChild("left_tail_spike",
                ModelPartBuilder.create().uv(0, 117).mirrored().cuboid(hornOfs, hornOfs, hornOfs, hornThick, hornThick, hornLength),
                ModelTransform.of(hornPosX * -1, hornPosY, hornPosZ, hornRotX, hornRotY * -1, hornRotZ));
    }

    private static void buildWings(ModelPartData root) {
        buildWing(root, false); // right wing
        buildWing(root, true); // left wing
    }

    private static void buildWing(ModelPartData root, boolean mirror) {
        String direction = mirror ? "left_" : "right_";

        ModelPartBuilder wingArmCube = ModelPartBuilder.create().mirrored(mirror);
        centerMirroredBox(wingArmCube.uv(0, 152), mirror, -28, -3, -3, 28, 6, 6); // bone
        centerMirroredBox(wingArmCube.uv(116, 232), mirror, -28, 0, 2, 28, 0, 24); // skin

        ModelPartBuilder foreArmCube = centerMirroredBox(ModelPartBuilder.create().mirrored(mirror).uv(0, 164), mirror, -48, -2, -2, 48, 4, 4); // bone

        ModelPartBuilder shortSkinCube = ModelPartBuilder.create().mirrored(mirror);
        centerMirroredBox(shortSkinCube.uv(0, 172), mirror, -70, -1, -1, 70, 2, 2); // bone
        centerMirroredBox(shortSkinCube.uv(-49, 176), mirror, -70, 0, 1, 70, 0, 48); // skin
        ModelTransform shortSkinPos = mirrorXPos(-47, 0, 0, mirror);

        ModelPartBuilder lastFingerCube = ModelPartBuilder.create().mirrored(mirror);
        centerMirroredBox(lastFingerCube.uv(0, 172), mirror, -70, -1, -1, 70, 2, 2); // bone
        centerMirroredBox(lastFingerCube.uv(-32, 224), mirror, -70, 0, 1, 70, 0, 32); // shortskin

        ModelPartData arm = root.addChild(direction + "wing_arm", wingArmCube, mirrorXPos(-10, 5, 4, mirror));
        ModelPartData foreArm = arm.addChild(direction + "wing_forearm", foreArmCube, mirrorXPos(-28, 0, 0, mirror));
        for (int j = 1; j < 4; j++) foreArm.addChild(direction + "wing_finger_" + j, shortSkinCube, shortSkinPos);
        foreArm.addChild(direction + "wing_finger_4", lastFingerCube, shortSkinPos);
    }

    private static void buildLegs(ModelPartData root, Properties properties) {
        buildLeg(root, false, properties.thinLegs(), false); // front right
        buildLeg(root, true, properties.thinLegs(), false); // back right
        buildLeg(root, false, properties.thinLegs(), true); // front left
        buildLeg(root, true, properties.thinLegs(), true); // back left
    }

    private static void buildLeg(ModelPartData root, boolean hind, boolean thin, boolean mirror) {
        float baseLength = 26;
        String baseName = (mirror ? "left_" : "right_") + (hind ? "hind_" : "fore_");

        // thigh variables
        float thighPosX = -11;
        float thighPosY = 18;
        float thighPosZ = 4;

        int thighThick = 9 - (thin ? 2 : 0);
        int thighLength = (int) (baseLength * (hind ? 0.9f : 0.77f));

        if (hind) {
            thighThick++;
            thighPosY -= 5;
        }

        float thighOfs = -(thighThick / 2f);

        ModelPartData thigh = root.addChild(baseName + "thigh", ModelPartBuilder.create().uv(112, hind ? 29 : 0).cuboid(thighOfs, thighOfs, thighOfs, thighThick, thighLength, thighThick), mirrorXPos(thighPosX, thighPosY, thighPosZ, mirror));

        // crus variables
        float crusPosX = 0;
        float crusPosY = thighLength + thighOfs;
        float crusPosZ = 0;

        int crusThick = thighThick - 2;
        int crusLength = (int) (baseLength * (hind ? 0.7f : 0.8f));

        if (hind) {
            crusThick--;
            crusLength -= 2;
        }

        float crusOfs = -(crusThick / 2f);

        ModelPartData crus = thigh.addChild(baseName + "crus",
                ModelPartBuilder.create().uv(hind ? 152 : 148, hind ? 29 : 0).cuboid(crusOfs, crusOfs, crusOfs, crusThick, crusLength, crusThick),
                mirrorXPos(crusPosX, crusPosY, crusPosZ, mirror));

        // foot variables
        float footPosX = 0;
        float footPosY = crusLength + (crusOfs / 2f);
        float footPosZ = 0;

        int footWidth = crusThick + 2 + (thin ? 2 : 0);
        int footHeight = 4;
        int footLength = (int) (baseLength * (hind ? 0.67f : 0.34f));

        float footOfsX = -(footWidth / 2f);
        float footOfsY = -(footHeight / 2f);
        float footOfsZ = footLength * -0.75f;

        ModelPartData foot = crus.addChild(baseName + "foot",
                ModelPartBuilder.create().uv(hind ? 180 : 210, hind ? 29 : 0).cuboid(footOfsX, footOfsY, footOfsZ, footWidth, footHeight, footLength),
                mirrorXPos(footPosX, footPosY, footPosZ, mirror));

        // toe variables
        int toeWidth = footWidth;
        int toeHeight = footHeight;
        int toeLength = (int) (baseLength * (hind ? 0.27f : 0.33f));

        float toePosX = 0;
        float toePosY = 0;
        float toePosZ = footOfsZ - (footOfsY / 2f);

        float toeOfsX = -(toeWidth / 2f);
        float toeOfsY = -(toeHeight / 2f);
        float toeOfsZ = -toeLength;

        foot.addChild(baseName + "toe",
                ModelPartBuilder.create().uv(hind ? 215 : 176, hind ? 29 : 0).cuboid(toeOfsX, toeOfsY, toeOfsZ, toeWidth, toeHeight, toeLength),
                mirrorXPos(toePosX, toePosY, toePosZ, mirror));
    }

    @Override
    public void animateModel(TameableDragon dragon, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
        this.size = Math.min(dragon.getScaleFactor(), 1);
        dragon.getAnimator().setPartialTicks(pPartialTick);
    }

    @Override
    public void setAngles(TameableDragon dragon, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        DragonAnimator animator = dragon.getAnimator();
        animator.setLook(pNetHeadYaw, pHeadPitch);
        animator.setMovement(pLimbSwing, pLimbSwingAmount * dragon.getScaleFactor());
        ((ClientDragonAnimator) dragon.getAnimator()).animate(this);
    }

    @Override
    public void render(MatrixStack ps, VertexConsumer vertices, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        this.body.render(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        this.renderHead(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        for (ModelPartProxy proxy : this.neckProxy)
            proxy.render(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        for (ModelPartProxy proxy : this.tailProxy)
            proxy.render(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        this.renderWings(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
        this.renderLegs(ps, vertices, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
    }

    protected void renderHead(MatrixStack ps, VertexConsumer vertices, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        float headScale = 1.4f / (this.size + 0.4f);
        //noinspection DataFlowIssue
        ((ModelPartAccess) (Object) this.head).setRenderScale(headScale, headScale, headScale);
        this.head.render(ps, vertices, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha);
    }

    public void renderWings(MatrixStack ps, VertexConsumer vertices, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        ps.push();
        ps.scale(1.1f, 1.1f, 1.1f);
        this.wingArms[0].render(ps, vertices, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha);
        this.wingArms[1].render(ps, vertices, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha);
        ps.pop();
    }

    protected void renderLegs(MatrixStack ps, VertexConsumer vertices, int packedLight, int packedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
        for (ModelPart[] leg : this.legs)
            leg[0].render(ps, vertices, packedLight, packedOverlay, pRed, pGreen, pBlue, pAlpha);
    }

    private static ModelPartBuilder centerMirroredBox(ModelPartBuilder builder, boolean mirror, float pOriginX, float pOriginY, float pOriginZ, float pDimensionX, float pDimensionY, float pDimensionZ) {
        if (mirror) pOriginX = 0;
        return builder.cuboid(pOriginX, pOriginY, pOriginZ, pDimensionX, pDimensionY, pDimensionZ);
    }

    private static ModelTransform mirrorXPos(float x, float y, float z, boolean mirror) {
        if (mirror) x = -x;
        return ModelTransform.pivot(x, y, z);
    }

    /**
     * Hacky workaround for getting model parts that may or may not exist.
     */
    @Nullable
    private static ModelPart getNullableChild(ModelPart parent, String child) {
        try {
            return parent.getChild(child);
        } catch (NoSuchElementException ignore) {
            return null;
        }
    }

    public record Properties(boolean middleTailScales, boolean tailHorns, boolean thinLegs) {
        public static final Properties STANDARD = new Properties(true, false, false);

        public static final Codec<Properties> CODEC = RecordCodecBuilder.create(func -> func.group(
                Codec.BOOL.optionalFieldOf("middle_tail_scales", true).forGetter(Properties::middleTailScales),
                Codec.BOOL.optionalFieldOf("tail_horns", false).forGetter(Properties::tailHorns),
                Codec.BOOL.optionalFieldOf("thin_legs", false).forGetter(Properties::thinLegs)
        ).apply(func, Properties::new));
    }
}