package hexlet.code.controllers;
// Класс отвечает за всю логику работы с URL в приложении
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.Optional;
// Аннотация Lombok для автоматического создания логгера
@Slf4j
public class UrlController {
    // Обработка добавления нового URL с валидацией и нормализацией.
    public static Handler createUrl = ctx -> {
        // Получаем URL из формы (поле с name="url")
        String inputUrl = ctx.formParam("url");
        // Логирование входящего URL
        log.debug("Processing URL input: {}", inputUrl);
        // Проверка на пустой URL
        if (inputUrl == null || inputUrl.isEmpty()) {
            setFlashMessage(ctx, "URL не может быть пустым", "danger");
            ctx.redirect("/");
            return;
        }
        try {
            // Проверяем, что URL начинается с протокола (с http:// или https://)
            if (!inputUrl.matches("^https?://.+")) {
                throw new MalformedURLException("URL должен начинаться с http:// или https://");
            }
            // Парсим и нормализуем URL
            URI uri = new URI(inputUrl).normalize();
            URL url = uri.toURL();
            // Проверяем обязательные компоненты URL
            if (url.getProtocol() == null || url.getHost() == null || url.getHost().isEmpty()) {
                throw new MalformedURLException("Невалидный URL");
            }
            // Собираем нормализованный URL (протокол + домен + порт)
            String normalizedUrl = normalizeUrl(url);
            log.debug("Normalized URL: {}", normalizedUrl);
            // Проверяем существование URL в базе - уникальность
            Optional<Url> existingUrl = UrlRepository.findByName(normalizedUrl);
            if (existingUrl.isPresent()) {
                log.info("URL already exists: {}", normalizedUrl);
                setFlashMessage(ctx, "Страница уже существует", "warning");
            } else {
                // Создание и сохранение нового URL
                Url newUrl = new Url(normalizedUrl);
                UrlRepository.save(newUrl);     // Сохранение в БД
                log.info("URL added successfully: {}", normalizedUrl);
                setFlashMessage(ctx, "Страница успешно добавлена", "success");
            }
            // Перенаправление на страницу со списком URL
            ctx.redirect("/urls");

        } catch (Exception e) {
            // Обработка ошибок
            log.error("Invalid URL: {}", inputUrl, e);
            setFlashMessage(ctx, "Некорректный URL", "danger");
            ctx.redirect("/");
        }
    };
    // Обработчик для отображения список URL с пагинацией
    public static Handler showUrls = ctx -> {
        log.debug("Попытка загрузить URLs");
        // Получение номера страницы (по умолчанию 1)
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        // Получение всех URL из базы данных
        List<Url> urls = UrlRepository.getUrls();
        // Создание карты последних проверок для каждого URL
        Map<Long, UrlCheck> urlChecks = new HashMap<>();
        try {
            urlChecks = UrlCheckRepository.findLatestChecks();
        } catch (SQLException e) {
            log.error("Error getting checks", e);
            ctx.sessionAttribute("flash", "Ошибка при получении данных проверок");
            ctx.sessionAttribute("flash-type", "danger");
        }
        log.debug("Found {} URLs and {} checks", urls.size(), urlChecks.size());
        log.debug("urls is: " + urls);
        log.debug("urlChecks is: " + urlChecks);
        // Настройка пагинации
        int lastPage = urls.size() + 1;
        int currentPage = page + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .toList();
        // Установка атрибутов для шаблона
        ctx.attribute("urls", urls);
        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        // Рендеринг шаблона
        ctx.render("urls/showURLs.html");
        log.info("URLS PAGE IS RENDERED");
    };
    // показывает информацию о конкретном URL и его проверках.
    public static Handler showUrlById = ctx -> {
        log.info("Trying to find URL by its id");
        // Получение ID URL из параметра пути
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        // Поиск URL по ID
        Url url = UrlRepository.findById(id).orElse(null);
        if (url == null) {
            throw new NotFoundResponse("The ulr you are looking for is not found");
        }
        // Получение всех проверок для URL
        List<UrlCheck> checks = UrlCheckRepository.getAllChecks(url.getId());
        // Установка атрибутов для шаблона
        ctx.attribute("url", url);
        ctx.attribute("checks", checks);
        // Рендеринг шаблона
        ctx.render("urls/show.html");
    };
    // Стандартизация URL (нижний регистр, удаление порта по умолчанию).
    private static String normalizeUrl(URL url) {
        StringBuilder result = new StringBuilder();
        // Приведение URL к стандартному виду (http или https)
        result.append(url.getProtocol()).append("://");
        // Добавление домена (в нижнем регистре)
        result.append(url.getHost().toLowerCase());
        // Обработка порта (если указан и не стандартный для протокола)
        int port = url.getPort();
        if (port != -1) {
            boolean isDefaultPort = (url.getProtocol().equals("http") && port == 80)
                    || (url.getProtocol().equals("https") && port == 443);
            if (!isDefaultPort) {
                result.append(":").append(port);
            }
        }
        return result.toString();
    }
    // метод устанавливает flash-сообщение.
    private static void setFlashMessage(Context ctx, String message, String type) {
        ctx.sessionAttribute("flash", message);
        ctx.sessionAttribute("flash-type", type);
    }
}
