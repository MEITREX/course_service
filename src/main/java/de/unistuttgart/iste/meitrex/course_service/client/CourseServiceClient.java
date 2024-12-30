package de.unistuttgart.iste.meitrex.course_service.client;

import de.unistuttgart.iste.meitrex.course_service.exception.CourseServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.CourseMembership;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.FieldAccessException;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.UUID;

/*
Client allowing to query course memberships.
 */
public class CourseServiceClient {

    private static final long RETRY_COUNT = 3;
    private final GraphQlClient graphQlClient;

    public CourseServiceClient(GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
    }

    public List<CourseMembership> queryMembershipsInCourse(final UUID courseId) throws CourseServiceConnectionException {
        if (courseId == null) {
            throw new CourseServiceConnectionException("Error fetching courseMemberships from CourseService: Course ID cannot be null");
        }

        final String query =
                """
                        query($courseId: UUID!) {
                            coursesByIds(ids: [$courseId]) {
                                memberships {
                                    userId
                                    courseId
                                    role
                                }
                            }
                        }
                        """;
        String queryName = "coursesByIds[0].memberships";

        List<CourseMembership> courseMembershipList = null;

        try {
            courseMembershipList = graphQlClient.document(query)
                    .variable("courseId", courseId)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<List<CourseMembership>> sink)
                            -> handleGraphQlResponse(result, sink, queryName))
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

    private void handleGraphQlResponse(final ClientGraphQlResponse result, final SynchronousSink<List<CourseMembership>> sink, final String queryName) {
        if (!result.isValid()) {
            sink.error(new CourseServiceConnectionException(result.getErrors().toString()));
            return;
        }

        List<CourseMembership> retrievedCourseMemberships;
        try {
            retrievedCourseMemberships = result.field(queryName).toEntityList(CourseMembership.class);
        } catch (FieldAccessException e) {
            sink.error(new CourseServiceConnectionException(e.toString()));
            return;
        }

        // retrievedCourseMemberships == null is always false, therefore no check
        if (retrievedCourseMemberships.isEmpty()) {
            sink.error(new CourseServiceConnectionException("Error fetching courseMemberships from CourseService: CourseMembership List is empty."));
            return;
        }

        sink.next(retrievedCourseMemberships);
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
