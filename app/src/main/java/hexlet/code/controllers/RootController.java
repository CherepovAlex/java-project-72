package hexlet.code.controllers;

import io.javalin.http.Handler;

public class RootController {
    // рендерит главную страницу.
    public static Handler welcome = ctx -> {
        ctx.render("index.html");
    };
}
