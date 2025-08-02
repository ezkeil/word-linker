# Use a pre-built Hadoop base image.
# big-data-europe/hadoop-base:3.2.1 is a good option as it often includes Java.
# If you face issues with Java, you might need to use a base image with Java pre-installed
# or add explicit Java installation steps.
FROM big-data-europe/hadoop-base:3.2.1

# Set environment variables for Hadoop (often set by base image, but good to be explicit)
ENV HADOOP_HOME /opt/hadoop
ENV PATH $PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

# Create a working directory inside the container
WORKDIR /app

# Copy your compiled Hadoop application JAR
# Make sure to build your JAR first (e.g., 'mvn clean package') before building the Docker image.
# Assuming your JAR is named 'dictionary-linker-1.0-SNAPSHOT.jar' and is in your 'target/' directory.
COPY target/dictionary-linker-1.0-SNAPSHOT.jar /app/dictionary-linker.jar

# --- NEW: Copy the WordNet dictionary files ---
# Your WordNet 'dict' folder is at assets/WordNet-3.0/dict/
# We want to copy its *contents* to /app/wordnet_data/ in the container
# The trailing slash on the source `assets/WordNet-3.0/dict/.` is important to copy contents
COPY assets/WordNet-3.0/dict/. /app/wordnet_data/

# Set the entrypoint to bash to allow interactive commands and later execution of Hadoop job
ENTRYPOINT ["/bin/bash"]