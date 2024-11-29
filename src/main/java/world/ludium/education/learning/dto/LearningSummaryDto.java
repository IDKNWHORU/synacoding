package world.ludium.education.learning.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class LearningSummaryDto {
    private UUID postingId;
    private String title;
    private Timestamp createAt;

    public LearningSummaryDto() {
    }

    public LearningSummaryDto(UUID postingId, String title, Timestamp createAt) {
        this.postingId = postingId;
        this.title = title;
        this.createAt = createAt;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public String getTitle() {
        return title;
    }

    public Timestamp getCreateAt() {
        return createAt;
    }
}
