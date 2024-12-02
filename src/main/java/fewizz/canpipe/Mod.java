package fewizz.canpipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blue.endless.jankson.Jankson;
import net.fabricmc.api.ClientModInitializer;

public class Mod implements ClientModInitializer {
    public static final String MOD_ID = "canpipe";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Jankson JANKSON = Jankson.builder().build();

    @Override
    public void onInitializeClient() {
    }

}