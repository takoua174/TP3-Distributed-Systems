Distributed Replication System (TP3)
This project implements a distributed replication system using RabbitMQ for messaging, as specified in TP3 (CAP Disponibilité et Consistance). It supports data replication across three replicas, with clients for writing lines to data.txt files (ClientWriter), reading the last line (ClientReader), and reading all lines with majority voting (ClientReaderV2). The system ensures fault tolerance and availability, aligning with the CAP theorem’s focus on availability and partition tolerance with eventual consistency.
Project Overview

Objective: Prototype a distributed application for data replication on three replicas, using RabbitMQ as a Message-Oriented Middleware (MoM). Implement two strategies: one prioritizing consistency (writes to all replicas) and one balancing availability and consistency (majority voting for reads).
Functionalities (TP3 Tasks):
Task 1: ClientWriter sends write messages to add lines to replicas’ data.txt.
Task 2: Replica processes Write, Read Last, and Read All requests, with three instances (Replica 1, Replica 2, Replica 3).
Task 3: Verify consistent replication of writes across replicas.
Task 4: ClientReader sends Read Last requests and displays the first response.
Task 5: Test fault tolerance by stopping one replica and verifying ClientReader functionality.
Task 6: Simulate a scenario with writes, a replica failure, more writes, and replica restart, checking data.txt inconsistencies.
Task 7: ClientReaderV2 sends Read All requests, collects lines, and displays majority-agreed lines; Replica sends lines one-by-one with an END message.

Layers:
Presentation: ClientWriter, ClientReader, ClientReaderV2, Replica.
Application: MajorityVoter, ReadProcessor.
Messaging: QueueManager, MessagePublisher, MessageConsumer.
Infrastructure: ConfigManager, ErrorHandler.

Setup Instructions

Prerequisites:

Java 17 or higher (java -version).
Maven 3.6+ (mvn -version).
RabbitMQ server (docker run -it --rm --name rabbitmq -p 5672:5672 rabbitmq:3).
Project directory: C:\gl3\s2\ouni\tp\tp3.

Clone and Build:
cd C:\gl3\s2\ouni\tp\tp3
mvn clean install

Configuration:

Edit src/main/resources/config.properties:rabbitmq.host=localhost
rabbitmq.port=5672
reply.queue=reply_queue
replica.queue.prefix=replica

Ensure logs/replication-system.log is writable.

Run Replicas and Clients:
java -cp target/classes presentation.Replica 1
java -cp target/classes presentation.Replica 2
java -cp target/classes presentation.Replica 3
java -cp target/classes presentation.ClientWriter "1 Texte message1"
java -cp target/classes presentation.ClientReader
java -cp target/classes presentation.ClientReaderV2

ErrorHandler Explanation
ErrorHandler.java provides a retry mechanism for transient failures (e.g., file I/O, RabbitMQ connections). It executes an operation up to 3 times with a 1-second delay, logging warnings and errors.
Fields

logger: SLF4J logger.
MAX_RETRIES: 3 attempts.
RETRY_DELAY_MS: 1000 ms.

Methods

executeWithRetry: Retries an operation, returns result or throws exception.

Role
Enhances fault tolerance, used in ReadProcessor and Replica.
ClientReaderV2: Synchronization with CountDownLatch
In ClientReaderV2.java, readAllLines uses a CountDownLatch to wait for all replicas’ responses before processing with MajorityVoter.
How It Works

Sends Read All requests to replicas.
Collects lines and END messages from reply_queue.
CountDownLatch (count=3) waits for END messages or 5 seconds.

ClientReaderV2: Thread Safety with synchronized
readAllLines uses synchronized in DeliverCallback for thread-safe updates to allLines.
What It Means

Prevents race conditions when RabbitMQ callback threads add lines to allLines.
synchronized (allLines) ensures one thread executes add or countDown at a time.

CountDownLatch in ClientReaderV2
CountDownLatch synchronizes readAllLines to wait for replica responses.
What is a CountDownLatch?

Blocks threads until count reaches 0 via countDown().
Initialized with count 3, decremented on END messages.

Role
Ensures MajorityVoter processes all lines, with a 5-second timeout for fault tolerance.
ClientReaderV2: Fixing the CountDownLatch Issue
Originally, CountDownLatch wasn’t decremented, causing a 5-second wait. The fix adds countDown() for END messages.
Solution

ReadProcessor sends END after lines.
ClientReaderV2 calls latch.countDown() on END.

Impact
Achieves precise synchronization, completing when all replicas send END.
ClientReaderV2: Why the CountDownLatch Fix is in ReadProcessor
The END message is sent by ReadProcessor, not MessageConsumer or MessagePublisher.
Why ReadProcessor?

Handles Read All, knows when all lines are sent.
Sends END after the while loop:publisher.publish(replyQueue, "END", correlationId, null);

Why Not MessageConsumer/Publisher?

MessageConsumer: Processes messages, unaware of response completion.
MessagePublisher: Sends messages, doesn’t define protocol.

Testing Instructions
Run Unit Tests

Start RabbitMQ:docker run -it --rm --name rabbitmq -p 5672:5672 rabbitmq:3

Run Tests:cd C:\gl3\s2\ouni\tp\tp3
mvn test

Verify:
Tests pass for ClientWriterTest, ReplicaTest, ClientReaderTest, ClientReaderV2Test.
Fix failures by aligning mocks with actual implementations.

Manual Testing

Prepare Directories:

Create replica1, replica2, replica3 in C:\gl3\s2\ouni\tp\tp3.
Verify:mkdir replica1 replica2 replica3
dir C:\gl3\s2\ouni\tp\tp3\replica\*

Task 1 & 3: Test Write and Replication:

Run replicas:java -cp target/classes presentation.Replica 1
java -cp target/classes presentation.Replica 2
java -cp target/classes presentation.Replica 3

Write lines:java -cp target/classes presentation.ClientWriter "1 Texte message1"
java -cp target/classes presentation.ClientWriter "2 Texte message2"

Verify data.txt in replica1/data.txt, replica2/data.txt, replica3/data.txt:1 Texte message1
2 Texte message2

Check logs (logs/replication-system.log):
INFO: Wrote line to replica X: ...

Task 4: Test Read Last:

Run ClientReader:java -cp target/classes presentation.ClientReader

Verify output:Last line: 2 Texte message2

Check logs:
INFO: Sent last line from replica X: 2 Texte message2

Task 5: Test Fault Tolerance (Read Last):

Stop Replica 2 (Ctrl+C).
Run ClientReader:java -cp target/classes presentation.ClientReader

Verify output:Last line: 2 Texte message2

Check logs for timeout warning if no response.

Task 6: Simulate Failure and Inconsistency:

Run replicas.
Write:java -cp target/classes presentation.ClientWriter "1 Texte message1"
java -cp target/classes presentation.ClientWriter "2 Texte message2"

Stop Replica 2 (Ctrl+C).
Write:java -cp target/classes presentation.ClientWriter "3 Texte message3"
java -cp target/classes presentation.ClientWriter "4 Texte message4"

Restart Replica 2:java -cp target/classes presentation.Replica 2

Verify data.txt:
replica1/data.txt, replica3/data.txt:1 Texte message1
2 Texte message2
3 Texte message3
4 Texte message4

replica2/data.txt:1 Texte message1
2 Texte message2

Note: Inconsistency due to Replica 2 missing lines 3 and 4.

Task 7: Test Read All (ClientReaderV2):

Run replicas (use data.txt from Task 6).
Run ClientReaderV2:java -cp target/classes presentation.ClientReaderV2

Verify output:Majority lines:
1 Texte message1
2 Texte message2

Note: Lines 3 and 4 appear in only 2 replicas, may not pass majority voting (depends on MajorityVoter).
Check logs:
INFO: Sent all lines and END message for correlationId: ...
DEBUG: Received END message, latch count: X

Troubleshooting:

No Output: Ensure replicas are running and data.txt exists.
Latch Issues: Verify ReadProcessor sends END and ClientReaderV2 decrements latch.
RabbitMQ Errors: Check config.properties and RabbitMQ logs (docker logs rabbitmq).

Project Structure
C:\gl3\s2\ouni\tp\tp3
├── replica1
│ ├── data.txt
├── replica2
│ ├── data.txt
├── replica3
│ ├── data.txt
├── src
│ ├── main
│ │ ├── java
│ │ │ ├── application
│ │ │ │ ├── MajorityVoter.java
│ │ │ │ ├── ReadProcessor.java
│ │ │ ├── infrastructure
│ │ │ │ ├── ConfigManager.java
│ │ │ │ ├── ErrorHandler.java
│ │ │ ├── messaging
│ │ │ │ ├── MessageConsumer.java
│ │ │ │ ├── MessagePublisher.java
│ │ │ │ ├── QueueManager.java
│ │ │ ├── presentation
│ │ │ │ ├── ClientReader.java
│ │ │ │ ├── ClientReaderV2.java
│ │ │ │ ├── ClientWriter.java
│ │ │ │ ├── Replica.java
│ │ ├── resources
│ │ │ ├── config.properties
│ │ │ ├── logback.xml
│ ├── test
│ │ ├── java
│ │ │ ├── presentation
│ │ │ │ ├── ClientReaderTest.java
│ │ │ │ ├── ClientReaderV2Test.java
│ │ │ │ ├── ClientWriterTest.java
│ │ │ │ ├── ReplicaTest.java
├── logs
│ ├── replication-system.log
├── pom.xml
├── README.md

Dependencies
Update pom.xml:
<dependencies>
<dependency>
<groupId>com.rabbitmq</groupId>
<artifactId>amqp-client</artifactId>
<version>5.14.2</version>
</dependency>
<dependency>
<groupId>org.slf4j</groupId>
<artifactId>slf4j-api</artifactId>
<version>1.7.36</version>
</dependency>
<dependency>
<groupId>ch.qos.logback</groupId>
<artifactId>logback-classic</artifactId>
<version>1.2.11</version>
</dependency>
<dependency>
<groupId>org.junit.jupiter</groupId>
<artifactId>junit-jupiter</artifactId>
<version>5.9.1</version>
<scope>test</scope>
</dependency>
<dependency>
<groupId>org.mockito</groupId>
<artifactId>mockito-core</artifactId>
<version>4.8.0</version>
<scope>test</scope>
</dependency>
</dependencies>

qst 1:
java -cp "target/classes;target/dependency/_" presentation.Replica 1
java -cp "target/classes;target/dependency/_" presentation.ClientWriter
