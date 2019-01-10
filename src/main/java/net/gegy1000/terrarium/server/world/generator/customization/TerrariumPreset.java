package net.gegy1000.terrarium.server.world.generator.customization;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TerrariumPreset {
    private final String name;
    private final ResourceLocation worldType;
    private final ResourceLocation icon;
    private final GenerationSettings properties;

    public TerrariumPreset(String name, ResourceLocation worldType, ResourceLocation icon, GenerationSettings properties) {
        this.name = name;
        this.worldType = worldType;
        this.icon = new ResourceLocation(icon.getNamespace(), "textures/preset/" + icon.getNamespace() + ".png");
        this.properties = properties;
    }

    public static TerrariumPreset parse(JsonObject root) {
        String name = JsonUtils.getString(root, "name");
        ResourceLocation worldType = new ResourceLocation(JsonUtils.getString(root, "world_type"));
        ResourceLocation icon = new ResourceLocation(JsonUtils.getString(root, "icon"));

        JsonObject properties = JsonUtils.getJsonObject(root, "properties");
        return new TerrariumPreset(name, worldType, icon, GenerationSettings.deserialize(properties));
    }

    @SideOnly(Side.CLIENT)
    public String getLocalizedName() {
        return I18n.format("preset." + this.name + ".name");
    }

    @SideOnly(Side.CLIENT)
    public String getLocalizedDescription() {
        return I18n.format("preset." + this.name + ".desc");
    }

    public ResourceLocation getIcon() {
        return this.icon;
    }

    public GenerationSettings createProperties() {
        return GenerationSettings.deserialize(this.properties.serialize());
    }

    public ResourceLocation getWorldType() {
        return this.worldType;
    }
}