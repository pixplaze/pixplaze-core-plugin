package com.pixplaze.api.nms;

import com.pixplaze.plugin.PixplazeCorePlugin;

import java.lang.reflect.InvocationTargetException;

public class MinecraftServer {
    public static Object produce() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return produce(PixplazeCorePlugin.getInstance());
    }

    public static Object produce(Object producingObject) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final var minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
        final var getMinecraftServerMethod = minecraftServerClass.getMethod("getServer");
        return getMinecraftServerMethod.invoke(producingObject);
    }
}
