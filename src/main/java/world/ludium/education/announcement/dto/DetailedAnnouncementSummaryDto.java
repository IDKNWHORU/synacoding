package world.ludium.education.announcement.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class DetailedAnnouncementSummaryDto {
    private UUID detailId;
    private UUID postingId;
    private String title;
    private String status;
    private Timestamp createAt;
    private boolean isPinned;
    private Integer pinOrder;

    public DetailedAnnouncementSummaryDto() {
    }

    public DetailedAnnouncementSummaryDto(UUID detailId, UUID postingId, String title, String status, Timestamp createAt, boolean isPinned, Integer pinOrder) {
        this.detailId = detailId;
        this.postingId = postingId;
        this.title = title;
        this.status = status;
        this.createAt = createAt;
        this.isPinned = isPinned;
        this.pinOrder = pinOrder;
    }

    public UUID getDetailId() {
        return detailId;
    }

    public UUID getPostingId() {
        return postingId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
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
