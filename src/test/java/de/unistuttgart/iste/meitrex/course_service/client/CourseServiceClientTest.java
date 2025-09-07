package de.unistuttgart.iste.meitrex.course_service.client;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.ChapterEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.ChapterRepository;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.course_service.exception.CourseServiceConnectionException;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseMembershipEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseMembershipRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.course_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.generated.dto.Chapter;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import de.unistuttgart.iste.meitrex.generated.dto.UserRoleInCourse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.*;

/**
 * This class is used to test the ContentServiceClient.
 */
@GraphQlApiTest
class CourseServiceClientTest {

    private CourseEntity course;

    private GraphQlClient graphQlClient;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    @BeforeEach
    void setUp() {
        this.course = courseRepository.save(createTestCourse());
        final WebTestClient webTestClient = MockMvcWebTestClient.bindToApplicationContext(applicationContext)
                .configureClient().baseUrl("/graphql").build();

        graphQlClient = GraphQlClient.builder(new WebTestClientTransport(webTestClient)).build();
    }

    @Test
    void testQueryContentsOfChapter() {
        final CourseServiceClient contentServiceClient = new CourseServiceClient(graphQlClient);
        final var course = courseRepository.save(TestUtils.dummyCourseBuilder().build());

        final ChapterEntity chapterEntity = chapterRepository.save(TestUtils.dummyChapterBuilder().courseId(course.getId()).build());

        final List<Chapter> actualChapters = contentServiceClient.queryChapterByCourseId(course.getId());

        assertThat(actualChapters, hasSize(1));

        assertThat(actualChapters.getFirst().getCourse().getId(), is(course.getId()));

        ChapterEntity compareEntity = modelMapper.map(actualChapters.getFirst(), ChapterEntity.class);
        compareEntity.setCourseId(actualChapters.getFirst().getCourse().getId());

        assertThat(compareEntity, is(chapterEntity));
    }

    @Test
    void testQueryCourseById() throws CourseServiceConnectionException {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);

        var courseResult = courseServiceClient.queryCourseById(course.getId());

        assertThat(courseResult, notNullValue());
        assertThat(courseResult.getTitle(), is(course.getTitle()));
    }

    @Test
    void testQueryCourseByWrongId() {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);
        final UUID wrongCourseId = UUID.randomUUID();
        try {
            courseServiceClient.queryCourseById(wrongCourseId);
            assertThat(true, is(false));
        } catch (CourseServiceConnectionException e) {
            assertThat(e.getMessage(), containsString("Entities(s) with id(s) %s not found".formatted(wrongCourseId)));
        }
    }


    @Test
    void testQueryCourseByNullCourseId() {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);
        try {
            courseServiceClient.queryCourseById(null);
            assertThat(true, is(false));
        } catch (CourseServiceConnectionException e) {
            assertThat(e.getMessage(), is("Error fetching course from CourseService: Course ID cannot be null"));
        }
    }


    @Test
    void testQueryMembershipsInCourse() throws CourseServiceConnectionException {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);

        final UUID userId1 = UUID.randomUUID();
        final UUID userId2 = UUID.randomUUID();
        final UUID userId3 = UUID.randomUUID();

        courseMembershipRepository.save(CourseMembershipEntity.builder()
                .userId(userId1)
                .courseId(course.getId())
                .role(UserRoleInCourse.ADMINISTRATOR)
                .build());
        courseMembershipRepository.save(CourseMembershipEntity.builder()
                .userId(userId2)
                .courseId(course.getId())
                .role(UserRoleInCourse.STUDENT)
                .build());
        courseMembershipRepository.save(CourseMembershipEntity.builder()
                .userId(userId3)
                .courseId(course.getId())
                .role(UserRoleInCourse.STUDENT)
                .build());

        List<CourseMembership> queriedMemberships = courseServiceClient.queryMembershipsInCourse(course.getId());

        assertThat(queriedMemberships, hasSize(3));

        System.out.println(queriedMemberships);

        CourseMembership membership1 = queriedMemberships.get(0);
        CourseMembership membership2 = queriedMemberships.get(1);
        CourseMembership membership3 = queriedMemberships.get(2);

        assertThat(membership1.getCourseId(), is(course.getId()));
        assertThat(membership1.getRole(), is(UserRoleInCourse.ADMINISTRATOR));
        assertThat(membership1.getUserId(), is(userId1));

        assertThat(membership2.getCourseId(), is(course.getId()));
        assertThat(membership2.getRole(), is(UserRoleInCourse.STUDENT));
        assertThat(membership2.getUserId(), is(userId2));

        assertThat(membership3.getCourseId(), is(course.getId()));
        assertThat(membership3.getRole(), is(UserRoleInCourse.STUDENT));
        assertThat(membership3.getUserId(), is(userId3));

        List<CourseMembership> queriedStudents = queriedMemberships.stream()
                .filter(membership -> membership.getRole() == UserRoleInCourse.STUDENT)
                .toList();

        assertThat(queriedStudents, hasSize(2));

        List<UUID> studentIds = queriedStudents.stream().map(CourseMembership::getUserId).toList();

        assertThat(studentIds, hasSize(2));

        System.out.println(studentIds);

    }


    @Test
    void testQueryMembershipsInCourseNoMembers() {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);

        try {
            courseServiceClient.queryMembershipsInCourse(course.getId());
            assertThat(true, is(false));
        } catch (CourseServiceConnectionException e) {
            assertThat(e.getMessage(), is("Error fetching courseMemberships from CourseService: CourseMembership List is empty."));
        }

    }

    @Test
    void testQueryMembershipsInCourseWrongCourseId() {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);
        final UUID wrongCourseId = UUID.randomUUID();
        try {
            courseServiceClient.queryMembershipsInCourse(wrongCourseId);
            assertThat(true, is(false));
        } catch (CourseServiceConnectionException e) {
            assertThat(e.getMessage(), containsString("Entities(s) with id(s) %s not found".formatted(wrongCourseId)));
        }
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
