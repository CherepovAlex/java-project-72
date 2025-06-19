package hexlet.code.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.sql.Timestamp;
import java.time.Instant;

@ToString
@Getter
@Setter
public final class UrlCheck {

    private Long id;
    private int statusCode;
    private String title;
    private String h1;
    private String description;
    private Timestamp createdAt;
    private Long urlId;

    public UrlCheck(int statusCode, String title, String h1, String description, Long urlId) {
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
        this.urlId = urlId;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Instant getCreatedAtToInstant() {
        return createdAt != null ? createdAt.toInstant() : null;
    }
}
