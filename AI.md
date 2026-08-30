# AI Collaboration Log

## Context & Prompt History

**Prompt 1:**
> I am given an assessment to complete. I have to design a car parking availability api based on coordinates as input. Now the car parking data is available as static and live availability is available as a dynamic api which gets the current availability. I want to finalize the design before start the code. So load the context of the documents provided and give me a summary of your understanfing. docs - the assessment doc, static data, live api documentation, live api sample response.
> There is a file you can reference named "20260807-supply-software-engineer-coding-exercise.pdf".
> There is a file you can reference named "carpark-avail-response.json".
> There is a file you can reference named "CarparkAvailability.json".
> There is a file you can reference named "HDBCarparkInformation.csv".

**Prompt 2:**
> I want to start with the design of database of static data and the dynamic api usage. I want to find the pros and cons of these two approaches.
> 1. have a scheduler service, store the static data into the db on startup and scheduler runs every 1 minute fetching the live data and updating the db. client api facing service, just fetches the data from the db and performs filter, sort, paginantion.
> 2. single service, on every api call, run the live data api and get the result and based on that result show the result with filter, sort, paginantion. store the api latest result in a cache. in case of dynamic api miss / exception, fetch from cache latest data.

**Prompt 3:**
> Are you leaning towards using an in-memory store like Redis for the proximity search, or setting up spatial extensions in a relational database like PostgreSQL? I am planning for postgres, but give me the trade offs for both these.

**Prompt 4:**
> So the first step is to create a docker file which will run the postgres with postgis and all the parking space csv data loaded into the postgres with all indexes required.

**Prompt 5:**
> Next we will be creating our scheduler service. This will call the live api every 1 minute and update the data in the db. lets finalize what data has to be stored from the live api to the db, what data is present in the db , any extra columns if we have to add

**Prompt 6:**
> https://data.gov.sg/datasets/d_23f946fa557947f93a8043bbef41dd09/view
> this is the link to the static data. surf this page and find out, is this static or there is some way to update this data ? get a refreshed set ?

**Prompt 7:**
> Great !!
> Lets plan on creating the scheduler,
> 1. The scheduler will run for every 1 minute, Every 1 minute,
     >   a. run the datasource_search api  - GET https://data.gov.sg/api/action/datastore_search?resource_id=d_23f946fa557947f93a8043bbef41dd09 and get the data and create / update the postgres car park data.
     >  b. run the dynamic car park availability api and update the data base.

**Prompt 8:**
> A blank spring initializr project is created with details provided below, Generate the remaining code / business core logic needed for the scheduler service .
> - java 21, spring 4.1.1 ,group - com.wego.carpark , artifact - scheduler, packagename - com.wego.carpark.scheduler,
    > Dependencies
    > Add dependencies...⌘ + b
    > Spring Web Web
    > Build web, including RESTful, applications using Spring MVC. Uses Apache Tomcat as the default embedded container.
    > Lombok Developer Tools
    > Java annotation library which helps to reduce boilerplate code.

**Prompt 9:**
> give me the tradeoffs of creating two separate services vs creating a single multithreaded service, which will take care of both the tasks.

**Prompt 10:**
> make this monolith, add the business logic code for getting the nearest car parkings based on filters and sort them based on proximity. now the projectname is com.wego.carpark scheduler is a module and carparkfetch will be another module.

**Prompt 11:**
> Share teh entire context of the application we are building, will share to codex.

**Prompt 12:**
> Create the DESIGN.md and AImd, In design add the design we have created, no changes in design so far. basic crux of the design, no need to overexplain. In AI.md add all the prompts given in the chat so far.

## Codex Implementation & Verification Prompts

**Prompt 13:**
> Load the context given and fix the compilation issues in the code base. [Complete Car Parking Availability API architectural context and baseline code]

**Prompt 14:**
> Split the entire code to data.dto, data.dao, adapter, controller, scheduler, repository, client, service packages.

**Prompt 15:**
> Move CarparkFetchDao logic to repository, as its a repository call as well. I was expecting to keep the repository facing dtos inside dao and dtos will have the controller, business, scheduler related dtos.

**Prompt 16:**
> [IntelliJ Spring Boot startup log showing `UnknownHostException: db` and database connection failure]

**Prompt 17:**
> docker ps

**Prompt 18:**
> directly run the app on docker

**Prompt 19:**
> [Confirmation to build the standalone Streamlit UI after clarification]

**Prompt 20:**
> add a latitude longitude picker from the map given in the ui

**Prompt 21:**
> [Fresh-clone Docker build failure: `COPY target/app-0.0.1-SNAPSHOT.jar app.jar` failed because `target` was absent]

**Prompt 22:**
> [Docker app logs showing `last_sync_time` missing from `carpark_availability`]

**Prompt 23:**
> this change should be present in init.sql, why its added in the java ? what was the issue ?

**Prompt 24:**
> 2026-08-29T20:14:13.980731 this lastsynced time is correct ?

**Prompt 25:**
> add a ui setting for timezone. keep a current time and current zone at the top right of the page. based on the timezone change the lastsync time in the output.

**Prompt 26:**
> Update the AI.md will all the prompts used here so far.
