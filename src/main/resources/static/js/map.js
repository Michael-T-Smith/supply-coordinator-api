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

loadVessels();



async function loadVessels() {
    let response = await fetch('/api/vessels');
    let vessels = await response.json();

    

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
    
    vessels.forEach(element => {
       console.log(element); 
        element.vesselState.info = `
            <b>Name:</b> ${element.vesselState.name}<br/>
            <b>Fuel Level:</b> ${element.vesselState.fuel} % <br/>
            <b>Food Level:</b> ${element.vesselState.food} % <br/>
            ${element.vesselState.type == "NAVAL" ? `<b>Priority:</b> ${element.vesselState.priorityLevel || 'Low'}` : ''}
        `
        L.marker([element.vesselState.latitude, element.vesselState.longitude], {icon: element.vesselState.type == "SUPPLY" ? supplyShipIcon : navalShipIcon}).addTo(map).bindPopup(element.vesselState.info)
    });
    
}