package hexlet.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import hexlet.code.repository.BaseRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import lombok.extern.slf4j.Slf4j;

import io.javalin.Javalin;
//import io.javalin.rendering.template.JavalinThymeleaf;
//import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
//import org.thymeleaf.TemplateEngine;
//import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
//import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import io.javalin.rendering.template.JavalinJte;
import gg.jte.resolve.ResourceCodeResolver;

@Slf4j
public class App {

    private static int getPort() {
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
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
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
        });
        app.get("/", ctx -> ctx.render("index.jte"));

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
