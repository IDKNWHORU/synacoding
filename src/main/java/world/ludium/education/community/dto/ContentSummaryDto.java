package world.ludium.education.community.dto;

import java.sql.Timestamp;
import java.util.UUID;

public class ContentSummaryDto {
    private UUID contentId;
    private String title;
    private UUID usrId;
    private Timestamp createAt;
    private String type;
    private boolean visible;
    private boolean isPinned;
    private Integer pinOrder;

    public ContentSummaryDto() {
    }

    public ContentSummaryDto(UUID contentId, String title, UUID usrId, Timestamp createAt, String type, boolean visible, boolean isPinned, Integer pinOrder) {
        this.contentId = contentId;
        this.title = title;
        this.usrId = usrId;
        this.createAt = createAt;
        this.type = type;
        this.visible = visible;
        this.isPinned = isPinned;
        this.pinOrder = pinOrder;
    }

    public UUID getContentId() {
        return contentId;
    }

    public String getTitle() {
        return title;
    }

    public UUID getUsrId() {
        return usrId;
    }

    public Timestamp getCreateAt() {
        return createAt;
    }

    public String getType() {
        return type;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public Integer getPinOrder() {
        return pinOrder;
    }
}
