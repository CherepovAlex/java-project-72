@import hexlet.code.model.Url
@param Url url

<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Сайт ${url.getId()}</title>
    <!-- Подключаем Bootstrap для стилизации -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container-lg mt-4">
        <h1>Сайт: ${url.getName()}</h1>
        <p>ID: ${url.getId()}</p>
        <p>Дата создания: ${url.getCreatedAt()}</p>
    </div>
</body>
</html>