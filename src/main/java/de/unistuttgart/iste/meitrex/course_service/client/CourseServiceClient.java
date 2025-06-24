package de.unistuttgart.iste.meitrex.course_service.client;


import de.unistuttgart.iste.meitrex.course_service.exception.CourseServiceConnectionException;
import de.unistuttgart.iste.meitrex.generated.dto.Chapter;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.graphql.client.GraphQlClient;


import java.util.List;
import java.util.UUID;

@Slf4j
public class CourseServiceClient {
    private static final long RETRY_COUNT = 3;


    private String courseServiceUrl;


    private final ModelMapper modelMapper;

    private final GraphQlClient graphQlClient;

    public CourseServiceClient(GraphQlClient graphQlClient) {
        modelMapper = new ModelMapper();

        this.graphQlClient = graphQlClient;
    }

    public List<Chapter> queryChapterByCourseId(final UUID courseId) throws CourseServiceConnectionException {
        log.info("queryContentByCourseId {}", courseId);
        try {
            return graphQlClient.document(QueryDefinitions.CHAPTERS_BY_COURSEID)
                    .variable("courseId", courseId)
                    .retrieveSync(QueryDefinitions.CHAPTERS_BY_COURSEID_QUERY_NAME)
                    .toEntityList(Chapter.class);
        } catch (final RuntimeException e) {
            unwrapCourseServiceConnectionException(e);
        }
        return List.of(); // unreachable
    }

    private static void unwrapCourseServiceConnectionException(final RuntimeException e) throws CourseServiceConnectionException {
        // block wraps exceptions in a RuntimeException, so we need to unwrap them
        if (e.getCause() instanceof final CourseServiceConnectionException courseServiceConnectionException) {
            throw courseServiceConnectionException;
        }
        // if the exception is not a CourseServiceConnectionException, we don't know how to handle it
        throw e;
    }

}
