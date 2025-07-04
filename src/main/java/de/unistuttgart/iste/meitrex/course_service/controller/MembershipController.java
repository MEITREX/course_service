package de.unistuttgart.iste.meitrex.course_service.controller;

import de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.meitrex.course_service.service.MembershipService;
import de.unistuttgart.iste.meitrex.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.meitrex.common.user_handling.LoggedInUser.UserRoleInCourse.ADMINISTRATOR;
import static de.unistuttgart.iste.meitrex.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @QueryMapping(name = "_internal_userIdsByCourseId")
    public List<UUID> userIdsByCourseId(@Argument UUID courseId) {
        return membershipService.getUserIdsOfCourse(courseId);
    }

    @QueryMapping(name = "_internal_noauth_courseMembershipsByUserId")
    public List<CourseMembership> courseMembershipsByUserIds(@Argument final UUID userId,
                                                             @Argument final Boolean availabilityFilter) {
        return membershipService.getAllMembershipByUserId(userId, availabilityFilter);
    }

    @QueryMapping(name = "_internal_noauth_courseMembershipsByCourseId")
    public List<CourseMembership> courseMembershipsByCourse(@Argument final UUID courseId) {

        return membershipService.getMembershipsOfCourse(courseId);
    }

    @MutationMapping
    public CourseMembership joinCourse(@Argument final UUID courseId,
                             @ContextValue final LoggedInUser currentUser) {
        return membershipService.createMembership(CourseMembershipInput.builder()
                .setCourseId(courseId)
                .setUserId(currentUser.getId())
                .setRole(UserRoleInCourse.STUDENT)
                .build());
    }

    @MutationMapping
    public CourseMembership leaveCourse(@Argument final UUID courseId,
                                        @ContextValue final LoggedInUser currentUser) {
        return membershipService.deleteMembership(currentUser.getId(), courseId);
    }

    @MutationMapping
    public CourseMembership createMembership(@Argument(name = "input") final CourseMembershipInput inputDto,
                                             @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, ADMINISTRATOR, inputDto.getCourseId());

        return membershipService.createMembership(inputDto);
    }

    @MutationMapping
    public CourseMembership updateMembership(@Argument(name = "input") final CourseMembershipInput inputDto,
                                             @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, ADMINISTRATOR, inputDto.getCourseId());

        return membershipService.updateMembershipRole(inputDto);
    }

    @MutationMapping
    public CourseMembership deleteMembership(@Argument(name = "input") final CourseMembershipInput inputDto,
                                             @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, ADMINISTRATOR, inputDto.getCourseId());

        return membershipService.deleteMembership(inputDto.getUserId(), inputDto.getCourseId());
    }

    @SchemaMapping(typeName = "Course", field = "memberships")
    public List<CourseMembership> memberships(final Course course,
                                              @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, ADMINISTRATOR, course.getId());

        return membershipService.getMembershipsOfCourse(course.getId());
    }
}
