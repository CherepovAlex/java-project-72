package hexlet.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import hexlet.code.model.Url;
import hexlet.code.repository.BaseRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.repository.UrlRepository;
import org.flywaydb.core.Flyway;

import lombok.extern.slf4j.Slf4j;

import io.javalin.Javalin;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.rendering.template.JavalinJte;
import gg.jte.resolve.ResourceCodeResolver;

@Slf4j
public class App {

    private static int getPort() throws IOException, SQLException {
        // Получаем порт из переменных окружения или используем 7070 по умолчанию
        String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.valueOf(port);
    }

    private static String readResourceFile(String fileName) throws IOException {
        // Чтение файлов ресурсов (например, SQL-скриптов)
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    // метод создаёт и настраивает движок шаблонов
    private static TemplateEngine createTemplateEngine() {
        // Получает загрузчик классов для текущего класса App
        ClassLoader classLoader = App.class.getClassLoader();
        // 2. Создает резолвер шаблонов:
        //    - Ищет файлы шаблонов в директории "templates" classpath
        //    - Использует полученный загрузчик классов
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        // 3. Инициализирует движок шаблонов:
        //    - Передает резолвер для поиска шаблонов
        //    - Указывает тип контента HTML (рендеринг как HTML)
        TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        return templateEngine;
    }

    public static Javalin getApp() throws IOException, SQLException {

        var hikariConfig = new HikariConfig();
        // Получаем URL БД из переменных окружения
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            // Режим разработки (H2 в памяти)
            hikariConfig.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");
            hikariConfig.setDriverClassName("org.h2.Driver");
        } else {
            // Продакшен (PostgreSQL)
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            // Оптимальные настройки для Render.com
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setIdleTimeout(30000);
        }

        var dataSource = new HikariDataSource(hikariConfig);
        // Инициализация БД
        try {
            if (jdbcUrl != null && !jdbcUrl.contains("h2")) {
                // Для PostgreSQL используем Flyway миграции
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .baselineOnMigrate(true)
                        .load();
                flyway.repair();
                flyway.migrate();
            } else {
                // Инициализация схемы для H2 напрямую
                var sql = readResourceFile("schema.sql");
                try (var connection = dataSource.getConnection();
                     var statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }

        BaseRepository.dataSource = dataSource;

        // Создаем Javalin приложение
        var app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            // конфигурация Javalin изменена, чтобы использовать созданный движок шаблонов
            config.fileRenderer(new JavalinJte(createTemplateEngine()));
            // включаем встроенные сессии в Javalin 6.x
            config.useVirtualThreads = true;
        });
        // Middleware для обработки flash-сообщений
        app.before(ctx -> {
            // Переносим flash-сообщения из сессии в атрибуты запроса
            if (ctx.sessionAttribute("flash") != null) {
                ctx.attribute("flash", ctx.sessionAttribute("flash"));
                ctx.sessionAttribute("flash", null);
            }
            if (ctx.sessionAttribute("flash-type") != null) {
                ctx.attribute("flash-type", ctx.sessionAttribute("flash-type"));
                ctx.sessionAttribute("flash-type", null);
            }
        });
        // Главная страница с формой ввода URL
        app.get("/", ctx -> {
            Map<String, Object> model = new HashMap<>();
            model.put("flash", ctx.attribute("flash"));
            model.put("flashType", ctx.attribute("flash-type"));
            ctx.render("index.jte", model);
        });
        // Обработчик формы - добавляет URL в базу данных
        app.post("/urls", ctx -> {
            var inputUrl = ctx.formParam("url");
            try {
                // нормализуем url: оставляем только протокол, домен и порт
                var uri = new URI(inputUrl).toURL();
                var nomalizedUrl = uri.getProtocol() + "://" + uri.getAuthority();
                // проверяем есть ли дубль
                var existingUrl = UrlRepository.findByName(nomalizedUrl);
                if (existingUrl.isPresent()) {
                    // если есть - выводим flash-message
                    ctx.sessionAttribute("flash", "The page already exists");
                    ctx.sessionAttribute("flash-type", "info");
                } else {
                    // добавляем новый url в БД
                    var url = new Url(nomalizedUrl);
                    UrlRepository.save(url);
                    ctx.sessionAttribute("flash", "The page added successfully");
                    ctx.sessionAttribute("flash-type", "success");
                }
            } catch (Exception e) {
                // если url некорректный, то выводим сообщение
                ctx.sessionAttribute("flash", "Incorrect URL");
                ctx.sessionAttribute("flash-type", "danger");
            }
            ctx.redirect("/");
        });

        // страница со списком всех url
        app.get("/urls", ctx -> {
            var urls = UrlRepository.getEntities();
            ctx.render("urls/index.jte", Map.of("urls", urls));
        });

        app.get("/urls/{id}", ctx -> {
            var id = ctx.pathParamAsClass("id", Long.class).get();
            var url = UrlRepository.find(id);
            if (url.isEmpty()) {
                // если такой страницы нет, то ошибка 404
                ctx.status(404);
                return;
            }
            ctx.render("urls/show.jte", Map.of("url", url.get()));
        });

        app.get("/health", ctx -> {
            try (var conn = BaseRepository.dataSource.getConnection()) {
                ctx.result("Database connection OK");
            } catch (SQLException e) {
                ctx.status(500).result("DB connection error: " + e.getMessage());
            }
        });

        return app;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Javalin app = getApp();
        app.start(getPort());
    }
}
