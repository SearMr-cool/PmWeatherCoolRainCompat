package com.searmr.coolraincompat.mixin;

import com.leclowndu93150.coolrain.CoolRainSounds;
import com.leclowndu93150.coolrain.compat.ModCompat;
import com.leclowndu93150.coolrain.mixin.RainMixin;
import com.leclowndu93150.coolrain.tags.ModTags;
import com.searmr.coolraincompat.PmWeatherCoolRainCompat;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.sound.ModSounds;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 1)
public class TickRain {
    @Unique
    private int rainSoundTimer = 0;
    @Inject(method = "tickRain", at = @At("HEAD"))
    public void editTickRain(Camera camera, CallbackInfo callbackInfo){

        Player player = Minecraft.getInstance().player;
        WeatherHandlerClient weatherHandlerClient = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
        float rain = weatherHandlerClient.getPrecipitation();
        if(GameBusClientEvents.weatherHandler != null && player != null) {
            BlockPos blockPos = player.blockPosition();
            ClientLevel level = Minecraft.getInstance().level;
          BlockPos playAt = null;
            int i = (int)(100f*rain*rain);
            for(int j = 0; j < i; j++){
                int k = PMWeather.RANDOM.nextInt(21)-10;
                int l = PMWeather.RANDOM.nextInt(21)-10;
                BlockPos blockPos2 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos.offset(k,0,l));
                if(blockPos2.getY() > level.getMinBuildHeight() && blockPos2.getY() <= blockPos.getY()+10 && blockPos2.getY() >= blockPos.getY()-10){
                    playAt = blockPos2.below();
                }
            }
            ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.getPrecipitationType(weatherHandlerClient, player.position(), level, 0);
            if(playAt != null && PMWeather.RANDOM.nextInt(3) < this.rainSoundTimer++){
                this.rainSoundTimer = 0;
                if(precip != ThermodynamicEngine.Precipitation.SNOW){
                    handleCustomRainSound(camera, playAt, playAt.getY() > camera.getBlockPosition().getY() + 1
                            && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, camera.getBlockPosition()).getY() > camera.getBlockPosition().getY());
                }
                }
            }
        }

    @Unique
    private void handleCustomRainSound(Camera camera, BlockPos soundPos, boolean isMuffled) {
        BlockPos subLevelPos;
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null || soundPos == null) {
            return;
        }
        RandomSource random = world.random;
        BlockState blockState = world.getBlockState(soundPos);
        if (blockState.isAir() && (subLevelPos = ModCompat.getSubLevelSurfacePos((Level)world, soundPos)) != null) {
            soundPos = subLevelPos;
            blockState = world.getBlockState(subLevelPos);
        }
        this.playRainSound(world, soundPos, blockState, random, isMuffled);
    }

    @Unique
    private void playRainSound(ClientLevel world, BlockPos pos, BlockState blockState, RandomSource random, boolean isMuffled) {
        SoundEvent sound = getRainSoundForBlock(blockState);
        float volume = getRainSoundVolume(blockState, isMuffled);
        float pitch = this.getRainSoundPitch(blockState, random);
        boolean shouldAttenuate = this.shouldAttenuateSound(blockState);
        if (sound == CoolRainSounds.RAIN_SOUNDS_AMETHYST.get() || sound == CoolRainSounds.RAIN_SOUNDS_NOTEBLOCK.get()) {
            world.playLocalSound(pos, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2f, 1.0f, false);
        }
        if (sound == CoolRainSounds.RAIN_SOUNDS_LAVA.get()) {
            world.playLocalSound(pos, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.1f, pitch, false);
        }
        world.playLocalSound(pos, sound, SoundSource.WEATHER, volume, pitch, shouldAttenuate);
    }

    @Unique
    private SoundEvent getRainSoundForBlock(BlockState blockState) {
        if (blockState.is(Blocks.LAVA) || blockState.is(Blocks.MAGMA_BLOCK) || blockState.is(Blocks.CAMPFIRE) || blockState.is(Blocks.SOUL_CAMPFIRE) || blockState.is(ModTags.Blocks.LAVA_BLOCKS)) {
            return CoolRainSounds.RAIN_SOUNDS_LAVA.get();
        }
        if (blockState.is(Blocks.WATER) || blockState.is(ModTags.Blocks.WATER_BLOCKS)) {
            return CoolRainSounds.RAIN_SOUNDS_WATER.get();
        }
        if (blockState.is(Blocks.AMETHYST_BLOCK) || blockState.is(Blocks.AMETHYST_CLUSTER) || blockState.is(Blocks.BUDDING_AMETHYST) || blockState.is(Blocks.SMALL_AMETHYST_BUD) || blockState.is(Blocks.MEDIUM_AMETHYST_BUD) || blockState.is(Blocks.LARGE_AMETHYST_BUD) || blockState.is(ModTags.Blocks.AMETHYST_BLOCKS)) {
            return CoolRainSounds.RAIN_SOUNDS_AMETHYST.get();
        }
        if (blockState.is(Blocks.NOTE_BLOCK) || blockState.is(ModTags.Blocks.NOTEBLOCK_BLOCKS)) {
            return CoolRainSounds.RAIN_SOUNDS_NOTEBLOCK.get();
        }
        if (blockState.is(ModTags.Blocks.METAL_BLOCKS) || blockState.is(ModTags.Blocks.C_METAL_BLOCKS) || blockState.is(ModTags.Blocks.C_STORAGE_BLOCKS_IRON) || blockState.is(ModTags.Blocks.C_STORAGE_BLOCKS_COPPER) || blockState.is(ModTags.Blocks.C_STORAGE_BLOCKS_GOLD) || blockState.is(ModTags.Blocks.C_STORAGE_BLOCKS_NETHERITE) || blockState.is(Blocks.IRON_BLOCK) || blockState.is(Blocks.GOLD_BLOCK) || blockState.is(Blocks.NETHERITE_BLOCK) || blockState.is(Blocks.COPPER_BLOCK) || blockState.is(Blocks.RAW_IRON_BLOCK) || blockState.is(Blocks.RAW_COPPER_BLOCK) || blockState.is(Blocks.RAW_GOLD_BLOCK) || blockState.is(Blocks.LANTERN) || blockState.is(Blocks.SOUL_LANTERN) || blockState.is(Blocks.IRON_TRAPDOOR)) {
            return CoolRainSounds.RAIN_SOUNDS_METAL.get();
        }
        if (blockState.is(ModTags.Blocks.GLASS_BLOCKS) || blockState.is(ModTags.Blocks.C_GLASS_BLOCKS) || blockState.is(ModTags.Blocks.C_GLASS_PANES) || blockState.is(ModTags.Blocks.C_ICE) || blockState.is(Blocks.GLASS) || blockState.is(Blocks.TINTED_GLASS) || blockState.is(Blocks.ICE) || blockState.is(Blocks.BLUE_ICE) || blockState.is(Blocks.PACKED_ICE)) {
            return CoolRainSounds.RAIN_SOUNDS_GLASS.get();
        }
        if (blockState.is(ModTags.Blocks.FABRIC_BLOCKS) || blockState.is(BlockTags.WOOL) || blockState.is(BlockTags.WOOL_CARPETS) || blockState.is(BlockTags.BEDS)) {
            return CoolRainSounds.RAIN_SOUNDS_FABRIC.get();
        }
        if (blockState.is(ModTags.Blocks.FOLIAGE_BLOCKS) || blockState.is(ModTags.Blocks.C_LEAVES) || blockState.is(BlockTags.LEAVES)) {
            return CoolRainSounds.RAIN_SOUNDS_FOLIAGE.get();
        }
        return SoundEvents.WEATHER_RAIN;
    }

    @Unique
    private float getRainSoundVolume(BlockState blockState, boolean isMuffled) {
        SoundEvent sound = this.getRainSoundForBlock(blockState);
        if (sound == CoolRainSounds.RAIN_SOUNDS_FABRIC.get()) {
            return 0.75f;
        }
        if (sound == CoolRainSounds.RAIN_SOUNDS_WATER.get()) {
            return 0.175f;
        }
        if (sound == CoolRainSounds.RAIN_SOUNDS_AMETHYST.get()) {
            return 0.3f;
        }
        if (sound == CoolRainSounds.RAIN_SOUNDS_NOTEBLOCK.get()) {
            return 0.2f;
        }
        if (sound == CoolRainSounds.RAIN_SOUNDS_LAVA.get()) {
            return 0.2f;
        }
        if (isMuffled) {
            return 0.1f;
        }
        if (sound == SoundEvents.WEATHER_RAIN) {
            return 0.2f;
        }
        return 0.5f;
    }

    @Unique
    private float getRainSoundPitch(BlockState blockState, RandomSource random) {
        SoundEvent sound = this.getRainSoundForBlock(blockState);
        if (sound == CoolRainSounds.RAIN_SOUNDS_FOLIAGE.get() || sound == CoolRainSounds.RAIN_SOUNDS_LAVA.get()) {
            return 1.0f;
        }
        if (sound == SoundEvents.WEATHER_RAIN) {
            return 1.0f;
        }
        return (float)random.nextIntBetweenInclusive(8, 12) * 0.1f;
    }

    @Unique
    private boolean shouldAttenuateSound(BlockState blockState) {
        SoundEvent sound = this.getRainSoundForBlock(blockState);
        return sound != CoolRainSounds.RAIN_SOUNDS_AMETHYST.get() && sound != CoolRainSounds.RAIN_SOUNDS_NOTEBLOCK.get() && sound != SoundEvents.WEATHER_RAIN;
    }
}
