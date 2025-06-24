package de.unistuttgart.iste.meitrex.course_service.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryDefinitions {
    public static final String CHAPTERS_BY_COURSEID = """
            query($courseId: UUID!) {
                _internal_noauth_chaptersByCourseId(courseId: $courseId) {
                    id
                    title
                    description
                    number
                    startDate
                    endDate
                    course {
                        id
                        }
                }
            }
            """;

    public static final String CHAPTERS_BY_COURSEID_QUERY_NAME = "_internal_noauth_chaptersByCourseId";
}
