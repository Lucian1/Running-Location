package demo.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@JsonPropertyOrder({"numberOfGpsSimulatorRequests", "gpsSimulatorRequests"})
public class SimulatorInitLocations {

    private List<GpsSimulatorRequest> gpsSimulatorRequests = new ArrayList<>();

    public int getNumberOfGpsSimulatorRequests(){
        return gpsSimulatorRequests.size();
    }

    public void setGpsSimulatorRequests(List<GpsSimulatorRequest> gpsSimulatorRequests){
        Assert.notEmpty(gpsSimulatorRequests, "gpsSimulatorRequests cannot be empty");
        this.gpsSimulatorRequests = gpsSimulatorRequests;
    }
}
