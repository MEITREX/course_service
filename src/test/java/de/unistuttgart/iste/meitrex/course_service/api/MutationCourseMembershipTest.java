package de.unistuttgart.iste.meitrex.course_service.api;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.CourseMembershipEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseMembershipRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.course_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import de.unistuttgart.iste.meitrex.generated.dto.UserRoleInCourse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.WebGraphQlTester;

import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.testutil.HeaderUtils.addCurrentUserHeader;
import static de.unistuttgart.iste.meitrex.common.testutil.TestUsers.userWithMembershipInCourseWithId;

@GraphQlApiTest
class MutationCourseMembershipTest {

    @Autowired
    private CourseMembershipRepository courseMembershipRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void createMembershipTest(WebGraphQlTester tester) {
        // create course
        final CourseEntity course = courseRepository.save(TestUtils.dummyCourseBuilder().build());

        // create admin user object
        final LoggedInUser adminUser = userWithMembershipInCourseWithId(course.getId(),
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR);
        // save course memberships of admin to repository
        TestUtils.saveCourseMembershipsOfUserToRepository(courseMembershipRepository, adminUser);

        // add admin user data to header
        tester = addCurrentUserHeader(tester, adminUser);

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(UUID.randomUUID())
                .setCourseId(course.getId())
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
                .path("createMembership")
                .entity(CourseMembership.class)
                .isEqualTo(expectedDto);
    }

    @Test
    void updateMembershipMembershipNotExistingTest(WebGraphQlTester tester) {
        final UUID courseId = UUID.randomUUID();

        // create admin user object
        final LoggedInUser adminUser = userWithMembershipInCourseWithId(courseId,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR);
        // save course memberships of admin to repository
        TestUtils.saveCourseMembershipsOfUserToRepository(courseMembershipRepository, adminUser);

        // add admin user data to header
        tester = addCurrentUserHeader(tester, adminUser);

        final UUID userIdToTest = UUID.randomUUID();

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(userIdToTest)
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
                .expect(responseError -> responseError.getMessage().contains("Membership with user id " +
                                                                             expectedDto.getUserId() +
                                                                             " and course id " +
                                                                             expectedDto.getCourseId() +
                                                                             " not found"));
    }

    @Test
    void updateMembershipTest(WebGraphQlTester tester) {
        final UUID courseId = UUID.randomUUID();

        // create admin user object
        final LoggedInUser adminUser = userWithMembershipInCourseWithId(courseId,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR);
        // save course memberships of admin to repository
        TestUtils.saveCourseMembershipsOfUserToRepository(courseMembershipRepository, adminUser);

        // add admin user data to header
        tester = addCurrentUserHeader(tester, adminUser);

        //init input data
        final UUID userId = UUID.randomUUID();

        final CourseMembershipEntity entity = CourseMembershipEntity.builder()
                .userId(userId)
                .courseId(courseId)
                .role(UserRoleInCourse.STUDENT)
                .build();
        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(userId)
                .setCourseId(courseId)
                .setRole(UserRoleInCourse.STUDENT)
                .build();

        //create entity in DB
        courseMembershipRepository.save(entity);

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
                .path("updateMembership")
                .entity(CourseMembership.class)
                .isEqualTo(expectedDto);
    }

    @Test
    void deleteNotExistingMembershipTest(WebGraphQlTester tester) {
        final UUID courseId = UUID.randomUUID();

        // create admin user object
        final LoggedInUser adminUser = userWithMembershipInCourseWithId(courseId,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR);
        // save course memberships of admin to repository
        TestUtils.saveCourseMembershipsOfUserToRepository(courseMembershipRepository, adminUser);

        // add admin user data to header
        tester = addCurrentUserHeader(tester, adminUser);

        //init input data
        final UUID userId = UUID.randomUUID();

        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(userId)
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
                .expect(responseError -> responseError.getMessage().contains("Membership with user id " +
                                                                             expectedDto.getUserId() +
                                                                             " and course id " +
                                                                             expectedDto.getCourseId() +
                                                                             " not found"));
    }

    @Test
    void deleteMembershipTest(WebGraphQlTester tester) {
        final UUID courseId = UUID.randomUUID();

        // create admin user object
        final LoggedInUser adminUser = userWithMembershipInCourseWithId(courseId,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR);
        // save course memberships of admin to repository
        TestUtils.saveCourseMembershipsOfUserToRepository(courseMembershipRepository, adminUser);

        // add admin user data to header
        tester = addCurrentUserHeader(tester, adminUser);

        //init input data
        final UUID userId = UUID.randomUUID();

        final CourseMembershipEntity entity = CourseMembershipEntity.builder()
                .userId(userId)
                .courseId(courseId)
                .role(UserRoleInCourse.STUDENT).build();
        final CourseMembership expectedDto = CourseMembership.builder()
                .setUserId(userId)
                .setCourseId(courseId)
                .setRole(UserRoleInCourse.STUDENT)
                .build();

        //create entity in DB
        courseMembershipRepository.save(entity);

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
                .path("deleteMembership")
                .entity(CourseMembership.class)
                .isEqualTo(expectedDto);
    }

}
