package de.unistuttgart.iste.meitrex.course_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.*;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.CourseMembership;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.ChapterRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.course_service.test_utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.WebGraphQlTester;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for the `deleteChapter` mutation.
 */
@ContextConfiguration(classes = MockTestPublisherConfiguration.class)
@GraphQlApiTest
class MutationDeleteChapterTest {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ChapterRepository chapterRepository;


    /**
     * Given a valid chapter id
     * When the deleteChapter mutation is executed
     * Then the chapter is deleted and the uuid is returned
     */
    @Test
    void testDeletion(WebGraphQlTester tester) {
        // create a course in the database
        final var course = courseRepository.save(TestUtils.dummyCourseBuilder().build());
        // create two chapters in the database
        final var chapters = Stream.of(
                        TestUtils.dummyChapterBuilder().courseId(course.getId()).title("Chapter 1").build(),
                        TestUtils.dummyChapterBuilder().courseId(course.getId()).title("Chapter 2").build())
                .map(chapterRepository::save)
                .toList();

        final String query = """
                mutation {
                    deleteChapter(id: "%s")
                }""".formatted(chapters.get(0).getId());

        tester = HeaderUtils.addCurrentUserHeader(tester, new LoggedInUser(
                UUID.randomUUID(),
                "TestUser",
                "Test",
                "User",
                "Testy",
                List.of(new CourseMembership(course.getId(),
                        UserRoleInCourse.ADMINISTRATOR,
                        false,
                        OffsetDateTime.now(),
                        OffsetDateTime.now())
                ), Collections.emptySet()));

        tester.document(query)
                .execute()
                .path("deleteChapter").entity(UUID.class).isEqualTo(chapters.get(0).getId());

        final var entities = chapterRepository.findAll();
        assertThat(entities, hasSize(1));
        assertThat(entities.get(0).getId(), equalTo(chapters.get(1).getId()));
    }

    /**
     * Given an invalid chapter id
     * When the deleteChapter mutation is executed
     * Then an error is returned
     */
    @Test
    void testDeletionInvalidId(WebGraphQlTester tester) {
        final UUID chapterId = UUID.randomUUID();

        final String query = """
                mutation {
                    deleteChapter(id: "%s")
                }""".formatted(chapterId);

        tester = HeaderUtils.addCurrentUserHeader(tester, new LoggedInUser(
                UUID.randomUUID(),
                "TestUser",
                "Test",
                "User",
                "Testy",
                List.of(new CourseMembership(UUID.randomUUID(),
                        UserRoleInCourse.ADMINISTRATOR,
                        false,
                        OffsetDateTime.now(),
                        OffsetDateTime.now())
                ), Collections.emptySet()));

        tester.document(query)
                .execute()
                .errors()
                .satisfy(responseErrors -> assertThat(responseErrors, hasSize(1)));
    }

}
