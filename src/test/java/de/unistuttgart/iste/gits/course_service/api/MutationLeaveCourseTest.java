package de.unistuttgart.iste.gits.course_service.api;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.gits.course_service.persistence.entity.CourseMembershipEntity;
import de.unistuttgart.iste.gits.course_service.persistence.entity.CourseMembershipPk;
import de.unistuttgart.iste.gits.course_service.persistence.repository.CourseMembershipRepository;
import de.unistuttgart.iste.gits.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.gits.generated.dto.CourseMembership;
import de.unistuttgart.iste.gits.generated.dto.UserRoleInCourse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@GraphQlApiTest
class MutationLeaveCourseTest {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    @InjectCurrentUserHeader
    private final UUID currentUserId = UUID.randomUUID();

    @Test
    void testLeaveCourse(final HttpGraphQlTester tester) {
        final CourseEntity course = courseRepository.save(CourseEntity.builder().title("Course 1")
                .description("This is course 1")
                .startDate(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"))
                .endDate(OffsetDateTime.parse("2021-01-01T00:00:00.000Z"))
                .published(true)
                .build());

        courseMembershipRepository.save(courseMembershipRepository.save(CourseMembershipEntity.builder()
                .courseId(course.getId())
                .userId(currentUserId)
                .role(UserRoleInCourse.STUDENT)
                .build()));

        final String query =
                """
                mutation($courseId: UUID!) {
                    leaveCourse(courseId: $courseId) {
                        userId
                        courseId
                        role
                    }
                }
                """;

        tester.document(query)
                .variable("courseId", course.getId())
                .execute()
                .path("leaveCourse").entity(CourseMembership.class).isEqualTo(CourseMembership.builder()
                        .setUserId(currentUserId)
                        .setCourseId(course.getId())
                        .setRole(UserRoleInCourse.STUDENT)
                        .build());

        assertThat(courseMembershipRepository.findById(new CourseMembershipPk(currentUserId, course.getId()))).isEmpty();
    }
}
