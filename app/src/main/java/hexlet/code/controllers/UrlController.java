package hexlet.code.controllers;

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
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.Optional;

@Slf4j
public class UrlController {

    public static Handler createUrl = ctx -> {
        // Получаем URL из формы (поле с name="url")
        String inputUrl = ctx.formParam("url");
        log.debug("Processing URL input: {}", inputUrl);

        if (inputUrl == null || inputUrl.isEmpty()) {
            setFlashMessage(ctx, "URL не может быть пустым", "danger");
            ctx.redirect("/");
            return;
        }
        try {
            // Добавляем протокол только если URL не начинается с http:// или https://
            // и если это вообще похоже на URL
            if (!inputUrl.matches("^https?://.*") && inputUrl.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
                inputUrl = "http://" + inputUrl;
            }

            // Парсим и нормализуем URL
            URI uri = new URI(inputUrl).normalize();
            URL url = uri.toURL();

            // Дополнительная валидация
            if (url.getHost() == null || url.getHost().isEmpty()) {
                throw new URISyntaxException(inputUrl, "Invalid host");
            }

            // Получаем нормализованный URL (протокол + домен + порт)
            String normalizedUrl = normalizeUrl(url);
            log.debug("Normalized URL: {}", normalizedUrl);

            // Проверяем существование URL в базе
            Optional<Url> existingUrl = UrlRepository.findByName(normalizedUrl);
            if (existingUrl.isPresent()) {
                log.info("URL already exists: {}", normalizedUrl);
                setFlashMessage(ctx, "Страница уже существует", "warning");
            } else {
                Url newUrl = new Url(normalizedUrl);
                UrlRepository.save(newUrl);
                log.info("URL added successfully: {}", normalizedUrl);
                setFlashMessage(ctx, "Страница успешно добавлена", "success");
            }

            ctx.redirect("/urls");

        } catch (URISyntaxException | IllegalArgumentException | MalformedURLException e) {
            log.error("Invalid URL syntax: {}", inputUrl, e);
            setFlashMessage(ctx, "Некорректный URL", "danger");
            ctx.redirect("/");
        } catch (Exception e) {
            log.error("Error processing URL: {}", e.getMessage(), e);
            setFlashMessage(ctx, "Ошибка при обработке URL", "danger");
            ctx.redirect("/");
        }
    };

    public static Handler showUrls = ctx -> {
        log.debug("Попытка загрузить URLs");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        List<Url> urls = UrlRepository.getUrls();
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

        int lastPage = urls.size() + 1;
        int currentPage = page + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .toList();

        ctx.attribute("urls", urls);
        ctx.attribute("urlChecks", urlChecks);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/showURLs.html");

        log.info("URLS PAGE IS RENDERED");
    };

    public static Handler showUrlById = ctx -> {
        log.info("Trying to find URL by its id");
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        Url url = UrlRepository.findById(id).orElse(null);
        if (url == null) {
            throw new NotFoundResponse("The ulr you are looking for is not found");
        }
        List<UrlCheck> checks = UrlCheckRepository.getAllChecks(url.getId());

        ctx.attribute("url", url);
        ctx.attribute("checks", checks);
        ctx.render("urls/show.html");
    };

    private static String normalizeUrl(URL url) {
        StringBuilder result = new StringBuilder();

        // Протокол
        String protocol = url.getProtocol();
        result.append(protocol).append("://");

        // Домен (приводим к нижнему регистру)
        String host = url.getHost().toLowerCase();
        result.append(host);

        // Порт (если указан и не стандартный для протокола)
        int port = url.getPort();
        if (port != -1) {
            boolean isDefaultPort = (protocol.equals("http") && port == 80)
                    || (protocol.equals("https") && port == 443);
            if (!isDefaultPort) {
                result.append(":").append(port);
            }
        }
        return result.toString();
    }

    private static void setFlashMessage(Context ctx, String message, String type) {
        ctx.sessionAttribute("flash", message);
        ctx.sessionAttribute("flash-type", type);
    }
}
