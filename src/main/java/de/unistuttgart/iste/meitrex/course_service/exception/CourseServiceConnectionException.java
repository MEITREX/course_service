package de.unistuttgart.iste.meitrex.course_service.exception;

import org.springframework.graphql.ResponseError;

import java.util.List;

/**
 * Exception thrown when the connection to the Course service fails.
 */
public class CourseServiceConnectionException extends Exception {

  private final String message;

  public CourseServiceConnectionException(final String message) {
    super(message);
    this.message = message;
  }

  public CourseServiceConnectionException(final String message, final List<ResponseError> errors) {
    super(message);
    this.message = responseErrorsToString(message, errors);
  }

  private String responseErrorsToString(final String message, final List<ResponseError> errors) {
    final StringBuilder stringBuilder = new StringBuilder(message);
    stringBuilder.append("GraphQl Response Errors: \n");
    for (final ResponseError error : errors) {
      stringBuilder.append(error.getMessage())
              .append(" at path ").append(error.getPath())
              .append("\n");
    }
    return stringBuilder.toString();
  }

  @Override
  public String getMessage() {
    return this.message;
  }
}
