# Base Service
The idea behind writing a base service it for me to create a minimal framework for writing non-blocking async http services without having to deal with all the plumbing over and over again. That said, the framework should not rely on heavyweight third party servers and be nimble enough and extensible.

# Requirements
### 1. Create a service framework that provides common cross-cutting concerns for writing http services like:
#### a. Security - authentication, authorization
#### b. Role Based Access Control (RBAC)
#### c. Rate limiting & DOS prevention
#### d. Anomaly detection
#### e. Response caching, where applicable
#### f. Misc configurable interceptors
### 2. Base Service should be lightweight and void of heavyweight dependencies. The process should have small rss size, should have a tiny startup time and should be frugal on resource consumption.
### 3. Base Service should provide non-blocking I/O unless absolutely constrained by downstream dependencies
### 4. Base Service should be stateless and highly scalable
### 5. Base Service should be highly observable
### 6. Base Service should be easy to debug and debug quickly
### 7. Base Service should provide graceful shutdown


# Usage Manual


