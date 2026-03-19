# Ticket System Project

A full-stack ticket booking platform built with Spring Boot (Java) and React.

## Project Structure
- `backend/`: Spring Boot application providing RESTful APIs.
- `frontend/`: React application providing the user interface.

## Prerequisites
- **Java 17+**
- **Maven 3.8+**
- **Node.js 18+** & **npm**
- **SQL Server** (SSMS 22 or equivalent)

## Database Setup
1. Open SQL Server Management Studio (SSMS) and create a database named `TicketSystem`:
   ```sql
   CREATE DATABASE TicketSystem;
   ```
2. Update database credentials (username/password) in `backend/src/main/resources/application.yml` to match your local SQL Server instance authentication.

## Backend Setup (Spring Boot)
1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Build the project and download dependencies:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
   *The backend will start on `http://localhost:8080`*

## Frontend Setup (React Native / Web)
1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
   *The frontend will start on your local IP or `http://localhost:5173`*

## Key Features
- **User Authentication**: Login, Registration, JWT Tokens
- **Event Discovery**: Browse upcoming events, filter by location and status
- **Ticket Booking**: Interactive seat map selection, Stripe simulated checkout
- **Admin Dashboard**: Manage Events, Venues, Users, and Check-in Tickets using QR codes
- **Order History**: View past orders and download PDF tickets
