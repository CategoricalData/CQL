#!/usr/bin/env bash
./gradlew clean shadowJar generateManual &&
scp build/libs/cql-2021.1.28-SNAPSHOT-all.jar catdata@categoricaldata.net:categoricaldata.net/cql.jar &&
scp -r help catdata@categoricaldata.net:categoricaldata.net/.
