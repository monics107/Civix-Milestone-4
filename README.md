# Civix

Civix is a civic engagement platform that connects citizens, government officials, and administrators. Citizens can create petitions, sign public petitions, participate in polls, submit community reports, and track official responses. Officials can review local civic activity and update petition progress. Super administrators manage users, officials, departments, and platform configuration.

## Project Structure

```text
Civix-Milestone-4/
|-- civic-frontend/    # Angular 18 single-page application
|-- civic-backend/     # Spring Boot REST API
|-- LICENSE            # MIT License
`-- README.md
```

## Main Features

- JWT-based authentication and role-based access control.
- Citizen registration, login, profile settings, petitions, polls, and reports.
- Petition creation, public signing, status tracking, reviews, official decisions, and progress updates.
- Poll creation, active/closed lifecycle, voting, and result analytics.
- Community report creation and status tracking.
- Official dashboard filtered by official location and department.
- Super admin dashboard for officials, citizens, departments, and categories.
- Citizen and official account activation/deactivation.
- Notifications for relevant petition and poll activity.
- MySQL persistence through Spring Data JPA.

## Technology Stack

### Frontend

- Angular 18
- TypeScript
- Angular Material
- RxJS
- Chart.js

### Backend

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security
- JWT authentication
- Spring Data JPA / Hibernate
- MySQL
- Maven

## Requirements

Install the following before running the project:

- Node.js and npm
- Java JDK 25
- MySQL Server
- Git

Confirm the tools are available:

```bash
node --version
npm --version
java --version
```

## Database Setup

The backend currently uses these values from `civic-backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/civic_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=root
```

Create or configure a MySQL user that matches these values. The database `civic_db` is created automatically when the MySQL user has permission to create databases. To use different credentials, update `application.properties` before starting the backend.

For development, Hibernate is configured with `spring.jpa.hibernate.ddl-auto=update`, so tables are created or updated automatically. Do not use this setting for production migrations without reviewing the database change process.

## Install Dependencies

Open a terminal in the project root and install frontend dependencies:

```bash
cd civic-frontend
npm install
```

Backend dependencies are resolved by Maven from the backend directory.

## Run the Application

Use two terminals.

### Terminal 1: Backend

Windows PowerShell:

```powershell
cd civic-backend
./mvnw.cmd spring-boot:run
```

If the Maven wrapper is unavailable, use an installed Maven command:

```powershell
mvn spring-boot:run
```

The API runs at:

```text
http://localhost:8080/api
```

### Terminal 2: Frontend

```bash
cd civic-frontend
npm start or ng serve
```

Open the application at [http://localhost:4200](http://localhost:4200).

The frontend API URL is configured in `civic-frontend/src/environments/environment.ts`:

```typescript
apiUrl: 'http://localhost:8080/api'
```

When deploying to another environment, update this URL or use an environment-specific Angular configuration.

## Application Workflow

### 1. Registration and login

1. A user registers as a citizen or official.
2. The frontend sends the form data to the backend authentication endpoints.
3. The backend validates the request, hashes the password, stores the user, and returns a user response.
4. On login, the backend returns a JWT.
5. The frontend stores the authenticated session and sends the JWT in the `Authorization` header for protected requests.
6. Route guards show the correct dashboard for the authenticated role.

### 2. Citizen petition workflow

1. A citizen opens Create Petition.
2. The form validates the title, description, category, location, department, and signature goal.
3. The backend creates the petition with an active status.
4. The citizen is redirected to the new petition details page.
5. Other eligible citizens can sign the public petition.
6. Officials can review petitions connected to their location or department.
7. Officials can approve, reject, update progress, add work details, and close a petition.
8. Citizens see status, signatures, official responses, reviews, and timeline activity on the details page.

### 3. Poll workflow

1. An authorized citizen or official creates a poll with options, target location, department, and closing date.
2. New polls are created as `ACTIVE` when the closing date is valid.
3. Eligible users vote once while the poll is active.
4. The backend counts votes and prevents duplicate voting.
5. After the closing time, the poll becomes `CLOSED`.
6. The results page displays totals and percentages.

### 4. Community report workflow

1. A citizen submits a report with category, location, description, and priority.
2. The backend stores it with a pending status.
3. Officials review reports relevant to their area.
4. The report status can move through the supported workflow until resolution.

### 5. Super admin workflow

1. The super admin signs in through the normal login page.
2. The admin dashboard loads platform statistics and user directories.
3. Pending officials can be approved, rejected, assigned a department, and assigned a designation.
4. Officials and citizens can be activated or deactivated.
5. Departments can be added or deactivated.
6. Categories can be maintained from the admin interface.

## Default Development Admin

The development data initializer creates the following account when it does not already exist:

```text
Email:    superadmin@civix.com
Password: Civix@Admin123
```

Change or remove development credentials before sharing or deploying the application.

## Testing and Build

Build the frontend:

```bash
cd civic-frontend
npm run build
```

Run frontend tests:

```bash
npm test
```

Run backend tests:

```powershell
cd civic-backend
./mvnw.cmd test
```

## Troubleshooting

### Cannot connect to MySQL

- Confirm MySQL Server is running.
- Verify the username and password in `application.properties`.
- Confirm port `3306` is available.
- Ensure the configured user can create or access `civic_db`.

### Frontend shows API or CORS errors

- Start the backend before using authenticated pages.
- Confirm the backend is listening on port `8080`.
- Confirm `environment.ts` points to `http://localhost:8080/api`.
- Log in again after restarting the backend if the JWT has expired.

### Maven wrapper does not start

- Confirm Java JDK 25 is installed and available in `PATH`.
- Try `mvn spring-boot:run` if Maven is installed globally.
- Run the command from inside `civic-backend`.

## Security Notes

- Do not commit production passwords, JWT secrets, or database credentials.
- Replace development admin credentials before deployment.
- Use environment variables or a secret manager for production configuration.
- Use database migrations for controlled production schema changes.
- Review all third-party dependency licenses before redistribution.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file.

Copyright (c) 2026 Civix Team.
