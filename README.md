# Supply Coordinator

## What it is

This application is a small server-authoritative maritime logistics simulation built with Java and Spring Boot. A scheduled backend simulation manages fictional vessel movement, telemetry, fuel depletion, and autonomous resupply assignments. Spring MVC exposes the shared fleet through REST, while Thymeleaf, Leaflet, OpenStreetMap, and SVG markers provide the browser visualization.

The in-memory fleet contains 15 NAVAL vessels traveling among six fictional ports and three SUPPLY vessels available for support. At 25% fuel, a NAVAL vessel requests help; the backend assigns the nearest available SUPPLY vessel using squared latitude/longitude distance. At rendezvous, fuel is restored, the SUPPLY vessel is released, and the NAVAL vessel resumes its route.

There are plans for expansion of responses:
 - low food levels,
 - sustained damage, 
 - distress beacon call

## Architecture

```
SimulationEngine
      ↓
VesselService
      ↓
shared Vessel state
      ↓
GET /api/vessels
      ↓
map.js
      ↓
Leaflet / OpenStreetMap
```

Java owns all simulation decisions. The browser polls `/api/vessels` every five seconds, reuses the existing SVG markers, and interpolates them toward the latest backend positions.

## Stack

- Java 25
- Spring Boot
- Embedded Tomcat
- Spring MVC
- Thymeleaf
- Leaflet
- OpenStreetMap
- Maven

## Live Demo
https://ships.michaeltylersmith.com/

This is a fictional software-integration demonstration, not a realistic maritime or military system. It intentionally uses simplified coordinate-space movement and resets whenever the application restarts.


# Purpose

This project was created to demonstrate practical Java and Spring Boot development skills for prospective employers. It showcases object-oriented design, dependency injection, scheduled backend processing, REST APIs, shared application state, JSON serialization, and frontend integration through a small server-authoritative maritime simulation. The goal was to build something beyond a basic tutorial project: a compact application that shows real programming capability, clear separation of responsibilities, and the ability to design, implement, and explain a complete Java-based system.

# Project Story Deep Dive

This project started as a way for me to relearn Java and get comfortable with Spring Boot again while preparing for Java developer roles. I did not want to make another basic CRUD app or copy a tutorial, so I chose something visual that would force me to use several parts of the Java ecosystem together.

The idea became a small fictional maritime logistics simulator. The application has naval ships, supply ships, ports, fuel levels, destinations, and basic resupply behavior. Ships move around an OpenStreetMap display using custom SVG icons, consume fuel while traveling, and can request help from nearby supply ships when fuel gets low. The maritime setting is intentionally simple and fictional; it just gives me an easy way to see whether the backend is behaving correctly.

The backend is built with Java and Spring Boot. Spring Boot starts the application with embedded Tomcat, Spring MVC handles HTTP requests, and Jackson serializes Java objects into JSON for the browser. The main fleet lives in a `VesselService`, which is managed by Spring and shared across the application.

One useful thing I had to relearn was the difference between a traditional Singleton pattern and Spring's singleton bean scope. At one point, manually creating a new `VesselService` inside another class would have resulted in an entirely separate fleet. Using constructor injection instead means Spring creates the service and passes the same managed instance to both the simulation engine and the controller.

That helped make Spring feel less like annotation magic and more like what it really is: a framework managing object creation, dependencies, and lifecycle.

The `SimulationEngine` contains most of the active behavior. It runs on a schedule using `@Scheduled` and updates vessel state independently of browser traffic. This is important because the simulation should continue even when nobody has the website open.

One of the more interesting problems during development was deciding where movement should actually live.

The first version sent movement information to JavaScript and let the browser calculate the ship's current position. This worked visually, but it created two sources of truth: Java thought it knew the vessel state, while JavaScript was also calculating movement independently.

That created awkward questions about refreshes, multiple browser sessions, and when a movement should count as finished. I briefly considered having JavaScript notify the backend when an animation completed, but that exposed the problem immediately. If nobody had the site open, then the browser could never report that a ship arrived.

The design was simplified so Java became the only authoritative source of simulation state.

The backend updates the real vessel positions. The frontend simply calls: setInterval(updateVessels, 5000);


and receives the latest server state from `/api/vessels`. JavaScript then interpolates the SVG markers smoothly from their current visual position toward the newest position supplied by Java.

That means browser interpolation is only for appearance. It does not control the simulation.

As a result, refreshing the page does not reset the vessels, closing the browser does not stop the simulation, and multiple users can view the same backend fleet.

The frontend is intentionally basic. It uses Leaflet with OpenStreetMap tiles and custom SVG markers. Each vessel has a stable ID, and the JavaScript keeps a marker associated with that ID instead of recreating everything every five seconds.

The popup for each ship displays information such as its name, type, fuel, food, and current status. JavaScript does not decide when fuel becomes low or which supply ship should respond. It only displays what the backend reports.

The final simulation contains multiple naval and supply vessels moving between several port locations. Each vessel owns state such as position, destination, speed, fuel, food, and operational mode.

As naval ships travel, their fuel decreases. Once fuel crosses a threshold, the backend can mark the ship as needing support. The simulation then checks the available supply vessels and chooses the nearest one using a simple coordinate-distance comparison.

The supply ship travels toward the requesting vessel, performs a simplified rendezvous, restores fuel, becomes available again, and the naval ship resumes normal travel.

I intentionally avoided adding technologies the project did not need. There is no database because persistence is not important here. There are no WebSockets because five-second polling is enough. There is no Kafka, Redis, microservice architecture, routing engine, or realistic maritime navigation.

The application state simply lives in memory inside the running Spring process. If the application restarts, the simulation can restart too.

The project also exposed some basic concurrency concerns. The scheduled simulation can modify vessel state while Tomcat handles a request for `/api/vessels`. I added lightweight synchronization and avoided directly exposing the internal fleet collection.

I would not call that production-grade concurrency, but it gave me a better understanding of where shared mutable state becomes important in Java applications.

Another useful realization is that Spring's singleton scope only applies inside one running application. If two copies of the application were deployed, each would have its own fleet. A larger production system would need shared external state or a single authoritative simulation owner.

Overall, this became a Java refresher project that grew into a small full-stack simulation.

It let me practice:

* Java classes and object ownership
* Spring Boot structure
* dependency injection
* Spring-managed beans
* scheduled backend work
* REST endpoints
* JSON serialization
* embedded Tomcat
* Maven
* shared state
* basic synchronization
* JavaScript consuming a Java API
* Leaflet and OpenStreetMap integration

The ships are really just a visual way to expose those concepts.

The main reason I built the project was to get comfortable writing Java again and to have something concrete to show while applying for Java roles. It is small enough that I can explain every major part, but complex enough that I had to make real design decisions and correct a few bad ideas along the way instead of simply following a tutorial.

