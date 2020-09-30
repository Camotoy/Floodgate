/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Floodgate
 */

package org.geysermc.floodgate.command;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.geysermc.floodgate.config.FloodgateConfig;
import org.geysermc.floodgate.platform.command.Command;
import org.geysermc.floodgate.platform.command.CommandMessage;
import org.geysermc.floodgate.platform.command.CommandUtil;

import java.util.UUID;

// probably better as a subcommand of Floodgate
//TODO: Make this more modular for platforms like Fabric?
@NoArgsConstructor
public final class BedrockWhitelistCommand implements Command, Listener {

    @Inject private FloodgateConfig config;
    @Inject private CommandUtil commandUtil;

    @Override
    public void execute(Object source, String locale, String... args) {
        if (!Bukkit.hasWhitelist()) {
            commandUtil.sendMessage(source, locale, Message.WHITELIST_NOT_ENABLED);
            return;
        }
        if (args.length == 0) {
            commandUtil.sendMessage(source, locale, Message.WHITELIST_NO_PLAYER);
            return;
        }
        if (args.length < 2) {
            commandUtil.sendMessage(source, locale, Message.WHITELIST_NO_PLAYER);
            return;
        }
        String prefix = config.getUsernamePrefix();
        String username = args[1];
        if (!username.startsWith(prefix)) {
            username = prefix + username;
        }
        // Here's the problem: Spigot would LOVE the UUID
        // But we can't access the Xbox API and get the UUID without an API key
        // So we have to wait until the player actually joins
        // Until then, have a placeholder name
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        if (args[0].equals("add")) {
            player.setWhitelisted(true);
        } else if (args[0].equals("remove")) {
            player.setWhitelisted(false);
        } else {
            commandUtil.sendMessage(source, locale, Message.WHITELIST_NO_PLAYER);
            return;
        }
        Bukkit.getServer().reloadWhitelist();
        commandUtil.sendMessage(source, locale, Message.WHITELIST_ADD_SUCCESSFUL);
    }

    @Override
    public void execute(Object player, UUID uuid, String username, String locale, String... args) {
        this.execute(player, locale, args);
    }

    @Override
    public String getName() {
        return "bedrockwhitelist";
    }

    @Override
    public String getPermission() {
        return "floodgate.bedrockwhitelist";
    }

    @Override
    public boolean isRequirePlayer() {
        return false;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!Bukkit.hasWhitelist()) return;
        OfflinePlayer floodgatePlayer = Bukkit.getPlayer(event.getUniqueId());
        if (floodgatePlayer.isWhitelisted()) return;
        for (OfflinePlayer player : Bukkit.getWhitelistedPlayers()) {
            // See if this player was hacky-whitelisted earlier
            if (player.getName().equals(event.getName())) {
                player.setWhitelisted(false);
                floodgatePlayer.setWhitelisted(true);
                if (event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST)) {
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
                }
                return;
            }
        }
    }

    public enum Message implements CommandMessage {
        WHITELIST_ADD_SUCCESSFUL("Successfully whitelisted player."),
        WHITELIST_NO_PLAYER("Syntax: /bedrockwhitelist <add/remove> [player]"),
        // Can we get text in the server jar for this?
        WHITELIST_NOT_ENABLED("Whitelisting is not enabled on this server");

        @Getter
        private final String message;

        Message(String message) {
            this.message = message;
        }
    }
}
