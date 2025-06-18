package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import jakarta.servlet.http.HttpServletResponse;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.HttpRequest;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static MockWebServer mockServer;
    private static Javalin app;
    private static String baseUrl;
    private static final String CORRECT_URL = "https://www.google.com";
    private static final String URL_FOR_NON_EXISTING_ENTITY_TEST = "https://www.yandex.ru";
    private static final String EXISTING_URL = "https://existing-url.com";
    private static final String WRONG_URL = "htp:/invalid-123456.url";

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    // запускает приложение и mock-сервер
    @BeforeAll
    public static void beforeAll() throws SQLException, IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        mockServer = new MockWebServer();
        MockResponse mockedResponse = new MockResponse()
                .setBody(readFixture("index.html"));
        mockServer.enqueue(mockedResponse);
        mockServer.start();
    }

    // останавливает приложение и mock-сервер
    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockServer.shutdown();
        Unirest.shutDown(); // Закрываем все соединения Unirest
    }

    // очищает БД перед каждым тестом
    @BeforeEach
    public void beforeEach() throws SQLException {
        UrlRepository.truncateDB();
        UrlCheckRepository.truncateDB();

        // Добавляем несколько URL для тестов
        Url firstUrl = new Url(CORRECT_URL);
        firstUrl.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        UrlRepository.save(firstUrl);

        Url secondUrl = new Url(EXISTING_URL);
        secondUrl.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        UrlRepository.save(secondUrl);
    }

    // тривиальный тест
    @Test
    public void testInit() {
        assertThat(app).isNotNull();
    }

    // проверяет доступность главной страницы
    @Test
    public void testWelcome() {
        // Для GET-запросов с простым телом закрывать ничего не нужно
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    // Тесты для обработчиков ошибок
    @Test
    public void testInternalServerErrorHandler() {
        app.get("/test-500", ctx -> {
            throw new RuntimeException("Test error");
        });

        HttpResponse<String> response = Unirest.get(baseUrl + "/test-500").asString();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getBody()).contains("Internal server error");
    }

    @Test
    public void testNotFoundHandler() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/non-existent-route").asString();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getBody()).contains("Not found");
    }

    // тест для контроллера URL
    @Nested
    class UrlControllerTest {
        // добавление URL
        @Test
        public void testCreateUrl() {
            HttpRequest request = Unirest.post(baseUrl + "/urls")
                    .field("url", CORRECT_URL);

            try {
                HttpResponse<String> response = request.asString();
                assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);

                // Проверяем, что URL появился в списке
                HttpResponse<String> urlsResponse = Unirest.get(baseUrl + "/urls").asString();
                assertThat(urlsResponse.getBody()).contains(CORRECT_URL);
            } catch (UnirestException e) {
                throw new RuntimeException("Request failed", e);
            }
        }
        // пустой url
        @Test
        public void testCreateEmptyUrl() {
            HttpRequest request = Unirest.post(baseUrl + "/urls")
                    .field("url", "");  // Пустой URL

            try {
                HttpResponse<String> response = request.asString();
                assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
                assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/");

                // Проверяем flash-сообщение на главной странице
                HttpResponse<String> homeResponse = Unirest.get(baseUrl).asString();
                String homeBody = homeResponse.getBody();
                assertThat(homeBody).contains("URL не может быть пустым");

                // Проверяем, что URL не добавился в список
                HttpResponse<String> urlsResponse = Unirest.get(baseUrl + "/urls").asString();
                String urlsBody = urlsResponse.getBody();

                // Проверяем что в списке нет пустого URL
                assertThat(urlsBody)
                        .doesNotContain("> </a>")  // Проверка на отсутствие пустой ссылки
                        .doesNotContain("\"\"");   // Проверка на отсутствие пустых кавычек
            } catch (UnirestException e) {
                throw new RuntimeException("Request failed", e);
            }
        }
        // отсутствующий url
        @Test
        public void testCreateNullUrl() {
            HttpRequest request = Unirest.post(baseUrl + "/urls")
                    .field("url", (String) null);  // Null значение

            try {
                HttpResponse<String> response = request.asString();
                assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);
                assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/");

                HttpResponse<String> homeResponse = Unirest.get(baseUrl).asString();
                assertThat(homeResponse.getBody()).contains("URL не может быть пустым");

                HttpResponse<String> urlsResponse = Unirest.get(baseUrl + "/urls").asString();
                String urlsBody = urlsResponse.getBody();

                // Проверяем что в списке нет null-значений
                assertThat(urlsBody)
                        .doesNotContain("null")
                        .doesNotContain(">null<");
            } catch (UnirestException e) {
                throw new RuntimeException("Request failed", e);
            }
        }

        // корректность URL
        @ParameterizedTest
        @ValueSource(strings = {"htp:/invalid.url", "not-a-url", "http://"})
        public void testCreateWrongUrl(String url) {
            // Убираем приведение типа и работаем напрямую с HttpRequest
            HttpRequest request = Unirest.post(baseUrl + "/urls")
                    .field("url", url);

            try {
                HttpResponse<String> response = request.asString();
                assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);

                // Проверяем flash-сообщение
                HttpResponse<String> getResponse = Unirest.get(baseUrl).asString();
                assertThat(getResponse.getBody()).contains("Некорректный URL");

                // Проверяем, что URL не добавился в список
                HttpResponse<String> urlsResponse = Unirest.get(baseUrl + "/urls").asString();
                assertThat(urlsResponse.getBody()).doesNotContain(url);
            } catch (UnirestException e) {
                throw new RuntimeException("Request failed", e);
            }
        }

        // отображение URLs
        @Test
        public void testShowUrls() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            String body = response.getBody();
            int getQueryStatus = response.getStatus();

            assertThat(getQueryStatus).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(CORRECT_URL);
            assertThat(body).contains(EXISTING_URL);
        }
        // отображение пустого url
        @Test
        public void testShowEmptyUrls() throws SQLException {
            // Очищаем базу
            UrlRepository.truncateDB();
            UrlCheckRepository.truncateDB();

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body)
                    .contains("Сайты")
                    .containsPattern("<tbody>\\s*</tbody>"); // Пустая таблица
        }

        // отображение URL
        @Test
        public void testShowUrlById() throws SQLException {
            Url actualUrl = UrlRepository.findByName(CORRECT_URL).orElseThrow(
                    () -> new SQLException("url with the name " + CORRECT_URL + " was not found!"));

            Long id = actualUrl.getId();

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + id).asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(body).contains(CORRECT_URL,
                    actualUrl.getCreatedAt()
                            .toLocalDateTime()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            Url wrongUrl = new Url(URL_FOR_NON_EXISTING_ENTITY_TEST);
            wrongUrl.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            UrlRepository.save(wrongUrl);
            Long idForDeletion = UrlRepository.findByName(URL_FOR_NON_EXISTING_ENTITY_TEST)
                    .orElseThrow(() -> new SQLException("wrongUrl with name " + URL_FOR_NON_EXISTING_ENTITY_TEST
                            + " was not found in DB!"))
                    .getId();

            UrlRepository.delete(idForDeletion);
            response = Unirest.get(baseUrl + "/urls/" + idForDeletion).asString();
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Test
    public void testShowNonExistentUrl() {
        HttpResponse<String> response = Unirest.get(baseUrl + "/urls/9999").asString();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getBody()).contains("Not found");
    }

    @Test
    public void testCreateExistingUrl() {
        HttpRequest request = Unirest.post(baseUrl + "/urls")
                .field("url", EXISTING_URL);

        try {
            HttpResponse<String> response = request.asString();
            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FOUND);

            HttpResponse<String> urlsResponse = Unirest.get(baseUrl + "/urls").asString();
            assertThat(urlsResponse.getBody()).contains("Страница уже существует");
        } catch (UnirestException e) {
            throw new RuntimeException("Request failed", e);
        }
    }

    // тесты для проверки URL
    @Nested
    class UrlCheckControllerTest {
        @Test
        public void addUrlCheckTest() throws SQLException, IOException {
            Javalin additionalApp = App.getApp();

            String url = mockServer.url("/").toString().replaceAll("/$", "");

            JavalinTest.test(additionalApp, (server, client) -> {
                String requestBody = "url=" + url;
                var postResponse = client.post("/urls", requestBody);
                assertThat(postResponse.code()).isEqualTo(HttpServletResponse.SC_OK);

                Url actualUrl = UrlRepository.findByName(url).orElse(null);
                assertThat(actualUrl).isNotNull();

                var checkResponse = client.post("/urls/" + actualUrl.getId() + "/checks");
                assertThat(checkResponse.code()).isEqualTo(HttpServletResponse.SC_OK);

                var actualCheck = UrlCheckRepository.findLastCheckByUrlId(actualUrl.getId())
                        .orElse(null);
                assertThat(actualCheck).isNotNull();

                assertThat(actualCheck.getTitle()).isEqualTo("Test page");
                assertThat(actualCheck.getH1()).isEqualTo("Test page.");
                assertThat(actualCheck.getDescription()).isEqualTo("all right");
            });
        }

        @Test
        public void testAddCheckWithInvalidUrl() throws SQLException {
            Url url = new Url(WRONG_URL);
            UrlRepository.save(url);
            Long id = url.getId();

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/" + id + "/checks")
                    .asString();

            assertThat(response.getStatus()).isEqualTo(302);
            assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/urls/" + id);

            HttpResponse<String> showResponse = Unirest
                    .get(baseUrl + "/urls/" + id)
                    .asString();
            assertThat(showResponse.getBody()).contains("Некорректный адрес");
        }
        // для URL без проверок
        @Test
        public void testShowUrlWithoutChecks() throws SQLException {
            Url url = UrlRepository.findByName(CORRECT_URL).orElseThrow();
            Long id = url.getId();

            // Удаляем все проверки
            UrlCheckRepository.truncateDB();

            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/" + id).asString();
            String body = response.getBody();

            assertThat(body)
                    .contains(CORRECT_URL)
                    .containsPattern("<tbody>\\s*</tbody>"); // Пустая таблица проверок
        }

    }

    // Тесты для репозиториев
    @Nested
    class RepositoryTest {
        @Test
        public void testFindLatestChecks() throws SQLException, InterruptedException {
            Url url1 = new Url("https://example1.com");
            Url url2 = new Url("https://example2.com");
            UrlRepository.save(url1);
            UrlRepository.save(url2);

            UrlCheck check1 = new UrlCheck(200, "Title1", "H1-1", "Desc1", url1.getId());
            Thread.sleep(100);
            UrlCheck check2 = new UrlCheck(200, "Title2", "H1-2", "Desc2", url1.getId());
            UrlCheck check3 = new UrlCheck(404, "Title3", "H1-3", "Desc3", url2.getId());

            UrlCheckRepository.save(check1);
            UrlCheckRepository.save(check2);
            UrlCheckRepository.save(check3);

            Map<Long, UrlCheck> latestChecks = UrlCheckRepository.findLatestChecks();

            assertThat(latestChecks).hasSize(2);
            assertThat(latestChecks.get(url1.getId())).isNotNull();
            assertThat(latestChecks.get(url1.getId()).getId()).isEqualTo(check2.getId());
            assertThat(latestChecks.get(url2.getId())).isNotNull();
            assertThat(latestChecks.get(url2.getId()).getStatusCode()).isEqualTo(404);
        }

        @Test
        public void testGetAllChecks() throws SQLException {
            Url url = new Url("https://example.com");
            UrlRepository.save(url);

            UrlCheck check1 = new UrlCheck(200, "Title1", "H1-1", "Desc1", url.getId());
            UrlCheck check2 = new UrlCheck(200, "Title2", "H1-2", "Desc2", url.getId());
            UrlCheckRepository.save(check1);
            UrlCheckRepository.save(check2);

            List<UrlCheck> checks = UrlCheckRepository.getAllChecks(url.getId());
            assertThat(checks).hasSize(2);
            assertThat(checks.get(0).getCreatedAt().after(checks.get(1).getCreatedAt()));
        }

        @Test
        public void testDeleteUrl() throws SQLException {
            Url url = new Url("https://to-delete.com");
            UrlRepository.save(url);
            Long id = url.getId();

            boolean deleted = UrlRepository.delete(id);
            assertThat(deleted).isTrue();

            Optional<Url> result = UrlRepository.findById(id);
            assertThat(result).isEmpty();
        }

        @Test
        public void testFindByIdNonExistent() throws SQLException {
            Optional<Url> result = UrlRepository.findById(9999L);
            assertThat(result).isEmpty();
        }
    }
}
