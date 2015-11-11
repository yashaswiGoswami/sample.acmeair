# Acme Air Sample

This application shows an implementation of a fictitious airline called "Acme Air".

## Repository Contents

Source:

- **acmeair-common**: The Java entities used throughout the application
- **acmeair-services**:  The Java data services interface definitions
- **acmeair-service-jpa**:  JPA-based data service implementations
- **acmeair-webapp**:  The Web 2.0 application and associated Java REST services
- **acmeair-itests**:  Integration tests for the application

## Building 

The sample can be build using [Apache Maven](http://maven.apache.org/).

```bash
$ mvn install
```

### Running integration tests

The integration tests are executed against the AcmeAir application running in a Liberty profile server. The project uses [Liberty Maven Plug-in][] to automatically download, install, and configure the Liberty profile runtime for the tests.

```bash
$ mvn install -Prun-its
```

## Deploying to Bluemix

Click the button below to deploy your own copy of this application to [Bluemix](https://bluemix.net). Once the application is deployed, visit *http://&lt;yourAppName&gt;.mybluemix.net/rest/api/loader/loadSmall* first to preload the database with sample data.

[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/WASdev/sample.acmeair.git)

[Liberty Maven Plug-in]: https://github.com/WASdev/ci.maven


