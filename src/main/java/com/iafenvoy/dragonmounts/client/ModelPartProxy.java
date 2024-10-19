package com.iafenvoy.dragonmounts.client;

import com.iafenvoy.dragonmounts.accessors.ModelPartAccess;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;

/**
 * Proxy for a model part that is used to project one model renderer on multiple
 * visible instances.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
@SuppressWarnings("DataFlowIssue")
public class ModelPartProxy {
    // projected parts and part childs
    public final ModelPart part;
    private final List<ModelPartProxy> children;
    // scale multiplier
    public float scaleX = 1;
    public float scaleY = 1;
    public float scaleZ = 1;
    // rotation points
    private float x;
    private float y;
    private float z;
    // rotation angles
    private float xRot;
    private float yRot;
    private float zRot;
    // misc meta data
    private boolean visible;

    /**
     * Constructs a new proxy for the given model part.
     *
     * @param part model part to project on this proxy
     */
    public ModelPartProxy(ModelPart part) {
        this.part = part;

        this.children = part.traverse().skip(1).map(ModelPartProxy::new).toList();

        this.update();
    }

    public void copy(ModelPartProxy other) {
        other.x = this.x;
        other.y = this.y;
        other.z = this.z;

        other.xRot = this.xRot;
        other.yRot = this.yRot;
        other.zRot = this.zRot;

        other.scaleX = this.scaleX;
        other.scaleY = this.scaleY;
        other.scaleZ = this.scaleZ;

        other.visible = this.visible;

        if (this.children.size() != other.children.size())
            throw new IllegalArgumentException("Proxies do not share the same children.");
        for (int i = 0; i < this.children.size(); i++) this.children.get(i).copy(other.children.get(i));
    }

    /**
     * Saves the properties of the model part to this proxy with the default
     * rendering scale.
     */
    public final void update() {
        this.x = this.part.pivotX;
        this.y = this.part.pivotY;
        this.z = this.part.pivotZ;

        this.xRot = this.part.pitch;
        this.yRot = this.part.yaw;
        this.zRot = this.part.roll;

        ModelPartAccess mixinPart = (ModelPartAccess) (Object) this.part;

        this.scaleX = mixinPart.getXScale();
        this.scaleY = mixinPart.getYScale();
        this.scaleZ = mixinPart.getZScale();

        this.visible = this.part.visible;

        for (ModelPartProxy child : this.children) child.update();
    }

    /**
     * Restores the properties from this proxy to the model part.
     */
    public final void apply() {
        this.part.pivotX = this.x;
        this.part.pivotY = this.y;
        this.part.pivotZ = this.z;

        this.part.pitch = this.xRot;
        this.part.yaw = this.yRot;
        this.part.roll = this.zRot;

        ((ModelPartAccess) (Object) this.part).setRenderScale(this.scaleX, this.scaleY, this.scaleZ);

        this.part.visible = this.visible;

        for (ModelPartProxy child : this.children) child.apply();
    }

    public void render(MatrixStack ps, VertexConsumer vertices, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        this.apply();
        this.part.render(ps, vertices, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }
}