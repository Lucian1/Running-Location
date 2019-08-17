package demo.task;

import demo.model.*;
import demo.service.PositionService;
import demo.support.NavUtils;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocationSimulator implements Runnable {

    @Getter
    @Setter
    private long id;

    @Setter
    private PositionService positionSerivce;

    private AtomicBoolean cancel = new AtomicBoolean();

    private double speedInMps;

    private boolean shoudMove;
    private boolean exportPositionsToMessaging = true;
    private Integer reportInterval=500;

    @Getter
    @Setter
    private PositionInfo positionInfo = null;

    @Setter
    private List<Leg> legs;
    private RunnerStatus runnerStatus = RunnerStatus.NONE;
    private String runningId;

    @Setter
    private Point startPoint;
    private Date executionStartTime;

    private MedicalInfo medicalInfo;

    public LocationSimulator(GpsSimulatorRequest gpsSimulatorRequest){
        this.shoudMove = gpsSimulatorRequest.isMove();
        this.exportPositionsToMessaging = gpsSimulatorRequest.isExportPositionsToMessaging();
        this.setSpeed(gpsSimulatorRequest.getSpeed());
        this.reportInterval = gpsSimulatorRequest.getReportInterval();

        this.runningId = gpsSimulatorRequest.getRunningId();
        this.runnerStatus = gpsSimulatorRequest.getRunnerStatus();
        this.medicalInfo = gpsSimulatorRequest.getMedicalInfo();
    }


    @Override
    public void run() {
        try{
            executionStartTime = new Date();
            if (cancel.get()){
                destory();
                return;
            }

            while(!Thread.interrupted()){
                long startTime = new Date().getTime();

                if(positionInfo!=null){
                    if(shoudMove){
                        moveRunningLocation();
                        positionInfo.setSpeed(speedInMps);
                    }
                    else{
                        positionInfo.setSpeed(0.0);
                    }

                    positionInfo.setRunnerStatus(this.runnerStatus);

                    final MedicalInfo medicalInfoToUse;

                    switch (this.runnerStatus){
                        case SUPPLY_NOW:
                        case SUPPLY_SOON:
                        case STOP_NOW:
                            medicalInfoToUse = this.medicalInfo;
                            break;
                        default:
                            medicalInfoToUse = null;
                            break;
                    }

                    final CurrentPosition currentPosition = new CurrentPosition(this.positionInfo.getRunningId(),
                            new Point(this.positionInfo.getPosition().getLatitude(), this.positionInfo.getPosition().getLongitude()),
                            this.positionInfo.getRunnerStatus(),
                            this.positionInfo.getSpeed(),
                            this.positionInfo.getLeg().getHeading(),
                            medicalInfoToUse
                            );

                    // send current position to distribution service via REST API
                    // @TODO implement positionInfoService
                    positionSerivce.processPositionInfo(id, currentPosition, this.exportPositionsToMessaging);

                }

                // wait until next position report
                sleep(startTime);
            }
        }
        catch(InterruptedException ie){
            destory();
            return;
        }
        destory();
    }

    void destory(){
        positionInfo = null;
    }

    private void sleep(long startTime) throws InterruptedException{
        long endTime = new Date().getTime();
        long elapsedTime = endTime - startTime;
        long sleepTime = reportInterval - elapsedTime > 0 ? reportInterval - elapsedTime : 0 ;
        Thread.sleep(sleepTime);
    }

    // Set new position of running location based on current position and running speed
    private void moveRunningLocation(){
        double distance  = speedInMps * reportInterval / 1000.0;
        double distanceFromStart = positionInfo.getDistanceFromStart() + distance;
        double excess = 0.0;

        for (int i=positionInfo.getLeg().getId(); i<legs.size(); i++){
            Leg currentLeg = legs.get(i);
            excess = distanceFromStart > currentLeg.getLength() ? distanceFromStart - currentLeg.getLength() : 0.0;

            if(Double.doubleToRawLongBits(excess) == 0){
                // this means new position falls within current leg
                positionInfo.setDistanceFromStart(distanceFromStart);
                positionInfo.setLeg(currentLeg);
                //Use the new position calcuation method in NavUtils
                Point newPosition = NavUtils.getPosition(currentLeg.getStartPosition(), distanceFromStart, currentLeg.getHeading());
                positionInfo.setPosition(newPosition);
                return;
            }
            distanceFromStart = excess;
        }

        setStartPosition();
    }

    // Position running location at start of path
    public void setStartPosition(){
        positionInfo = new PositionInfo();
        positionInfo.setRunningId(this.runningId);
        Leg leg = legs.get(0);
        positionInfo.setLeg(leg);
        positionInfo.setPosition(leg.getStartPosition());
        positionInfo.setDistanceFromStart(0.0);
    }

    public void setSpeed(double speed){
        this.speedInMps = speed;
    }

    public double getSpeed(){
        return this.speedInMps;
    }

    public synchronized void cancel(){
        this.cancel.set(true);
    }
}
