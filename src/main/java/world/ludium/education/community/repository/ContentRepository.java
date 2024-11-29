package world.ludium.education.community.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import world.ludium.education.community.dto.ContentSummaryDto;
import world.ludium.education.community.model.Content;

public interface ContentRepository extends JpaRepository<Content, UUID> {
  @Query("SELECT NEW world.ludium.education.community.dto.ContentSummaryDto(c.contentId, c.title, c.usrId, c.createAt, c.type, c.visible, c.isPinned, c.pinOrder) FROM Content c WHERE c.visible = :visible ORDER BY c.isPinned DESC, c.pinOrder DESC, c.createAt DESC")
  List<ContentSummaryDto> findAllByVisibleOrderByIsPinnedDescPinOrderDescCreateAtDesc(boolean visible);

  Optional<Content> findByContentIdAndVisible(UUID id, boolean visible);

  @Query("SELECT NEW world.ludium.education.community.dto.ContentSummaryDto(c.contentId, c.title, c.usrId, c.createAt, c.type, c.visible, c.isPinned, c.pinOrder) FROM Content c WHERE c.type = ?1 AND c.visible = ?2 ORDER BY c.createAt DESC LIMIT 1")
  Optional<ContentSummaryDto> findTop1ByTypeAndVisibleOrderByCreateAtDesc(String string, boolean visible);

  Optional<Content> findTop1ByVisibleOrderByPinOrder(boolean visible);
}