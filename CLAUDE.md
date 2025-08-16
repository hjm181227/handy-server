# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot REST API server (handy-server) for a custom product marketplace platform. The application provides:
- User authentication and authorization with JWT
- Product management with categories, sizes, and shapes
- Shopping cart functionality
- Image upload/management via AWS S3
- Snap posts (social features)
- Comment and report systems
- Admin and user role management

**Tech Stack**: Java 21, Spring Boot 3.4.5, Spring Security, Spring Data JPA, MySQL, Flyway, AWS S3, Lombok

## Key Commands

### Build and Run
```bash
# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run locally (dev mode)
./scripts/run-local.sh
# or
./gradlew bootRun --args="--spring.profiles.active=local"

# Run in production mode
./scripts/run-prod.sh
```

### Development Scripts
- `./scripts/fix-flyway.sh` - Fix Flyway migration issues
- `./scripts/stop-prod.sh` - Stop production server
- Database management scripts are available in `/scripts/` directory

## Architecture Overview

### Package Structure
- `config/` - Spring configuration classes (Security, JPA, AWS, etc.)
- `controller/` - REST API endpoints organized by feature
- `dto/` - Request/Response objects for API communication
- `entity/` - JPA entities with relationships (User, Product, Cart, etc.)
- `repository/` - Spring Data JPA repositories
- `service/` - Business logic layer
- `security/` - JWT authentication components

### Key Entities and Relationships
- **User**: Has roles (USER/SELLER/ADMIN) with auth levels (100-300+)
- **Product**: Belongs to User (seller), has Categories, Sizes, Shapes
- **Cart/CartItem**: User-specific shopping cart with product variants
- **SnapPost**: Social feature for product showcases
- **Comment/Report**: Moderation and interaction features

### Authentication Flow
- JWT-based authentication with role-based authorization
- Most endpoints allow public access; /api/snap/** requires authentication
- Admin endpoints require ADMIN role
- Custom UserDetailsService with auth level mapping

## Database Management

### Flyway Migrations
Located in `src/main/resources/db/migration/`. Follow strict rules:
- **NEVER modify existing migration files** - creates checksum errors
- **NEVER delete migration files**
- Create new files for changes: `V{next_version}__{description}.sql`
- Use safe SQL patterns: `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE ADD COLUMN IF NOT EXISTS`
- Separate complex operations into individual migration files

### Migration Troubleshooting
- Use `./scripts/fix-flyway.sh` for automatic recovery
- Check FLYWAY_GUIDE.md for detailed troubleshooting steps
- Manual recovery via `flyway_schema_history` table updates if needed

## Development Guidelines

### Environment Configuration
- **Local**: Uses `application-local.yml` profile
- **Development**: Uses `application-dev.yml` profile  
- **Production**: Uses `application-prod.yml` profile
- Environment variables for sensitive data (AWS credentials, DB passwords)

### Testing
- Test configuration uses H2 in-memory database
- Test files in `src/test/java/com/handy/appserver/`
- Run tests before any deployment

### API Documentation
- Cart API is documented in `CART_API_DOCUMENTATION.md`
- RESTful endpoints follow `/api/{resource}` pattern
- JWT token required in Authorization header for protected endpoints

## Deployment

### GitHub Actions CI/CD
- Automatic testing on pull requests
- Build and deploy to AWS EC2 for main/develop branches
- Docker containerization with ECR
- Environment-specific deployments (dev on :8081, prod on :8080)

### Manual Deployment Scripts
Various deployment scripts in `/scripts/` directory for EC2, database setup, and service management.

## Common Issues

### Flyway Migration Errors
Most common issue. Use the fix-flyway script and follow migration file rules strictly.

### JWT Token Issues  
Check token expiration (24 hours) and ensure proper Authorization header format.

### Image Upload Issues
Verify AWS S3 configuration and credentials in environment variables.

### Rules 