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
        hikariConfig.setJdbcUrl("jdbc:h2:mem:project;DB_CLOSE_DELAY=-1;");

        var dataSource = new HikariDataSource(hikariConfig);
        var sql = readResourceFile("schema.sql");

        log.info(sql);

        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute(sql);
        }
        BaseRepository.dataSource = dataSource;

        var app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte());
        });
        app.get("/", ctx -> ctx.result("Hello World"));

        return app;
    }

    public static void main(String[] args) throws IOException, SQLException {
        Javalin app = getApp();
        app.start(getPort());
    }
}
