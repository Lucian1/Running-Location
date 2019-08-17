package demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

//Data transfer object
//Frontend <-> Backend
// repository <-> service <-> rest
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentPositionDto {
    private String runningId;
    private Point location;
    private RunnerStatus runnerStatus = RunnerStatus.NONE;
    private Double speed;
    private Double heading;
}
