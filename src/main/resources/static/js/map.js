const map = L.map("map").setView(
            [24.2, -72.2],
            6
        );

L.tileLayer(
    "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
    {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }
).addTo(map);

const navalShipIcon = L.icon({
    iconUrl:"/images/naval-ship.svg",
    iconSize: [40,40],
    iconAnchor: [20,20]
})
const supplyShipIcon = L.icon({
    iconUrl:"/images/supply-ship.svg",
    iconSize: [40,40],
    iconAnchor: [20,20]
})

                
//Initial creation of sprites
async function loadVessels() {
    let response = await fetch('/api/vessels');
    if (!response.ok){
        throw new Error(
            `Failed to load vessels: ${response.status}`
        );
    }
    let vessels = await response.json();
    
    for (const vessel of vessels) {
        console.log(vessel);
        createVesselMarker(vessel);
    }
}

function createVesselMarker(vessel) {
    const state = vessel.state;
    state.info = `
        <b>Name:</b> ${vessel.name}<br/>
        <b>Fuel Level:</b> ${state.fuel} % <br/>
        <b>Food Level:</b> ${state.food} % <br/>
    `
    const marker = L.marker([state.position.latitude, state.position.longitude], {icon: vessel.type == "SUPPLY" ? supplyShipIcon : navalShipIcon}).addTo(map);
    marker.bindPopup(state.info);

    if (hasMovement(state)) {
        console.log("Animating marker: ", marker);
        animateMovement(marker, state)
    }
}

function hasMovement(state) {
    return state.destination != null
        && state.movementStartTime != null
        && state.movementDuration != null
}

function interpolate(start, destination, progress) {
    console.log("interpolate, start: ", start)
    const latitude =
        start.latitude +
        (destination.latitude - start.latitude) * progress;

    const longitude =
        start.longitude +
        (destination.longitude - start.longitude) * progress;

    return [latitude, longitude];
}

function calculateMovementProgress(state) {
    const elapsed = Date.now() - state.movementStartTime;

    const progress = elapsed / state.movementDuration;

    return Math.min(Math.max(progress, 0),1);
}

function animateMovement(marker, state) {
    const start = {
        latitude: state.position.latitude,
        longitude: state.position.longitude
    }
    const destination = {
        latitude: state.destination.latitude,
        longitude: state.destination.longitude
    }

    function frame() {
        const progress = calculateMovementProgress(state);
        
        const currentPosition = interpolate(start, destination, progress);
        marker.setLatLng(currentPosition);
        if (progress < 1) {
            requestAnimationFrame(frame);
        }
    }

    requestAnimationFrame(frame);
}

loadVessels();