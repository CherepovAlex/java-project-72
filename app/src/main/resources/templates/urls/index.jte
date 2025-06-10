@import java.util.List
@import hexlet.code.model.Url
@param List<Url> urls

<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sites</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
    <div class="container-lg mt-4">
        <h1>Sites</h1>
        <table class="table table-bordered table-hover mt-3">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Created at</th>
                </tr>
            </thead>
            <tbody>
                @for (var url : urls)
                    <tr>
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>${url.getCreatedAt().toString()}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    </div>
</body>
</html>