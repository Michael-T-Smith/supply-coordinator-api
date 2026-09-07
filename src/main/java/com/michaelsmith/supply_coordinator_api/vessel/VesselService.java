package com.michaelsmith.supply_coordinator_api.vessel;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class VesselService {
    private static final Position PORT_A = new Position(30.395548, -81.404301);
    private static final Position PORT_B = new Position(36.974279, -76.282427);
    private static final Position PORT_C = new Position(38.666260, -9.255069);
    private static final Position PORT_D = new Position(49.882937, -6.356741);
    private static final Position PORT_E = new Position(4.936134,-52.340753);
    private static final Position PORT_F = new Position(23.628199,-15.994602);
    private static final Position PORT_G = new Position(64.037512,-22.724023);
    
    private static final Position OPEN_WATER_1 = new Position(43.94710, -43.14855);
    private static final Position OPEN_WATER_2 = new Position(20.50156, -46.65782);

    private static final List<Position> PORTS = List.of(
            PORT_A,
            PORT_B,
            PORT_C,
            PORT_D, 
            PORT_E,
            PORT_F,
            PORT_G,
            OPEN_WATER_1, 
            OPEN_WATER_2);

    private static final List<Position> SUPPLY_STARTS = List.of(
            new Position(20.400000, -46.300000),
            new Position(25.400000, -32.000000),
            new Position(31.800000, -60.800000));

    private static final int NAVAL_COUNT = 15;
    private static final int SUPPLY_COUNT = 3;
    private static final int INITIAL_FOOD = 100;
    private static final double NAVAL_BASE_SPEED = 0.10;
    private static final double NAVAL_SPEED_VARIATION = 0.01;
    private static final double SUPPLY_SPEED = 0.25;

    private final ArrayList<Vessel> vessels = new ArrayList<>();

    public VesselService() {
        createVessels();
    }

    private void createVessels() {
        for (int index = 0; index < NAVAL_COUNT; index++) {
            int fuel = Math.min(100, 30 + index * 5);
            double speed = NAVAL_BASE_SPEED + (index % 3) * NAVAL_SPEED_VARIATION;

            vessels.add(new Vessel(
                    index + 1,
                    VesselType.NAVAL,
                    String.format("NAVAL-%02d", index + 1),
                    INITIAL_FOOD,
                    fuel,
                    speed,
                    createNavalStartingPosition(index),
                    PORTS.get(index % PORTS.size())));
        }

        for (int index = 0; index < SUPPLY_COUNT; index++) {
            vessels.add(new Vessel(
                    NAVAL_COUNT + index + 1,
                    VesselType.SUPPLY,
                    String.format("SUPPLY-%02d", index + 1),
                    INITIAL_FOOD,
                    100,
                    SUPPLY_SPEED,
                    SUPPLY_STARTS.get(index),
                    null));
        }
    }

private Position createNavalStartingPosition(int index) {
    // Alternate between the two starting areas
    boolean firstArea = index % 2 == 0;

    double centerLat = firstArea ? 43.94710 : 28.500142;
    double centerLon = -43.911721;

    int groupIndex = index / 2;

    int columns = 5;
    int row = groupIndex / columns;
    int column = groupIndex % columns;
    double latitudeSpacing = 6.0;
    double longitudeSpacing = 6.0;

    return new Position(
        centerLat + row * latitudeSpacing,
        centerLon + column * longitudeSpacing
    );
}

    public synchronized ArrayList<Vessel> getVessels() {
        return new ArrayList<>(vessels);
    }

    public List<Position> getPorts() {
        return PORTS;
    }
}
