package com.scypter.smaintenance.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.scypter.smaintenance.core.proxy.ProxyCommandHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;

public final class VelocityCommand implements SimpleCommand {
    private final ProxyCommandHandler handler;

    public VelocityCommand(ProxyCommandHandler handler) {
        this.handler = handler;
    }

    private ProxyCommandHandler.CommandSource wrap(final CommandSource s) {
        return new ProxyCommandHandler.CommandSource() {
            public String name() {
                return s instanceof com.velocitypowered.api.proxy.Player p ? p.getUsername() : "CONSOLE";
            }

            public boolean has(String node) {
                return s.hasPermission(node);
            }

            public void send(Component c) {
                s.sendMessage(c);
            }
        };
    }

    @Override
    public void execute(Invocation invocation) {
        handler.execute(wrap(invocation.source()), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return handler.complete(wrap(invocation.source()), invocation.arguments());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("smaintenance.command");
    }
}
