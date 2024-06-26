package de.unistuttgart.iste.meitrex.course_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.*;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.ChapterEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.ChapterRepository;
import de.unistuttgart.iste.meitrex.course_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import de.unistuttgart.iste.meitrex.generated.dto.UserRoleInCourse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.UserRoleInCourse.STUDENT;


@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
@GraphQlApiTest
public class AuthorizationTest {

    @Autowired
    private ChapterRepository chapterRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser currentUser = userWithMembershipInCourseWithId(courseId, STUDENT);


    @Test
    void testCreateCourseCourseCreatorOnly(WebGraphQlTester tester) {

        final String query = """
                mutation {
                    createCourse(
                        input: {
                            title: "New Course"
                            description: "This is a new course"
                            startDate: "2020-01-01T00:00:00.000Z"
                            endDate: "2021-01-01T00:00:00.000Z"
                            published: false
                        }
                    ) {
                        id
                        title
                        description
                        startDate
                        endDate
                        published
                        chapters {
                            elements {
                                id
                            }
                        }
                    }
                }""";

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationTest::assertIsMissingGlobalPermission);
    }

    public static void assertIsMissingGlobalPermission(final List<ResponseError> graphqlErrors) {
        MatcherAssert.assertThat(graphqlErrors, Matchers.hasSize(1));
        MatcherAssert.assertThat(graphqlErrors.get(0).getExtensions().get("exception"), Matchers.is("MissingGlobalPermissionsException"));
        MatcherAssert.assertThat(graphqlErrors.get(0).getMessage(), Matchers.containsString("User is missing the required permissions."));
    }

    @Test
    @Transactional
    void testUpdateCourseAdminOnly(WebGraphQlTester tester) {

        final String query = """
                mutation {
                    updateCourse(
                        input: {
                            id: "%s"
                            title: "New Course"
                            description: "This is a new course"
                            startDate: "2000-01-01T00:00:00.000Z"
                            endDate: "2001-01-01T00:00:00.000Z"
                            startYear: 2021
                            yearDivision: FIRST_SEMESTER
                            published: false
                        }
                    ) {
                        id
                        title
                        description
                        startDate
                        endDate
                        startYear
                        yearDivision
                        published
                        chapters {
                            elements {
                                id
                            }
                        }
                    }
                }""".formatted(courseId);

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);

    }

    @Test
    void testDeleteCourseAdminOnly(WebGraphQlTester tester) {

        final String query = """
                mutation {
                    deleteCourse(id: "%s")
                }""".formatted(courseId);

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);

    }

    @Test
    void testCreateChapterAdminOnly(WebGraphQlTester tester) {

        final String query = """
                mutation {
                    createChapter(
                        input: {
                            courseId: "%s"
                            title: "New Chapter"
                            description: "This is a new chapter"
                            startDate: "2020-01-01T00:00:00.000Z"
                            endDate: "2021-01-01T00:00:00.000Z"
                            number: 1
                        }
                    ) {
                        id
                        title
                        description
                        startDate
                        endDate
                        number
                    }
                }""".formatted(courseId);

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }

    @Test
    void testUpdateChapterAdminOnly(WebGraphQlTester tester) {

        final ChapterEntity chapterEntity = chapterRepository.save(TestUtils.dummyChapterBuilder().courseId(courseId).build());

        final String query = """
                mutation {
                    updateChapter(
                        input: {
                            id: "%s"
                            title: "New Chapter"
                            description: "This is a new chapter"
                            startDate: "2020-01-01T00:00:00.000Z"
                            endDate: "2021-01-01T00:00:00.000Z"
                            number: 1
                        }
                    ) {
                        id
                        title
                        description
                        startDate
                        endDate
                        number
                    }
                }""".formatted(chapterEntity.getId());

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);

    }

    @Test
    void testDeleteChapterAdminOnly(WebGraphQlTester tester) {
        final ChapterEntity chapterEntity = chapterRepository.save(TestUtils.dummyChapterBuilder().courseId(courseId).build());

        final String query = """
                mutation {
                    deleteChapter(id: "%s")
                }""".formatted(chapterEntity.getId());

        tester = HeaderUtils.addCurrentUserHeader(tester, new LoggedInUser(
                UUID.randomUUID(),
                "TestUser",
                "Test",
                "User",
                List.of(new LoggedInUser.CourseMembership(courseId,
                        STUDENT,
                        false,
                        OffsetDateTime.now(),
                        OffsetDateTime.now())
                ), Collections.emptySet()));

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }


    @Test
    void testCreateMembershipAdminOnly(WebGraphQlTester tester) {

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(UUID.randomUUID())
                .setCourseId(courseId)
                .setRole(UserRoleInCourse.STUDENT)
                .build();

        final String query = """
                mutation {
                    createMembership(
                        input: {
                            userId: "%s"
                            courseId: "%s"
                            role: %s
                        }
                    ) {
                        userId
                        courseId
                        role
                    }
                }
                """.formatted(expectedDto.getUserId(), expectedDto.getCourseId(), expectedDto.getRole());

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }

    @Test
    void testUpdateMembershipAdminOnly(WebGraphQlTester tester) {

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(currentUser.getId())
                .setCourseId(courseId)
                .setRole(UserRoleInCourse.STUDENT)
                .build();

        final String query = """
                mutation {
                    updateMembership(
                        input: {
                            userId: "%s"
                            courseId: "%s"
                            role: %s
                        }
                    ) {
                        userId
                        courseId
                        role
                    }
                }
                """.formatted(expectedDto.getUserId(), expectedDto.getCourseId(), expectedDto.getRole());

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }

    @Test
    void testDeleteMembershipAdminOnly(WebGraphQlTester tester) {

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(currentUser.getId())
                .setCourseId(courseId)
                .setRole(UserRoleInCourse.STUDENT)
                .build();

        final String query = """
                mutation {
                    deleteMembership(
                        input: {
                            userId: "%s"
                            courseId: "%s"
                            role: %s
                        }
                    ) {
                        userId
                        courseId
                        role
                    }
                }
                """.formatted(expectedDto.getUserId(), expectedDto.getCourseId(), expectedDto.getRole());

        tester.document(query)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }


}
