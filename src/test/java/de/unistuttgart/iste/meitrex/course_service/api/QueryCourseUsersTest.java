package de.unistuttgart.iste.meitrex.course_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseMembershipEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseMembershipRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@GraphQlApiTest
class QueryUserIdsByCourseIdTest {

    @Autowired
    private CourseMembershipRepository membershipRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void testNoUsersForCourse(GraphQlTester tester) {
        UUID courseId = UUID.randomUUID();

        executeUserIdsQuery(tester, courseId)
                .path("_internal_userIdsByCourseId[*]")
                .entityList(UUID.class)
                .hasSize(0);
    }

    @Test
    void testUserIdsReturned(GraphQlTester tester) {
        CourseEntity course = courseRepository.save(createTestCourse());
        UUID courseId = course.getId();

        List<UUID> expectedUserIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        expectedUserIds.forEach(userId -> membershipRepository.save(
                CourseMembershipEntity.builder()
                        .userId(userId)
                        .courseId(courseId)
                        .role(de.unistuttgart.iste.meitrex.generated.dto.UserRoleInCourse.STUDENT)
                        .build()
        ));

        List<UUID> actualUserIds = executeUserIdsQuery(tester, courseId)
                .path("_internal_userIdsByCourseId[*]")
                .entityList(UUID.class)
                .get();

        assertThat(actualUserIds).containsExactlyInAnyOrderElementsOf(expectedUserIds);
    }

    private GraphQlTester.Response executeUserIdsQuery(GraphQlTester tester, UUID courseId) {
        String query = """
                query($courseId: UUID!) {
                    _internal_userIdsByCourseId(courseId: $courseId)
                }
                """;

        return tester.document(query)
                .variable("courseId", courseId)
                .execute();
    }

    private static CourseEntity createTestCourse() {
        return CourseEntity.builder()
                .startDate(OffsetDateTime.parse("2021-01-01T00:00:00+00:00"))
                .endDate(OffsetDateTime.parse("2021-01-01T00:00:00+00:00"))
                .title("Test Course")
                .description("Test Description")
                .chapters(new ArrayList<>())
                .build();
    }
}
