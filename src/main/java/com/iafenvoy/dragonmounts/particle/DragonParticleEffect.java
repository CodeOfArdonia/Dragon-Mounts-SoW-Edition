package com.iafenvoy.dragonmounts.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class DragonParticleEffect extends ParticleType<DragonParticleEffect> implements ParticleEffect {
    @SuppressWarnings("deprecation")
    public static final Factory<DragonParticleEffect> FACTORY = new Factory<>() {
        @Override
        public DragonParticleEffect read(ParticleType<DragonParticleEffect> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int color = reader.readInt();
            reader.expect(' ');
            float scale = reader.readFloat();
            return new DragonParticleEffect(type, color, scale);
        }

        @Override
        public DragonParticleEffect read(ParticleType<DragonParticleEffect> type, PacketByteBuf buf) {
            return new DragonParticleEffect(buf.readRegistryValue(Registries.PARTICLE_TYPE), buf.readInt(), buf.readFloat());
        }
    };
    public static final Codec<DragonParticleEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            Registries.PARTICLE_TYPE.getCodec().fieldOf("type").forGetter(DragonParticleEffect::getType),
            Codec.INT.fieldOf("color").forGetter(DragonParticleEffect::getColor),
            Codec.FLOAT.fieldOf("scale").forGetter(DragonParticleEffect::getScale)
    ).apply(i, DragonParticleEffect::new));
    private final ParticleType<?> type;
    private final int color;
    private final float scale;

    public DragonParticleEffect(ParticleType<?> type, int color, float scale) {
        super(true, FACTORY);
        this.type = type;
        this.color = color;
        this.scale = scale;
    }

    public int getColor() {
        return this.color;
    }

    public float getScale() {
        return this.scale;
    }

    @NotNull
    @Override
    public ParticleType<?> getType() {
        return this.type;
    }

    @Override
    public void write(@NotNull PacketByteBuf buf) {
        buf.writeRegistryValue(Registries.PARTICLE_TYPE, this.type);
        buf.writeInt(this.color);
        buf.writeFloat(this.scale);
    }

    @NotNull
    @Override
    public String asString() {
        return String.format(Locale.ROOT, "%s color=%d", Registries.PARTICLE_TYPE.getId(this.getType()), this.color);
    }

    @Override
    public Codec<DragonParticleEffect> getCodec() {
        return CODEC;
    }
}