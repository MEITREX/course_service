package de.unistuttgart.iste.gits.course_service.persistence.repository;

import de.unistuttgart.iste.gits.course_service.persistence.entity.ChapterEntity;
import de.unistuttgart.iste.meitrex.common.persistence.MeitrexRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repository for {@link ChapterEntity}.
 */
public interface ChapterRepository extends MeitrexRepository<ChapterEntity, UUID>, JpaSpecificationExecutor<ChapterEntity> {


}
