package de.unistuttgart.iste.gits.course_service.persistence.repository;

import de.unistuttgart.iste.gits.course_service.persistence.entity.ChapterEntity;
import de.unistuttgart.iste.gits.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link CourseEntity}.
 */
@Repository
public interface CourseRepository extends MeitrexRepository<CourseEntity, UUID>, JpaSpecificationExecutor<CourseEntity> {

    /**
     * retrieves a course by one of its Chapters
     * @param chapterEntity chapter to be used
     * @return Course Entity if existing
     */
    Optional<CourseEntity> findCourseEntityByChaptersContaining(ChapterEntity chapterEntity);

}
