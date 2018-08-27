Audit Task Generator Tool
==================

Occassionally content may be copied directly into an S3 bucket that happens to have been 
generated originally by DuraCloud.  Since the content bypassed the DuraCloud layer, audit events
and all their downstream consequences (such as audit trail support for the space as well the creation
of a manifest) will not occur.  The need may arise to retroactively generate an audit log and manifest.
Enter the Audit Task Generator Tool.  This tool effectively simulates the events that would 
have occurred had the content been ingested through DuraCloud. 

# Dependencies
This tool has the following dependencies
* The DuraCloud service
* Java 8+

# Building
Once cloned, this tool can be built using:
```
mvn install
```

# Running
AWS credentials associated with permissions
to write to the audit queue are required for this application to run successfully. You can 
pass these credentials using the -Daws.profile="profile your-profile" mechanism. You 
may also set the credentials on the command line using -Daws.accesskeyid and -Daws.secretkey.
Alternatively you can set environment variables AWS_ACCESS_KEY_ID and  AWS_SECRET_KEY. Also
note you must set the region in your profile if it differs from the default. You can also
set -Daws.region or use an environment variable (AWS_REGION).

The tool builds into an executable JAR file  which can be run using the following command: 
```
java -Daws.profile="<profile your-profile>" -jar audit-task-generator-<version>.jar
```
This will display help text that indicates the necessary parameters.