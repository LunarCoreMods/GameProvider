package com.example.javaexamplemod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import emu.lunarcore.LunarCore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LunarCore.class, remap = false)
public abstract class LunarCoreMixin {
    @Shadow
    private static String getJarVersion() {
        return null;
    }

    // simple mixin
    @Inject(method = "main", at = @At("HEAD"))
    private static void onMain(String[] args, CallbackInfo ci) {
        System.out.println("Hello from LunarCoreMixin! (jarVersion: " + getJarVersion() + ")");
    }

    // mixinextras
    @Inject(method = "main", at = @At(value = "NEW", target = "()Lemu/lunarcore/command/CommandManager;"))
    private static void onMainStore(String[] args, CallbackInfo ci, @Local boolean generateHandbook) {
        System.out.println("handbook: " + generateHandbook);
    }
}
