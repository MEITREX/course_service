package de.unistuttgart.iste.meitrex.course_service.persistence.entity;

import de.unistuttgart.iste.meitrex.common.persistence.IWithId;
import de.unistuttgart.iste.meitrex.generated.dto.YearDivision;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity(name = "Course")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseEntity implements IWithId<UUID> {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(nullable = false)
    private OffsetDateTime startDate;

    @Column(nullable = false)
    private OffsetDateTime endDate;

    @Column(nullable = false)
    private boolean published;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "courseId")
    @OrderBy("number ASC")
    private List<ChapterEntity> chapters;
    
    @Column()
    private Integer startYear;

    @Enumerated(EnumType.STRING)
    private YearDivision yearDivision;


}
