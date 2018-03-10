## How to build and run application.
To run the application in development mode use:
`sbt run`

To run tests, use:
`sbt test`

To build an archieve fro deployment, use:
`sbt dist`

This produces an archieve in a `<project_dir>/tagret/universal/account_service-1.0.zip`.
That archieve is a standalone web application, that can be run the following way:
1. Unzip the archieve
2. Run the script `account_service-1.0/bin/account_service` or `account_service-1.0/bin/account_service.bat`

For more details on this, see: https://www.playframework.com/documentation/2.6.x/Deploying
