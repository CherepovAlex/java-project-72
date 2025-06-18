package hexlet.code.repository;
// Класс использует соединение с базой данных через HikariCP (наследуется от BaseRepository)
// и реализует все основные операции CRUD для работы с проверками URL.
import hexlet.code.model.UrlCheck;
import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
// Аннотация Lombok для автоматического создания логгера
@Slf4j
public class UrlCheckRepository extends BaseRepository {
    // Исполнение SQL-запроса через HikariCP, сохраняет проверку в БД
    public static void save(UrlCheck urlCheck) throws SQLException {

        log.info("UrlCheckRepository's method save() was started!");
        // SQL-запрос для вставки данных проверки
        String query = """
                        INSERT INTO url_checks (status_code, title, h1, description, created_at, url_id)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """;
        // Создание timestamp текущего времени
        Timestamp dayTime = new Timestamp(System.currentTimeMillis());

        try (Connection connection = dataSource.getConnection();
             // Подготовка statement с возможностью получения сгенерированных ключей
             PreparedStatement preparedStatement = connection
                     .prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            // Установка параметров запроса
            preparedStatement.setInt(1, urlCheck.getStatusCode());
            preparedStatement.setString(2, urlCheck.getTitle());
            preparedStatement.setString(3, urlCheck.getH1());
            preparedStatement.setString(4, urlCheck.getDescription());
            preparedStatement.setTimestamp(5, dayTime);
            preparedStatement.setLong(6, urlCheck.getUrlId());

            log.info("preparedStatement is: " + preparedStatement);
            // Выполнение запроса
            preparedStatement.executeUpdate();
            // Получение сгенерированного ID
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();

            if (generatedKeys.next()) {
                urlCheck.setId(generatedKeys.getLong("id"));
            }
        } catch (SQLException throwables) {
            log.error(throwables.getMessage(), throwables);
            throw new SQLException("DB has not returned an id after attempt to save the UrlCheck entity!");
        }
    }
    // Метод для поиска последней проверки по ID URL
    public static Optional<UrlCheck> findLastCheckByUrlId(Long urlId) throws SQLException {
        // SQL-запрос для получения последней проверки
        String query = """
                SELECT DISTINCT ON (url_id) * FROM url_checks
                ORDER BY url_id DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection
                     .prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            UrlCheck urlCheck = null;
            // Обработка результатов запроса
            while (resultSet.next()) {
                Long id = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                Timestamp createdAt = resultSet.getTimestamp("created_at");
                urlCheck = new UrlCheck(statusCode, title, h1, description, urlId);
                urlCheck.setId(id);
                urlCheck.setCreatedAt(createdAt);
            }
            // Создание объекта UrlCheck
            return Optional.ofNullable(urlCheck);

        } catch (SQLException throwables) {
            log.error(throwables.getMessage(), throwables);
            throw new SQLException("Last urlCheck of url with id " + urlId + " was not found!");
        }
    }
    // Метод для получения последних проверок для всех URL
    public static Map<Long, UrlCheck> findLatestChecks() throws SQLException {
        // SQL-запрос для получения последних проверок
        String query = """
                SELECT DISTINCT ON (url_id) *
                FROM url_checks
                ORDER BY url_id DESC, id DESC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            ResultSet rs = stmt.executeQuery();
            Map<Long, UrlCheck> result = new HashMap<>();
            // Обработка результатов запроса
            while (rs.next()) {
                UrlCheck check = new UrlCheck(
                        rs.getInt("status_code"),
                        rs.getString("title"),
                        rs.getString("h1"),
                        rs.getString("description"),
                        rs.getLong("url_id")
                );
                check.setId(rs.getLong("id"));
                check.setCreatedAt(rs.getTimestamp("created_at"));
                result.put(check.getUrlId(), check);
            }
            return result;
        }
    }
    // Метод для получения всех проверок по ID URL
    public static List<UrlCheck> getAllChecks(Long urlId) throws SQLException {
        // SQL-запрос для получения всех проверок URL
        String query = """
                SELECT * FROM url_checks WHERE url_id = ?
                ORDER BY created_at DESC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, urlId);
            ResultSet resultSet = preparedStatement.executeQuery();

            List<UrlCheck> urlChecks = new ArrayList<>();
            // Обработка результатов запроса
            while (resultSet.next()) {
                Long urlCheckId = resultSet.getLong("id");
                int statusCode = resultSet.getInt("status_code");
                String title = resultSet.getString("title");
                String h1 = resultSet.getString("h1");
                String description = resultSet.getString("description");
                Timestamp createdAt = resultSet.getTimestamp("created_at");

                UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, urlId);
                urlCheck.setCreatedAt(createdAt);
                urlCheck.setId(urlCheckId);

                urlChecks.add(urlCheck);
            }

            return urlChecks;
        } catch (SQLException throwables) {
            log.error(throwables.getMessage(), throwables);
            throw new SQLException("DB does not find checks of url with id " + urlId);
        }
    }
    // Метод для очистки таблицы (используется в тестах)
    public static void truncateDB() throws SQLException {
        String query = "TRUNCATE TABLE url_checks RESTART IDENTITY";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection
                     .prepareStatement(query)) {

            preparedStatement.executeUpdate();

        } catch (SQLException throwables) {
            log.error(throwables.getMessage(), throwables);
            throw new SQLException("Truncate task on table url_checks has failed!");
        }
    }
}
