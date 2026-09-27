package de.tebrox.vertexCore;

import de.tebrox.vertexCore.command.VertexCoreAdminCommands;
import de.tebrox.vertexCore.command.internal.CommandServiceImpl;
import de.tebrox.vertexCore.database.DatabaseService;
import de.tebrox.vertexCore.database.PluginDataRegistry;
import de.tebrox.vertexCore.language.api.LanguageService;
import de.tebrox.vertexCore.language.internal.LanguageServiceImpl;
import de.tebrox.vertexCore.util.Timeouts;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class VertexCore extends JavaPlugin {

    private CommandServiceImpl commandService;

    @Override
    public void onEnable() {
        PluginDataRegistry registry = new PluginDataRegistry();
        DatabaseService db = new DatabaseService(this, registry);

        this.commandService = new CommandServiceImpl();

        LanguageService languageService = new LanguageServiceImpl();

        VertexCoreApi.init(this, registry, db, this.commandService, languageService);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            this.commandService.registerInto(event.registrar());
        });

        VertexCoreApi.get().commands().register(this, new VertexCoreAdminCommands(this, registry));

        getLogger().info("VertexCore enabled.");

    }

    @Override
    public void onDisable() {
        if(this.commandService != null) this.commandService.shutdown();

        VertexCoreApi.get().languages().shutdown();
        VertexCoreApi.get().databaseService().closeAll();
        Timeouts.shutdown();

        getLogger().info("VertexCore disabled.");
    }
}
