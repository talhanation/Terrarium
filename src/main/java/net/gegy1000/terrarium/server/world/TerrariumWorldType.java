package net.gegy1000.terrarium.server.world;

import net.gegy1000.gengen.api.GenericWorldType;
import net.gegy1000.terrarium.Terrarium;
import net.gegy1000.terrarium.server.world.chunk.ComposableBiomeProvider;
import net.gegy1000.terrarium.server.world.chunk.ComposableChunkGenerator;
import net.gegy1000.terrarium.server.world.data.ColumnDataCache;
import net.gegy1000.terrarium.server.world.generator.customization.GenerationSettings;
import net.gegy1000.terrarium.server.world.generator.customization.PropertyPrototype;
import net.gegy1000.terrarium.server.world.generator.customization.TerrariumCustomization;
import net.gegy1000.terrarium.server.world.generator.customization.TerrariumPreset;
import net.gegy1000.terrarium.server.world.generator.customization.TerrariumPresetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

public abstract class TerrariumWorldType implements GenericWorldType {
    private final String name;
    private final ResourceLocation identifier;
    private final ResourceLocation presetIdentifier;

    private final TerrariumCustomization customization;

    public TerrariumWorldType(String name, ResourceLocation identifier, ResourceLocation presetIdentifier) {
        this.name = Terrarium.ID + "." + name;
        this.identifier = identifier;
        this.presetIdentifier = presetIdentifier;
        this.customization = this.buildCustomization();
    }

    public abstract TerrariumGeneratorInitializer createGeneratorInitializer(World world, GenerationSettings settings, ColumnDataCache dataCache);

    public abstract TerrariumDataInitializer createDataInitializer(GenerationSettings settings);

    public abstract Collection<ICapabilityProvider> createCapabilities(GenerationSettings settings);

    public abstract PropertyPrototype buildPropertyPrototype();

    public abstract TerrariumCustomization buildCustomization();

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ComposableChunkGenerator createGenerator(World world) {
        return new ComposableChunkGenerator(world);
    }

    @Override
    public BiomeProvider createBiomeProvider(World world) {
        if (!world.isRemote) {
            return new ComposableBiomeProvider(world);
        }
        return new BiomeProviderSingle(Biomes.DEFAULT);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public abstract void onCustomize(Minecraft client, WorldType worldType, GuiCreateWorld parent);

    @Override
    public final boolean isCustomizable() {
        return !this.customization.getCategories().isEmpty();
    }

    @Override
    public final int calculateMaxGenerationHeight(WorldServer world) {
        if (world.provider.getDimensionType() == DimensionType.OVERWORLD) {
            GenerationSettings settings = GenerationSettings.parse(world);
            return this.calculateMaxGenerationHeight(world, settings);
        }
        return 256;
    }

    protected int calculateMaxGenerationHeight(WorldServer world, GenerationSettings settings) {
        return Short.MAX_VALUE;
    }

    public ResourceLocation getIdentifier() {
        return this.identifier;
    }

    public TerrariumPreset getPreset() {
        return TerrariumPresetRegistry.get(this.presetIdentifier);
    }

    public TerrariumCustomization getCustomization() {
        return this.customization;
    }

    public boolean isHidden() {
        return false;
    }
}
