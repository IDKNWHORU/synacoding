package world.ludium.education.announcement.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class AnnouncementSummaryDto {
    private UUID postingId;
    private String title;
    private Timestamp createAt;
    private boolean isPinned;
    private Integer pinOrder;

    public AnnouncementSummaryDto() {
    }

    public AnnouncementSummaryDto(UUID postingId, String title, Timestamp createAt, boolean isPinned, Integer pinOrder) {
        this.postingId = postingId;
        this.title = title;
        this.createAt = createAt;
        this.isPinned = isPinned;
        this.pinOrder = pinOrder;
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

    public boolean isPinned() {
        return isPinned;
    }

    public Integer getPinOrder() {
        return pinOrder;
    }
}
