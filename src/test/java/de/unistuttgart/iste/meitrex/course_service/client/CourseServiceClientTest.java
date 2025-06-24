package de.unistuttgart.iste.meitrex.course_service.client;

import de.unistuttgart.iste.meitrex.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.meitrex.course_service.persistence.entity.ChapterEntity;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.ChapterRepository;
import de.unistuttgart.iste.meitrex.course_service.persistence.repository.CourseRepository;
import de.unistuttgart.iste.meitrex.course_service.test_utils.TestUtils;
import de.unistuttgart.iste.meitrex.generated.dto.Chapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * This class is used to test the ContentServiceClient.
 */
@GraphQlApiTest
class CourseServiceClientTest {

    private GraphQlClient graphQlClient;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() {
        final WebTestClient webTestClient = MockMvcWebTestClient.bindToApplicationContext(applicationContext)
                .configureClient().baseUrl("/graphql").build();

        graphQlClient = GraphQlClient.builder(new WebTestClientTransport(webTestClient)).build();
    }

    @Test
    void testQueryContentsOfChapter() throws Exception {
        final CourseServiceClient contentServiceClient = new CourseServiceClient(graphQlClient);


        final var course = courseRepository.save(TestUtils.dummyCourseBuilder().build());

        final ChapterEntity chapterEntity = chapterRepository.save(TestUtils.dummyChapterBuilder().courseId(course.getId()).build());;

        final List<Chapter> actualChapters = contentServiceClient.queryChapterByCourseId(course.getId());

        assertThat(actualChapters, hasSize(1));

        assertThat(actualChapters.getFirst().getCourse().getId(), is(course.getId()));

        ChapterEntity compareEntity = modelMapper.map(actualChapters.getFirst(), ChapterEntity.class);
        compareEntity.setCourseId(actualChapters.getFirst().getCourse().getId());

        assertThat(compareEntity, is(chapterEntity));
    }
}
