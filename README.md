# WordNet Linker

This project uses a Hadoop MapReduce job to extract relationships and links from the WordNet lexical database. It's built to run in a containerized environment using Docker and Docker Compose, with the final output of the MapReduce job being formatted for easy import into a Neo4j graph database.

## 🚀 Project Overview
The core purpose of this project is to transform the semi-structured data of WordNet into a highly connected graph format. It does this by:

- Using a Hadoop MapReduce job to process the WordNet 3.0 database.

- Extracting explicit relationships (like hypernyms, hyponyms) and implicit links (words found within glosses/definitions).

- Aggregating all relationships for each word.

- Outputting the results in a format suitable for direct import into Neo4j, enabling powerful graph-based queries and visualizations of lexical relationships.

## ⚙️ Prerequisites
- **Docker**: The project relies entirely on Docker and Docker Compose for a consistent and isolated runtime environment.

- **Maven**: To build the Java MapReduce application before creating the Docker image.

- **Java Development Kit (JDK) 8 or later**: For compiling the Java source code.

## 📂 Project Structure
This is the key directory layout for the project:

```
.
├── Dockerfile             			   # Defines the Docker image for the Hadoop application
├── assets/
│   └── WordNet-3.0/
│       └── dict/                      # Contains the WordNet database files (e.g., data.noun, index.verb)
├── docker-compose.yaml                # Orchestrates the Hadoop cluster, the application runner, and Neo4j
├── neo4j_import/                      # Docker volume mount for transferring data to Neo4j
├── src/                               # Maven-standard source directory
│   └── main/
│       └── java/
│           └── com/
│               └── ezkeil/
│                   └── wordlinker/
│                       ├── DictionaryLinker.java      # Main driver for the Hadoop job
│                       ├── LinkExtractorMapper.java   # The core logic for extracting links
│                       └── LinkReducer.java           # Aggregates mapper outputs
└── pom.xml                            # Maven configuration file with dependencies (Hadoop, JWI)
```

## 🛠️ Setup and Usage
Follow these steps to set up the project and run the entire data pipeline.

1. Compile the Java Application
First, you need to compile your Java code and package it into a JAR file using Maven. This will also download the necessary dependencies (JWI, Hadoop client).

```
Bash

mvn clean package
This command will create target/dictionary-linker-1.0-SNAPSHOT.jar.
```


2. Generate the Hadoop Input File
The Hadoop job needs a list of all words (lemmas) to process. This step creates that file.
```
Bash

# You need a small helper program to generate this file from the WordNet data.
# The program should read from assets/WordNet-3.0/dict and output to assets/wordnet_lemmas_input.txt.
# Ensure this file is present before the next step.
```

Once generated, the file assets/wordnet_lemmas_input.txt will be automatically included in the Docker image during the build process.

3. Run the Docker Environment
Start all the services defined in your docker-compose.yaml file, including the Hadoop cluster, your application runner, and the Neo4j database.

```
Bash

docker-compose up --build -d

```

- The --build flag is crucial as it builds the hadoop-app-runner image with your latest compiled JAR and the WordNet data.

- The -d flag runs the containers in the background.

4. Run the Hadoop MapReduce Job
Once the containers are running and the Hadoop services are healthy, you can submit your job.

```
Bash

# 1. Access the application runner container
docker exec -it hadoop-app-runner /bin/bash

# 2. Put the input file into HDFS
hdfs dfs -mkdir -p /user/root/wordnet_input
hdfs dfs -put /app/wordnet_lemmas_input.txt /user/root/wordnet_input/

# 3. Submit the MapReduce job
yarn jar /app/dictionary-linker.jar com.ezkeil.wordlinker.DictionaryLinker /user/root/wordnet_input /user/root/output_wordnet
```

5. Import the Results into Neo4j
After the Hadoop job completes, the output will be in HDFS. You need to get this output and transfer it to Neo4j.

```
Bash

# 1. Get the output file from HDFS and copy it to the local host
# Inside the hadoop-app-runner container:
hdfs dfs -get /user/root/output_wordnet/part-r-00000 /app/wordnet_links.csv
exit
# On the host machine:
docker cp hadoop-app-runner:/app/wordnet_links.csv ./neo4j_import/

# 2. Post-process the file for Neo4j (if necessary)
# You may need a script to parse wordnet_links.csv into separate nodes.csv and relationships.csv files.

# 3. Load the data into Neo4j
# Open the Neo4j Browser at http://localhost:7474 and run the appropriate LOAD CSV commands.
```

## 📚 Technical Details
- **Hadoop Version**: The Docker images are based on Hadoop 3.2.1.

- **WordNet Version**: The project is configured to use WordNet 3.0.

- **Java WordNet Interface (JWI)**: The Java library used to parse WordNet data programmatically.

- **Neo4j**: A graph database used to store and query the extracted lexical relationships.
