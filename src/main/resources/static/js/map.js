// Map setup
const map = L.map("map").setView([24.2, -72.2], 6);

L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
    maxZoom: 19,
    attribution: "&copy; OpenStreetMap contributors"
}).addTo(map);

const navalShipIcon = L.divIcon({
    html: '<img src="/images/naval-ship.svg" class="vessel-marker vessel-marker--naval" alt="">',
    iconSize: [40, 40],
    iconAnchor: [20, 20],
    className: "vessel-marker-wrapper"
});

const supplyShipIcon = L.divIcon({
    html: '<img src="/images/supply-ship.svg" class="vessel-marker vessel-marker--supply" alt="">',
    iconSize: [40, 40],
    iconAnchor: [20, 20],
    className: "vessel-marker-wrapper"
});

// Application Variables
const ANIMATION_DURATION_MS = 4500;
const markerMap = {};
let hasFitInitialFleet = false;
let isUpdateInFlight = false;



function createPopupContent(vessel, vesselLookup) {
    const state = vessel.state;

    return `
        <b>Name:</b> ${vessel.name}<br/>
        <b>Type:</b> ${vessel.type}<br/>
        <b>Fuel:</b> ${state.fuel.toFixed(0)}%<br/>
        <b>Food:</b> ${state.food}%<br/>
        <b>Status:</b> ${getVesselStatus(vessel, vesselLookup)}
    `;
}

function applyMarkerState(vesselMarker, vessel) {
    const markerElement = getMarkerVisual(vesselMarker);
    const mode = vessel.state.mode;
    const isRequesting = vessel.type === "NAVAL" && (mode === "RESUPPLY_REQUESTED" || mode === "WAITING_FOR_SUPPLY" || mode === "RESUPPLYING");
    const isResponding = vessel.type === "SUPPLY" && (mode === "RESPONDING" || mode === "RESUPPLYING");

    if (markerElement) {
        markerElement.classList.toggle("vessel-marker--requesting", isRequesting);
        markerElement.classList.toggle("vessel-marker--responding", isResponding);
    }

    vesselMarker.setZIndexOffset(isRequesting ? 1000 : isResponding ? 900 : 0);
}

function createVesselMarker(vessel, vesselLookup) {
    const position = vessel.state.position;
    const vesselMarker = L.marker(
        [position.latitude, position.longitude],
        {icon: getVesselIcon(vessel)}
    )
        .addTo(map)
        .bindPopup(createPopupContent(vessel, vesselLookup));

    markerMap[vessel.id] = vesselMarker;
    applyMarkerState(vesselMarker, vessel);
    updateMarkerHeading(vesselMarker, position, vessel.state.destination);
}

function interpolate(start, target, progress) {
    const latitude = start.latitude + (target.latitude - start.latitude) * progress;
    const longitude = start.longitude + (target.longitude - start.longitude) * progress;

    return [latitude, longitude];
}

function calculateHeading(start, target) {
    if (!isValidCoordinate(start) || !isValidCoordinate(target)) {
        return null;
    }

    const latitudeDelta = target.latitude - start.latitude;
    const longitudeDelta = target.longitude - start.longitude;

    if (latitudeDelta === 0 && longitudeDelta === 0) {
        return null;
    }

    return Math.atan2(longitudeDelta, latitudeDelta) * 180 / Math.PI;
}

function updateMarkerHeading(vesselMarker, start, target) {
    const heading = calculateHeading(start, target);

    if (heading == null) {
        return;
    }

    const previousHeading = vesselMarker._headingDegrees;
    const displayHeading = previousHeading == null
        ? heading
        : heading + Math.round((previousHeading - heading) / 360) * 360;
    const markerElement = getMarkerVisual(vesselMarker);

    vesselMarker._headingDegrees = displayHeading;

    if (markerElement) {
        markerElement.style.transform = `rotate(${displayHeading}deg)`;
    }
}

function animateMovement(vesselMarker, targetPosition) {
    if (vesselMarker._animationFrameId != null) {
        cancelAnimationFrame(vesselMarker._animationFrameId);
        vesselMarker._animationFrameId = null;
    }

    const markerPosition = vesselMarker.getLatLng();
    const start = {
        latitude: markerPosition.lat,
        longitude: markerPosition.lng
    };

    updateMarkerHeading(vesselMarker, start, targetPosition);

    if (start.latitude === targetPosition.latitude
            && start.longitude === targetPosition.longitude) {
        return;
    }

    const startTime = performance.now();

    function frame(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / ANIMATION_DURATION_MS, 1);

        vesselMarker.setLatLng(interpolate(start, targetPosition, progress));

        if (progress < 1) {
            vesselMarker._animationFrameId = requestAnimationFrame(frame);
        } else {
            vesselMarker._animationFrameId = null;
        }
    }

    vesselMarker._animationFrameId = requestAnimationFrame(frame);
}

function isValidCoordinate(coordinate) {
    return coordinate && Number.isFinite(coordinate.latitude) && Number.isFinite(coordinate.longitude);
}

function hasValidPosition(vessel) {
    return vessel && vessel.id != null && vessel.state && isValidCoordinate(vessel.state.position);
}

function updateVessel(vessel, vesselLookup) {
    if (!hasValidPosition(vessel)) {
        return;
    }

    const existingMarker = markerMap[vessel.id];

    if (!existingMarker) {
        createVesselMarker(vessel, vesselLookup);
        return;
    }

    existingMarker.setPopupContent(createPopupContent(vessel, vesselLookup));
    applyMarkerState(existingMarker, vessel);
    animateMovement(existingMarker, vessel.state.position);
}

function fitInitialFleet(vessels) {
    if (hasFitInitialFleet) {
        return;
    }

    const fleetPositions = [];

    for (const vessel of vessels) {
        if (!vessel || !vessel.state) {
            continue;
        }

        for (const coordinate of [vessel.state.position, vessel.state.destination]) {
            if (isValidCoordinate(coordinate)) {
                fleetPositions.push([coordinate.latitude, coordinate.longitude]);
            }
        }
    }

    if (fleetPositions.length === 0) {
        return;
    }

    map.fitBounds(fleetPositions, {
        padding: [40, 40],
        maxZoom: 6
    });
    hasFitInitialFleet = true;
}

async function updateVessels() {
    if (isUpdateInFlight) {
        return;
    }

    isUpdateInFlight = true;

    try {
        const response = await fetch("/api/vessels");

        if (!response.ok) {
            throw new Error(`Failed to load vessels: ${response.status}`);
        }

        const vessels = await response.json();
        const vesselLookup = new Map(
            vessels
                .filter((vessel) => vessel && vessel.id != null)
                .map((vessel) => [vessel.id, vessel])
        );

        for (const vessel of vessels) {
            updateVessel(vessel, vesselLookup);
        }

        fitInitialFleet(vessels);
    } finally {
        isUpdateInFlight = false;
    }
}


// Getters
function getMarkerVisual(vesselMarker) {
    const markerElement = vesselMarker.getElement();

    return markerElement
        ? markerElement.querySelector(".vessel-marker")
        : null;
}

function getVesselIcon(vessel) {
    return vessel.type === "SUPPLY" ? supplyShipIcon : navalShipIcon;
}

function getAssignedVessel(vessel, vesselLookup) {
    const assignedVesselId = vessel.state.assignedVesselId;

    return assignedVesselId == null
        ? null
        : vesselLookup.get(assignedVesselId) || null;
}

function getVesselStatus(vessel, vesselLookup) {
    const mode = vessel.state.mode;
    const assignedVessel = getAssignedVessel(vessel, vesselLookup);

    if (mode === "RESUPPLY_REQUESTED") {
        return "RESUPPLY REQUESTED";
    }

    if (mode === "WAITING_FOR_SUPPLY") {
        return assignedVessel
            ? `WAITING FOR ${assignedVessel.name}`
            : "WAITING FOR SUPPLY";
    }

    if (mode === "RESPONDING") {
        return assignedVessel
            ? `RESPONDING TO ${assignedVessel.name}`
            : "RESPONDING";
    }

    if (mode === "RESUPPLYING") {
        if (vessel.type === "SUPPLY") {
            return assignedVessel
                ? `RESUPPLYING ${assignedVessel.name}`
                : "RESUPPLY IN PROGRESS";
        }

        return assignedVessel
            ? `RESUPPLY IN PROGRESS WITH ${assignedVessel.name}`
            : "RESUPPLY IN PROGRESS";
    }

    if (mode === "MOVING") {
        return "IN TRANSIT";
    }

    return vessel.type === "SUPPLY" ? "AVAILABLE" : "IDLE";
}


// Launch Point
updateVessels();

setInterval(updateVessels, 5000);
