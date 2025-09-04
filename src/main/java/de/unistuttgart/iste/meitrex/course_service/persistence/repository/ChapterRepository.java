package de.unistuttgart.iste.meitrex.course_service.persistence.repository;

import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.ChapterEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ChapterEntity}.
 */
public interface ChapterRepository extends MeitrexRepository<ChapterEntity, UUID>, JpaSpecificationExecutor<ChapterEntity> {

    /* 找到需要 Unlock 的章节（unlocked = false） */
    @Query("""
    SELECT c
    FROM Chapter c
    WHERE DATE(c.startDate) = DATE(:now)
    """)
    List<ChapterEntity> findChaptersToUnlock(@Param("now") OffsetDateTime now);

    @Query("""
    SELECT c
    FROM Chapter c
    WHERE DATE(c.endDate) = DATE(:now)
    """)
    List<ChapterEntity> findChaptersToLock(@Param("now") OffsetDateTime now);

    List<ChapterEntity> findChapterEntitiesByCourseId(UUID courseId);
}
