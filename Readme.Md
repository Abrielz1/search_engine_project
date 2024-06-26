# Project Search Engine

## Project description:

**This application creates indexes of given sites and helps find pages in index.**
### 1. The Dashboard:
The first tab of _Dashboard_ is showed are state of the _**websites** that are **indexed**_ or
it were be indexed in the future.
##### 
![1 tab](https://github.com/Abrielz1/search_engine_project/blob/master/1.png?raw=true)
##### 
#### 2. The states of Indexing:

##### The **_Application_** are has 3 given states of **Indexing**:
1. **_INDEXING_** – the site is in the middle of process being indexed
2. **_INDEXED_** – the site has been indexed successfully
3. **_FAILED_** – an error has occurred or the user has stopped the process and the site is not indexed completely.

### 2. The Management
The second tab of _Dashboard_ contains are:
#####
![2 tab](https://github.com/Abrielz1/search_engine_project/blob/master/2.png?raw=true)
#####
The button ***"Start indexing"*** clears all the previous data and _starts the indexation process all over again_.
Also particular pages of the given websites can be _indexed or updated separately below the button_.
#####
### 3. The Search
The last tab contains The Search.
Here is the search bar wich can search through given sites.
You can choose the site you want to get pages from in the drop-down list above.
#####
![3 tab](https://github.com/Abrielz1/search_engine_project/blob/master/3.png?raw=true)
####
## The Technologies
#### The technologies used in the project are the following:
1. Spring Boot
2. Thymeleaf
3. Spring Boot Data JPA
4. PostgreSQL
5. Docker Compose
6. Jsoup
7. Lombok
8. Apache Lucene Morphology
9. Dictionary reader: English and Russian
10. Hibernate
11. Java 20 (Core, Collections, Optional, Stream)

### Project objectives
#### Practical application was worked out in this project:
* the Spring Boot framework
* Maven Build Systems
* Lombok's libraries for reducing boilerplate code
* using Docker-compose to package PostgreSQL components into a Docker image

## Endpoints
- `[GET] /api/statistics` - Output of statistics and other service information about the status of search indexes and the engine itself
- `[GET] /api/startIndexing` - The method starts full indexing of all sites or full reindexing if they are already indexed.
- `[GET] /api/stopIndexing` - The method stops the current indexing (reindexing) process.
- `[GET] /api/indexPage` - The method adds to the index or updates a separate page, the address of which is passed in the parameter.
- `[GET] /api/search` - The method returns statistics and other service information about the status of the search indexes and the engine itself.

## DataBase scema:
![schema1](https://github.com/Abrielz1/search_engine_project/blob/master/search_engine_db_scema.png?raw=true)

## How to run Application

1. go to path to directory of project: \search_engine_project\docker

2. open terminal in that catalog and type: **_docker-compose up -d_**

3. then in terminal type: **_mvn clean package_**

4. And at last type: **_java -jar "-Dserver.port=8089" ./target/search-engine-1.0-SNAPSHOT.jar_**