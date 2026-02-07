# snailycad-postgre-mariadb-sync
A project to synchronize the FiveM database with a Postgres database from SnailyCad (https://github.com/SnailyCAD/snaily-cadv4).

## English

### Overview
This is a Java Spring Boot application. You need Java and Maven installed on the machine or server.

Install Java and Maven:
- Windows: https://learn.microsoft.com/en-us/java/openjdk/install
- Windows (Maven): https://maven.apache.org/install.html#windows
- Linux: https://docs.microsoft.com/en-us/java/openjdk/install
- Linux (Maven): https://maven.apache.org/install.html#unix

### Installation guide
1) In SnailyCAD, create the following values:
- Gender: male, female
- Ethnicity: create one "none"
- License: driver license, registration status, insurance status

2) In MariaDB, add a `snailycadid` TEXT column to the `users` and `owned_vehicles` tables.

3) Copy `.env.example` to `.env`.
Fill in the database hosts/ports/users/passwords and insert the IDs from the SnailyCAD PostgreSQL tables into the ID fields in `.env`.

4) Build the Maven package to get the JAR:
```bash
mvn clean package
```

### Run methods
Standalone (Windows/Linux):
```bash
java -jar target/<your-jar-name>.jar
```

Docker:
```bash
docker build -t snailycad-postgre-mariadb-sync .
docker compose up -d
```
If your MariaDB/PostgreSQL run in Docker, you may need to adjust the hostnames in `.env` or `docker-compose.yml`.

Once running, you should see the first sync results in the logs.

### Discord
Username: PrisonIndustry  
Server: https://discord.gg/ukx6f2Fpgz

## Deutsch

### Überblick
Dies ist eine Java Spring Boot Anwendung. Java und Maven müssen auf dem PC oder Server installiert sein.

Java und Maven installieren:
- Windows: https://learn.microsoft.com/en-us/java/openjdk/install
- Windows (Maven): https://maven.apache.org/install.html#windows
- Linux: https://docs.microsoft.com/en-us/java/openjdk/install
- Linux (Maven): https://maven.apache.org/install.html#unix

### Installationsanleitung
1) In SnailyCAD folgende Werte anlegen:
- Geschlechter: male, female
- Ethnicity: einmal bitte ein "none" anlgegen
- License: driver license, registration status, insurance status

2) In MariaDB in den Tabellen `users` und `owned_vehicles` eine Spalte `snailycadid` als TEXT anlegen.

3) `.env.example` nach `.env` kopieren.
Host/Port/User/Passwort eintragen und die IDs aus den SnailyCAD PostgreSQL Tabellen in die ID Felder in `.env` eintragen.

4) Maven Paket bauen, um die JAR zu erhalten:
```bash
mvn clean package
```

### Startmethoden
Standalone (Windows/Linux):
```bash
java -jar target/<dein-jar-name>.jar
```

Docker:
```bash
docker build -t snailycad-postgre-mariadb-sync .
docker compose up -d
```
Wenn MariaDB/PostgreSQL in Docker laufen, müssen ggf. die Hostnamen in `.env` oder `docker-compose.yml` angepasst werden.

Wenn alles läuft, solltest du die ersten Sync Ergebnisse in den Logs sehen.

### Discord
Username: PrisonIndustry  
Server: https://discord.gg/ukx6f2Fpgz
