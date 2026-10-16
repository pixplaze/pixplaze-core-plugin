package com.pixplaze.plugin;

import com.pixplaze.api.host.ServerHostResolver;
import com.pixplaze.api.host.ServerHostResolvers;
import com.pixplaze.api.state.ServerStateSnapshot;
import com.pixplaze.chat.ChatBuffer;
import com.pixplaze.exchange.ExchangeServer;
import com.pixplaze.exchange.JavalinExchangeServer;
import com.pixplaze.rcon.ConsoleBuffer;
import io.javalin.Javalin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

public final class PixplazeCorePlugin extends JavaPlugin {

    private static PixplazeCorePlugin instance;
    private ConsoleBuffer consoleBuffer;
    private ChatBuffer chatBuffer;
    private ExchangeServer<Javalin> exchangeServer;
    private ServerHostResolver serverHostResolver;
    private ServerStateSnapshot serverStateSnapshot;

    /// Экземпляр, созданный сервером: заполняется в {@link #onEnable()} и обнуляется
    /// в {@link #onDisable()}. Создавать {@link JavaPlugin} вручную нельзя — его конструктор
    /// требует загрузки через PluginClassLoader сервера, поэтому при обращении к выключенному
    /// плагину честнее упасть, чем молча подсунуть неработоспособный объект.
    public static PixplazeCorePlugin getInstance() {
        return Objects.requireNonNull(instance, "PixplazeCorePlugin is disabled");
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        initMessageBuffers();
        // До HTTP-сервера: контроллеры и DAO создаются при монтировании маршрутов
        // и рассчитывают, что снимок уже есть.
        initServerStateSnapshot();
        initHttpServer();
        initServerHostResolver();
    }

    /// Порядок важен: сначала отцепляемся от log4j и событий Bukkit (они живут дольше плагина),
    /// затем останавливаем HTTP-сервер и задачу обновления снимка, затем отпускаем
    /// статическую ссылку на плагин. Всё вместе гарантирует, что после выгрузки на классы
    /// плагина никто не ссылается, и обращений к закрытому jar не будет.
    @Override
    public void onDisable() {
        Optional.ofNullable(consoleBuffer)
                .ifPresent(ConsoleBuffer::detach);

        Optional.ofNullable(chatBuffer)
                .ifPresent(ChatBuffer::detach);

        Optional.ofNullable(exchangeServer)
                .ifPresent(ExchangeServer::stop);

        Optional.ofNullable(serverStateSnapshot)
                .ifPresent(ServerStateSnapshot::stop);

        instance = null;
    }

    public ConsoleBuffer getConsoleBuffer() {
        return consoleBuffer;
    }

    public ChatBuffer getChatBuffer() {
        return chatBuffer;
    }

    /// Резолвер публичного хоста сервера. Реализация выбрана {@link ServerHostResolvers}
    /// в {@link #onEnable()} по условиям окружения (конфиг/прокси/подключения).
    public ServerHostResolver getServerHostResolver() {
        return serverHostResolver;
    }

    private void initServerHostResolver() {
        serverHostResolver = ServerHostResolvers.create(this);
    }

    /// Снимок состояния сервера, обновляемый из главного потока. HTTP-слой читает значения
    /// отсюда, а не дёргает Bukkit API из потоков Jetty.
    public ServerStateSnapshot getServerStateSnapshot() {
        return serverStateSnapshot;
    }

    private void initServerStateSnapshot() {
        serverStateSnapshot = new ServerStateSnapshot(this);
        serverStateSnapshot.start();
    }

    /// Лог сервера и видимый всем чат — источники websocket-потоков {@code /console} и {@code /chat}.
    private void initMessageBuffers() {
        consoleBuffer = new ConsoleBuffer();
        consoleBuffer.attach();

        chatBuffer = new ChatBuffer(this);
        chatBuffer.attach();
    }

    /**<pre>
     * Создаёт и инициализирует HTTP-сервер по адресу и порту из {@code config.yml}.
     *
     * В случае, если в конфигурационном файле не указан {@code http-listen-ip},
     * указан неправильно или указан как {@code auto},
     * то пытается автоматически определить ip-адрес.
     *
     * В случае, если автоматически определить адрес не удаётся, сервер запускается
     * на {@code 127.0.0.1}.
     *
     * Если создать HTTP-сервер по прежнему не удаётся, печатает сообщение об ошибке.
     * </pre>
     */
    private void initHttpServer() {
        var address = getConfig().getString("http-listen-ip");
        var port = getConfig().getInt("http-listen-port");

        exchangeServer = new JavalinExchangeServer(port);
        exchangeServer.start();
    }
}
