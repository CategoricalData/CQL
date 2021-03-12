#!/usr/bin/env bash
./gradlew clean shadowJar &&
scp build/libs/cql-2021.1.28-SNAPSHOT-all.jar catdata@categoricaldata.net:categoricaldata.net/cql.jar
