package de.unistuttgart.iste.meitrex.course_service.client;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.course_service.exception.CourseServiceConnectionException;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseMembershipEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseMembershipRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import de.unistuttgart.iste.meitrex.generated.dto.UserRoleInCourse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
public class CourseServiceClientTest {

    private CourseEntity course;

    private GraphQlClient graphQlClient;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    @BeforeEach
    void setUp() {
        this.course = courseRepository.save(createTestCourse());
        LoggedInUser loggedInUser = userWithMembershipInCourseWithId(course.getId(), LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

        WebTestClient webTestClient = MockMvcWebTestClient.bindToApplicationContext(applicationContext)
                .configureClient().baseUrl("/graphql").defaultHeaders(httpHeaders -> httpHeaders.add("CurrentUser", getJson(loggedInUser))).build();

        graphQlClient = GraphQlClient.builder(new WebTestClientTransport(webTestClient)).build();
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


    @Test
    void testQueryMembershipsInCourseNullCourseId() {
        final CourseServiceClient courseServiceClient = new CourseServiceClient(graphQlClient);
        try {
            courseServiceClient.queryMembershipsInCourse(null);
            assertThat(true, is(false));
        } catch (CourseServiceConnectionException e) {
            assertThat(e.getMessage(), is("Error fetching courseMemberships from CourseService: Course ID cannot be null"));
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


    /*
        Copied from de.unistuttgart.iste.meitrex.common.testutil.HeaderUtils
     */
    private static String getJson(final LoggedInUser user) {

        final StringBuilder courseMemberships = new StringBuilder().append("[");
        final StringBuilder realmRoles = new StringBuilder().append("[");

        for (int i = 0; i < user.getCourseMemberships().size(); i++) {
            final LoggedInUser.CourseMembership courseMembership = user.getCourseMemberships().get(i);

            courseMemberships.append("{")
                    .append("\"courseId\": \"").append(courseMembership.getCourseId()).append("\",")
                    .append("\"role\": \"").append(courseMembership.getRole()).append("\",")
                    .append("\"published\": ").append(courseMembership.isPublished()).append(",")
                    .append("\"startDate\": \"").append(courseMembership.getStartDate()).append("\",")
                    .append("\"endDate\": \"").append(courseMembership.getEndDate()).append("\"")
                    .append("}");

            if (i < user.getCourseMemberships().size() - 1) {
                courseMemberships.append(",");
            }
        }

        courseMemberships.append("]");

        List<String> roleStrings = LoggedInUser.RealmRole.getRoleStringsFromEnum(user.getRealmRoles()).stream().toList();

        for (int j = 0; j < roleStrings.size(); j++) {

            realmRoles.append("\"")
                    .append(roleStrings.get(j))
                    .append("\"");

            if (j < roleStrings.size() - 1) {
                realmRoles.append(",");
            }
        }

        realmRoles.append("]");

        return """
                {
                  "id": "%s",
                  "userName": "%s",
                  "firstName": "%s",
                  "lastName": "%s",
                  "courseMemberships": %s,
                  "realmRoles": %s
                }
                """
                .formatted(user.getId(),
                        user.getUserName(),
                        user.getFirstName(),
                        user.getLastName(),
                        courseMemberships.toString(),
                        realmRoles.toString());
    }
}
