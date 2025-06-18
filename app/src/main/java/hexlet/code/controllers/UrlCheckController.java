package hexlet.code.controllers;
// Этот контроллер отвечает за проверку URL (анализ его содержимого) и сохранение результатов проверки в базу данных.
// Он использует Unirest для HTTP-запросов и Jsoup для парсинга HTML.
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Handler;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.sql.Timestamp;
// Аннотация Lombok для автоматического создания логгера
@Slf4j
public class UrlCheckController {
    // Анализ HTML-страницы и сохранение результатов проверки
    // Это статическое поле типа Handler, которое обрабатывает POST-запрос на добавление проверки URL
    public static Handler addCheck = ctx -> {
        log.debug("addCheck Handler: trying to save an UrlCheck entity to DB");
        // Получаем ID URL из параметра пути
        Long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);
        log.info("Url's id is " + id);
        // Находим URL по ID в БД
        Url url = UrlRepository.findById(id).orElse(null);
        log.info("Url is" + url);

        try {
            // Выполняем GET-запрос к URL
            HttpResponse<String> response = Unirest
                    .get(url.getName())
                    .asString();
            // Получаем статус код ответа
            int statusCode = response.getStatus();
            // Парсим HTML-документ из тела ответа
            Document document = Jsoup.parse(response.getBody());
            // Получаем заголовок страницы
            String title = document.title();                         // Извлечение title
            // Получаем первый элемент h1
            Element h1Element = document.selectFirst("h1"); // Извлечение h1
            // Если h1 не найден, используем пустую строку
            String h1 = h1Element == null
                    ? ""
                    : h1Element.text();
            // Получаем мета-описание
            Element descriptionElement = document.selectFirst("meta[name=description]");
            String description = descriptionElement == null
                    ? ""
                    : descriptionElement.attr("content");
            // Создаем timestamp текущего времени
            Timestamp createdAt = new Timestamp(System.currentTimeMillis());

            log.info("Trying to create a new object of UrlCheck class");
            // Создаем объект UrlCheck с полученными данными
            UrlCheck urlCheckToAdd = new UrlCheck(statusCode, title, h1, description, url.getId());
            // Логируем данные проверки
            log.info("UrlCheckToAdd's fields are these: statusCode " + statusCode + " title " + title + " h1 " + h1
                    + " description " + description + " createdAt " + createdAt + " urlId " + urlCheckToAdd.getUrlId());
            // Сохраняем проверку в базу данных
            UrlCheckRepository.save(urlCheckToAdd);     // Сохранение результатов
            // Устанавливаем флеш-сообщение об успехе
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
            log.info("Check is done and added to the DB");
        } catch (UnirestException e) {
            // Обработка ошибки Unirest (некорректный URL)
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        } catch (Exception e) {
            // Обработка других ошибок
            ctx.sessionAttribute("flash", e.getMessage());
            ctx.sessionAttribute("flash-type", "danger");
        }
        // Перенаправляем на страницу URL
        ctx.redirect("/urls/" + url.getId());
    };
}
