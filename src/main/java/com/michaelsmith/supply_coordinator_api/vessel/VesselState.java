package com.michaelsmith.supply_coordinator_api.vessel;

public class VesselState {

    private static final int FULL_FUEL = 100;

    private int food;
    private double fuel;
    private Position position;
    private Position destinationPosition;
    private VesselMode mode;
    private final double speed;
    private Integer assignedVesselId;
    private int resupplyHoldTicksRemaining;

    public VesselState(int food, double fuel, Position position, Position destinationPosition, double speed, VesselMode mode) {
        if (!Double.isFinite(speed) || speed <= 0.0) {
            throw new IllegalArgumentException("speed must be positive");
        }
        this.food = food;
        this.fuel = fuel;
        this.position = position;
        this.destinationPosition = destinationPosition;
        this.speed = speed;
        this.mode = mode;
        this.assignedVesselId = null;
        this.resupplyHoldTicksRemaining = 0;
    }

    //Getters
    public synchronized int getFood() {
        return food;
    }

    public synchronized double getFuel() {
        return fuel;
    }

    public synchronized Position getPosition() {
        return position;
    }

    public synchronized Position getDestination() {
        return destinationPosition;
    }

    public synchronized VesselMode getMode() {
        return mode;
    }

    public synchronized double getSpeed() {
        return speed;
    }

    public synchronized Integer getAssignedVesselId() {
        return assignedVesselId;
    }

    //Setters
    public synchronized void setDestination(Position destination) {
        this.destinationPosition = destination;
        this.mode = destination == null ? VesselMode.IDLE : VesselMode.MOVING;
    }



    // Helper functions
    public synchronized void consumeFood(int amount) {
        this.food = Math.max(0, this.food - amount);
    }

    public synchronized void consumeFuel(double amount) {
        this.fuel = Math.max(0, this.fuel - amount);
    }

    public synchronized boolean isMoving() {
        return destinationPosition != null
                && (mode == VesselMode.MOVING || mode == VesselMode.RESPONDING);
    }

    public synchronized void requestResupply() {
        assignedVesselId = null;
        resupplyHoldTicksRemaining = 0;
        mode = VesselMode.RESUPPLY_REQUESTED;
    }

    public synchronized void waitForSupply(int supplyVesselId) {
        assignedVesselId = supplyVesselId;
        resupplyHoldTicksRemaining = 0;
        mode = VesselMode.WAITING_FOR_SUPPLY;
    }

    public synchronized void respondTo(int navalVesselId, Position rendezvousPosition) {
        assignedVesselId = navalVesselId;
        destinationPosition = rendezvousPosition;
        resupplyHoldTicksRemaining = 0;
        mode = VesselMode.RESPONDING;
    }

    public synchronized void beginReceivingResupply() {
        fuel = FULL_FUEL;
        resupplyHoldTicksRemaining = 0;
        mode = VesselMode.RESUPPLYING;
    }

    public synchronized void beginResupplyHold(int holdTicks) {
        if (holdTicks <= 0) {
            throw new IllegalArgumentException("holdTicks must be positive");
        }

        resupplyHoldTicksRemaining = holdTicks;
        mode = VesselMode.RESUPPLYING;
    }

    public synchronized boolean advanceResupplyHold() {
        if (mode != VesselMode.RESUPPLYING) {
            return false;
        }

        if (resupplyHoldTicksRemaining > 0) {
            resupplyHoldTicksRemaining--;
        }

        return resupplyHoldTicksRemaining == 0;
    }

    public synchronized void finishResupply() {
        assignedVesselId = null;
        resupplyHoldTicksRemaining = 0;
        mode = destinationPosition == null ? VesselMode.IDLE : VesselMode.MOVING;
    }

    public synchronized void releaseSupport() {
        assignedVesselId = null;
        destinationPosition = null;
        resupplyHoldTicksRemaining = 0;
        mode = VesselMode.IDLE;
    }

    public synchronized boolean advanceTowardDestination() {
        if (destinationPosition == null) {
            return false;
        }

        double latitudeDelta = destinationPosition.latitude() - position.latitude();
        double longitudeDelta = destinationPosition.longitude() - position.longitude();
        double distance = Math.hypot(latitudeDelta, longitudeDelta);

        if (distance <= speed) {
            position = destinationPosition;
            destinationPosition = null;
            return true;
        }

        double stepRatio = speed / distance;
        position = new Position(position.latitude() + latitudeDelta * stepRatio, position.longitude() + longitudeDelta * stepRatio);
        return false;
    }
}
