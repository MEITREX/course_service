package de.unistuttgart.iste.meitrex.course_service.persistence.specification;

import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.generated.dto.CourseFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import static de.unistuttgart.iste.meitrex.common.util.SpecificationUtil.*;


public class CourseFilterSpecification {

    private CourseFilterSpecification() {
        // Utility class
    }

    public static Specification<CourseEntity> courseFilter(@Nullable CourseFilter filter) {
        if (filter == null) {
            return null;
        }
        return Specification.allOf(
                        stringFilter("title", filter.getTitle()),
                        stringFilter("description", filter.getDescription()),
                        dateTimeFilter("startDate", filter.getStartDate()),
                        dateTimeFilter("endDate", filter.getEndDate()),
                        booleanFilter("published", filter.getPublished()),
                        and(filter.getAnd(), CourseFilterSpecification::courseFilter),
                        not(filter.getNot(), CourseFilterSpecification::courseFilter))
                .or(
                        or(filter.getOr(), CourseFilterSpecification::courseFilter));
    }

}
