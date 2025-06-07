package hexlet.code;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
//import java.util.List;
import java.util.stream.Collectors;

//import io.javalin.rendering.template.JavalinJte;
//import static io.javalin.rendering.template.TemplateUtil.model;

//import hexlet.code.controller.CarsController;
//import hexlet.code.controller.PostsController;
//import hexlet.code.controller.SessionsController;
//import hexlet.code.controller.UsersController;
//import hexlet.code.dto.MainPage;
//import hexlet.code.dto.courses.CoursesPage;
//import hexlet.code.dto.courses.CoursePage;
//import hexlet.code.dto.users.BuildUserPage;
//import hexlet.code.dto.users.UsersPage;
//import hexlet.code.model.Course;
//import hexlet.code.model.User;
import hexlet.code.repository.BaseRepository;
//import hexlet.code.repository.CourseRepository;
//import hexlet.code.repository.UserRepository;
//import hexlet.code.util.NamedRoutes;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import io.javalin.Javalin;
//import io.javalin.validation.ValidationException;
//import io.javalin.http.NotFoundResponse;
import io.javalin.rendering.template.JavalinJte;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.valueOf(port);
    }

    private static String readResourceFile(String fileName) throws IOException {
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static Javalin getApp() throws IOException, SQLException {

        var hikariConfig = new HikariConfig();
// Получаем параметры подключения из переменных окружения
        String jdbcUrl = System.getenv("JDBC_DATABASE_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            // Режим разработки (H2)
            hikariConfig.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");
            hikariConfig.setDriverClassName("org.h2.Driver");
        } else {
            // Продакшен режим (PostgreSQL)
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(dbUser);
            hikariConfig.setPassword(dbPassword);
            hikariConfig.setDriverClassName("org.postgresql.Driver");

            // Оптимальные настройки для Render.com
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setIdleTimeout(30000);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setLeakDetectionThreshold(30000);
        }

        var dataSource = new HikariDataSource(hikariConfig);

// Инициализация базы данных
        try {
            if (jdbcUrl != null && !jdbcUrl.contains("h2")) {
                // Миграции для PostgreSQL
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .baselineOnMigrate(true)
                        .load();
                flyway.migrate();
            } else {
                // Инициализация схемы для H2
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

        var app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte());
        });
        app.get("/", ctx -> ctx.result("Hello World with PostgreSQL!"));

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
