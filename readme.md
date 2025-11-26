This repository is the part of Full stack development project and encapsulates the backend business logic of the web application


Each service in services folder can have its own .env file which should include the required environment variables as a key value pairs.
.env.example file should mention the required keys for the services to work properly.

After cloning the repository, create the required .env file at each directory location wherever the .env.example file is present, which should include the local urls such as postgres db url, etc


To run any service, use this command: gradlew.bat :services:[service-name]:[run-command]
ex. To run auth-service: gradlew.bat :services:auth:run
ex. To run user-service: gradlew.bat :services:user-service:bootRun


To Do
Containerization of backend as a combined service, with individual service containers