package world.ludium.education.announcement.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import world.ludium.education.announcement.dto.AnnouncementSummaryDto;
import world.ludium.education.announcement.model.Announcement;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

  @Query("SELECT NEW world.ludium.education.announcement.dto.AnnouncementSummaryDto(a.postingId, a.title, a.createAt, a.isPinned, a.pinOrder) FROM Announcement a ORDER BY a.isPinned DESC, a.pinOrder DESC, a.createAt DESC")
  List<AnnouncementSummaryDto> findAllByOrderByIsPinnedDescPinOrderDescCreateAtDesc();

  @Query("SELECT NEW world.ludium.education.announcement.dto.AnnouncementSummaryDto(a.postingId, a.title, a.createAt, a.isPinned, a.pinOrder) FROM Announcement a ORDER BY a.createAt DESC LIMIT 5")
  List<AnnouncementSummaryDto> findTop5ByOrderByCreateAtDesc();

  Optional<Announcement> findTop1ByOrderByPinOrder();
}
