# Habit Tracker - Spring Boot Backend

Requirements: Java 17+, Maven 3.8+

    mvn spring-boot:run

API runs at http://localhost:8080 (H2 console: /h2-console, JDBC URL jdbc:h2:mem:habitdb).
To use MySQL, edit src/main/resources/application.properties.

## Endpoints
| Method | URL                              | Description                          |
|--------|----------------------------------|--------------------------------------|
| GET    | /api/habits                      | List habits (with streak + today)    |
| GET    | /api/habits/{id}                 | Get one habit                        |
| POST   | /api/habits                      | Create {name, description, color}    |
| PUT    | /api/habits/{id}                 | Update                               |
| DELETE | /api/habits/{id}                 | Delete habit and its logs            |
| POST   | /api/habits/{id}/toggle?date=    | Toggle completion (default today)    |
| GET    | /api/habits/{id}/logs?from=&to=  | Completed dates (heatmap data)       |
| GET    | /api/habits/{id}/stats           | Current/longest streak, 30-day rate  |
