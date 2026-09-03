package com.michaelsmith.supply_coordinator_api.vessel;

public class VesselState {
    private int food;
    private int fuel;
    private Position position;
    private Position destinationPosition;
    private VesselMode mode;
    private Long movementStartedAt;
    private Long movementDuration;


    public VesselState(int food, int fuel, double latitude, double longitude, VesselMode mode) {
        this.food = food;
        this.fuel = fuel;
        this.position = new Position(latitude, longitude);
        this.mode = mode;
    }

    public int getFood() { return food; }
    public int getFuel() { return fuel; }
    public Position getPosition() { return position; }
    public Position getDestination() { return destinationPosition; }
    public VesselMode getMode() { return mode; }
    public Long getMovementStartTime() { return movementStartedAt; }
    public Long getMovementDuration() { return movementDuration; }
    public void consumeFood(int amount) { this.food = Math.max(0, this.food - amount); }
    public void consumeFuel(int amount) { this.fuel = Math.max(0, this.fuel - amount); }
    
    public boolean isMoving(){
        boolean moving = mode == VesselMode.MOVING;
        return moving;
    }

    public boolean hasMovementFinished(long currentTime) {
        if (!isMoving()){
            return false;
        }

        return currentTime >= movementStartedAt + movementDuration;
    }

    public void move(Position destination, long currentTime, long duration) {
        this.destinationPosition = destination;
        this.movementStartedAt = currentTime;
        this.movementDuration = duration;
    }

    public void finishMovement() {
        if (destinationPosition == null) {
            return;
        }

        this.position = destinationPosition;
        this.destinationPosition = null;
        this.movementStartedAt = null;
        this.movementDuration = null;
    }

}
