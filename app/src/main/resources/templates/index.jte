@param String flash
@param String flashType

<!DOCTYPE html>
<html xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout" layout:decorate="~{layouts/application.html}">
<section layout:fragment="content">
    <div class="container-fluid bg-dark p-5">
        <div class="row">
            <div class="col-md-10 col-lg-8 mx-auto text-white">
                @if (flash != null)
                    <div class="alert alert-${flashType}">
                        ${flash}
                    </div>
                @endif
                <h1 class="display-3 mb-0">Page analiser</h1>
                <p class="lead">Check sites for SEO-optimization</p>
                <!-- Форма для ввода URL -->
                <form action="/urls" method="post" class="rss-form text-body">
                    <div class="row">
                        <div class="col">
                            <div class="form-floating">
                                <input id="url-input" autofocus required name="url"
                                       type="url" class="form-control w-100" aria-label="url" placeholder="http://www.yandex.ru" aria-describedby="urlHelp" autocomplete="off">
                                <label for="url-input">Link</label>
                            </div>
                        </div>
                        <div class="col-auto">
                            <button type="submit" class="h-100 btn btn-lg btn-primary px-sm-5">Check</button>
                        </div>
                    </div>
                    <div class="row">
                        <div id="urlHelp" class="form-text">For example: http://www.example.com</div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</section>
</html>