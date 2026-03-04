# Adventure Engine API

A production-ready Spring Boot application that powers interactive
JSON-based adventure books via REST APIs.

The engine supports session-based gameplay, health mechanics, strict
validation rules, persistent storage, Dockerized deployment, and
Infrastructure-as-Code provisioning with Terraform.

------------------------------------------------------------------------

## Table of Contents

-   Overview
-   Architecture
-   Prerequisites
-   Local Development
-   Docker Deployment
-   Gameplay API Flow
-   Validation Rules
-   Health & Game Mechanics
-   Infrastructure (Terraform)
-   Monitoring & Database Access
-   Project Structure
-   Tech Stack
-   Production Notes

------------------------------------------------------------------------

## Overview

Adventure Engine is a RESTful backend service that:

-   Loads adventure books from JSON files\
-   Manages player sessions\
-   Tracks player health (HP)\
-   Enforces strict story validation rules\
-   Persists player progress using H2 database\
-   Supports containerized deployment\
-   Can be provisioned on AWS via Terraform

Each player session is isolated and stateful.

------------------------------------------------------------------------

## Architecture

### Core Components

-   Book Loader (JSON → Domain Model)
-   Validation Engine
-   Session Manager
-   Health/Consequence Processor
-   Persistence Layer (H2 file-based DB)
-   REST API Layer (Spring Boot)

### Storage

-   File-based H2 database\
-   Persistent volume when running via Docker

------------------------------------------------------------------------

## Prerequisites

-   Java 21\
-   Maven 3.9+\
-   Docker (optional)\
-   Terraform (optional, for AWS deployment)

------------------------------------------------------------------------

## Local Development

### 1. Add Adventure Books

Place your JSON files in:

    src/main/resources/books/

### 2. Build the Project

    mvn clean install

### 3. Run the Application

    mvn spring-boot:run

Application starts at:

    http://localhost:8080

Swagger UI:

    http://localhost:8080/swagger-ui/index.html

H2 Console: :

    http://localhost:8080/h2-console

    JDBC URL: jdbc:h2:file:/app/data/adventure_db

    User/Pass: sa / password

------------------------------------------------------------------------

## Docker Deployment

The application is fully containerized with persistent database storage.

### Build and Run

    docker-compose up --build

### Persistent Storage

Database files are stored in:

    ./data

This ensures player progress survives container restarts.

------------------------------------------------------------------------

## Gameplay API Flow

### 1. Discover Available Books

    GET /api/books

Copy the id (UUID) of the book you want to play.

### 2. Start an Adventure

    POST /api/play/{bookId}/start

Response:

{ "sessionId": "UUID", "hp": 10 }

Players start with 10 HP.

### 3. Make a Choice

    POST /api/play/session/{sessionId}/choose

Request Body:

    20

Where 20 is the gotoId of the next section.

### 4. Check Session Status

    GET /api/play/session/{sessionId}

Returns:

-   Current HP\
-   Current section\
-   Event log\
-   Game over status

------------------------------------------------------------------------

## Validation Rules

A book is rejected if it:

-   Does not contain exactly one BEGIN section\
-   Does not contain at least one END section\
-   Contains a gotoId referencing a non-existent section\
-   Has a non-ending section with zero available choices

Validation runs automatically at load time.

------------------------------------------------------------------------

## Health & Game Mechanics

When a choice results in LOSE_HEALTH:

-   HP is reduced automatically\
-   Event is appended to session log\
-   gameOver is set to true if HP reaches 0

Health is enforced at the engine level --- no manual handling required
by clients.

------------------------------------------------------------------------

## Infrastructure (Terraform)

Provision AWS infrastructure using:

    terraform init
    terraform plan
    terraform apply

Terraform provisions:

-   Security-hardened EC2 instance\
-   Docker pre-installed\
-   Port 8080 exposed\
-   Ready-to-run container environment

------------------------------------------------------------------------

## Monitoring & Database Access

### H2 Console

    http://localhost:8080/h2-console

### JDBC Configuration

    JDBC URL: jdbc:h2:file:/app/data/adventure_db
    User: sa
    Password: password

------------------------------------------------------------------------

## Project Structure

    src/
    └── main/
        ├── java/                # Application source code
        └── resources/
            └── books/           # Adventure JSON files
    docker-compose.yml
    terraform/
    data/                         # Persistent DB volume (Docker)

------------------------------------------------------------------------

## Tech Stack

-   Java 21\
-   Spring Boot\
-   Maven\
-   H2 Database (file-based)\
-   Docker & Docker Compose\
-   Terraform\
-   AWS EC2

------------------------------------------------------------------------

## Production Notes

-   Designed for stateless API + persistent DB\
-   Docker-ready for container orchestration\
-   Infrastructure-as-Code enabled\
-   Validation layer prevents corrupted story definitions\
-   Session isolation ensures safe concurrent play
