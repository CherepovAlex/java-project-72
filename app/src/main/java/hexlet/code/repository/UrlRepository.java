package hexlet.code.repository;

import hexlet.code.model.Url;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UrlRepository {

    // сохранение URL в базу данных
    public static void save(Url url) throws SQLException {
        var sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";
        try (var conn = BaseRepository.dataSource.getConnection();
            var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, url.getName());
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.executeUpdate();
        }
    }

    // поиск URL по имени в базе
    public static Optional<Url> findByName(String name) throws SQLException {
        var sql = "SELECT * FROM urls WHERE name = ?";
        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                var url = new Url(resultSet.getString("name"));
                url.setId(resultSet.getLong("id"));
                url.setCreatedAt(resultSet.getTimestamp("created_at"));
                return Optional.of(url);
            }
            return Optional.empty();
        }
    }

    // получить все url из БД в обратном порядке по добавлению
    public static List<Url> getEntities() throws SQLException {
        var sql = "SELECT * FROM urls ORDER BY id DESC";
        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            var resultSet = stmt.executeQuery();
            var result = new ArrayList<Url>();
            while (resultSet.next()) {
                var url = new Url(resultSet.getString("name"));
                url.setId(resultSet.getLong("id"));
                url.setCreatedAt(resultSet.getTimestamp("created_at"));
                result.add(url);
            }
            return result;
        }
    }
    // поиск url по ID в БД
    public static Optional<Url> find(Long id) throws SQLException {
        var sql = "SELECT * FROM urls WHERE id = ?";
        try (var conn = BaseRepository.dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                var url = new Url(resultSet.getString("name"));
                url.setId(resultSet.getLong("id"));
                url.setCreatedAt(resultSet.getTimestamp("created_at"));
                return Optional.of(url);
            }
            return Optional.empty();
        }
    }
}
