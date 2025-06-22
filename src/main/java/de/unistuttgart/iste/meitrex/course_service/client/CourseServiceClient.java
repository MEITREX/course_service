package de.unistuttgart.iste.meitrex.course_service.client;

import de.unistuttgart.iste.meitrex.course_service.exception.CourseServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.Course;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.UUID;

/*
Client allowing to query course info.
 */
public class CourseServiceClient {

    private static final long RETRY_COUNT = 3;
    private final GraphQlClient graphQlClient;

    public CourseServiceClient(GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }

    public Course queryCourseById(final UUID courseId) throws CourseServiceConnectionException {
        if (courseId == null) {
            throw new CourseServiceConnectionException("Error fetching course from CourseService: Course ID cannot be null");
        }

        final String query =
                """
                query($courseId: UUID!) {
                    coursesByIds(ids: [$courseId]) {
                        id
                        title
                    }
                }
                """;

        final String queryName = "coursesByIds";
        List<Course> courseList = null;

        try {
            courseList = graphQlClient.document(query)
                    .variable("courseId", courseId)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<List<Course>> sink) ->
                            handleGraphQlResponse(result, sink, queryName, Course.class,
                                    "Entities(s) with id(s) %s not found".formatted(courseId)))
                    .retry(RETRY_COUNT)
                    .block();

        } catch (RuntimeException e) {
            unwrapCourseServiceConnectionException(e);
        }

        if (courseList == null || courseList.isEmpty()) {
            throw new CourseServiceConnectionException("Entities(s) with id(s) %s not found".formatted(courseId));
        }

        return courseList.getFirst();
    }


    public List<CourseMembership> queryMembershipsInCourse(final UUID courseId) throws CourseServiceConnectionException {
        if (courseId == null) {
            throw new CourseServiceConnectionException("Error fetching courseMemberships from CourseService: Course ID cannot be null");
        }

        final String query =
                """
                query($courseId: UUID!) {
                    _internal_noauth_courseMembershipsByCourseId(courseId: $courseId) {
                        userId
                        courseId
                        role
                    }
                }
                """;

        String queryName = "_internal_noauth_courseMembershipsByCourseId";

        List<CourseMembership> courseMembershipList = null;

        try {
            courseMembershipList = graphQlClient.document(query)
                    .variable("courseId", courseId)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<List<CourseMembership>> sink)
                            -> handleGraphQlResponse(result, sink, queryName, CourseMembership.class,
                            "Error fetching courseMemberships from CourseService: CourseMembership List is empty."))
                    .retry(RETRY_COUNT)
                    .block();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof JpaObjectRetrievalFailureException && e.getMessage().contains("Entities(s) with id(s) %s not found".formatted(courseId))) {
                throw new CourseServiceConnectionException(e.getMessage());
            } else {
                unwrapCourseServiceConnectionException(e);
            }
        }

        if (courseMembershipList == null) {
            throw new CourseServiceConnectionException("Error fetching courseMemberships from CourseService");
        }

        return courseMembershipList;
    }

    private <T> void handleGraphQlResponse(
            final ClientGraphQlResponse result,
            final SynchronousSink<List<T>> sink,
            final String queryName,
            final Class<T> clazz,
            final String emptyListErrorMessage) {

        if (!result.isValid()) {
            sink.error(new CourseServiceConnectionException(result.getErrors().toString()));
            return;
        }

        List<T> resultList;
        try {
            resultList = result.field(queryName).toEntityList(clazz);
        } catch (FieldAccessException e) {
            sink.error(new CourseServiceConnectionException(
                    "Error fetching from CourseService: Failed to access field '%s': %s".formatted(queryName, e)));
            return;
        }

        if (resultList.isEmpty()) {
            sink.error(new CourseServiceConnectionException(emptyListErrorMessage));
            return;
        }

        sink.next(resultList);
    }


    private static void unwrapCourseServiceConnectionException(final RuntimeException e) throws CourseServiceConnectionException {
        // block wraps exceptions in a RuntimeException, so we need to unwrap them
        if (e.getCause() instanceof final CourseServiceConnectionException courseServiceConnectionException) {
            throw courseServiceConnectionException;
        }
        // if the exception is not a ContentServiceConnectionException, we don't know how to handle it
        throw e;
    }

}
