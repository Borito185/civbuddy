package com.civbuddy.veins.session.listeners;

import com.civbuddy.common.utils.ChatHelper;
import com.civbuddy.veins.serializers.Base91;
import com.civbuddy.veins.session.SessionConfig;
import com.civbuddy.veins.session.VeinSessionClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.civbuddy.veins.session.VeinSessionClient.PREFIX;

public class MessageListener {
    private static final Pattern CHAT_PATTERN =
            Pattern.compile("^\\[([^]]+)] ([^:]+): (.+)$");
    public static void initialize() {
        ClientReceiveMessageEvents.ALLOW_GAME.register(MessageListener::onChatMessage);
    }

    private static boolean onChatMessage(Component component, boolean b) {
        String msg = component.getString();

        if (!msg.contains(PREFIX)) return true;

        Matcher matcher = CHAT_PATTERN.matcher(msg);
        if (!matcher.matches()) return true;

        String namelayer = matcher.group(1);
        String playerName = matcher.group(2);
        String message = matcher.group(3);
        if (!message.startsWith(PREFIX)) return true;
        message = message.substring(PREFIX.length());

        if (checkForConfig(namelayer, playerName, message)) {
            return false;
        }

        if (checkForDiamondMessage(namelayer, message)) {
            return false;
        }

        return false;
    }

    private static boolean checkForConfig(String namelayer, String playerName, String message) {
        SessionConfig decoded = SessionConfig.decode(namelayer, message);
        if (decoded == null) return false;

        VeinSessionClient.seenConfigs.put(namelayer, decoded);

        if (VeinSessionClient.isActive()) {
            VeinSessionClient.setSession(decoded);
        } else if (decoded.invite) {
            ChatHelper.say(
                    Component.literal(String.format(
                            "§aYou have been invited to a mining session by §f%s §aon §e%s§a. Click to join!",
                            playerName,
                            namelayer
                    )).withStyle(s -> s.withClickEvent(
                            new ClickEvent.RunCommand("/veins session join " + namelayer)
                    ))
            );
        }
        return true;
    }

    private static boolean checkForDiamondMessage(String namelayer, String message) {
        if (!VeinSessionClient.isActive()
                || !Objects.equals(namelayer, VeinSessionClient.activeSession.namelayer)) {
            return false;
        }

        try {
            String decoded = new String(
                    Base91.decode(message),
                    StandardCharsets.UTF_8
            );

            String[] parts = decoded.split(":");
            if (parts.length != 4) return false;

            String decodedPlayer = parts[0];
            String decodedNamelayer = parts[1];
            int number = Integer.parseInt(parts[2]);
            int hash = Integer.parseInt(parts[3]);

            if (!Objects.equals(decodedNamelayer, namelayer)) return false;

            String raw = decodedPlayer + ":" + decodedNamelayer + ":" + number;

            if (raw.hashCode() != hash) return false;

            VeinSessionClient.setPersonDiamonds(decodedPlayer, number);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
