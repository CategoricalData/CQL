package org.babeloff

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

class ProjectVersion {
    Integer year
    Integer month
    Integer day
    Boolean release

    ProjectVersion(Integer year, Integer month, Integer day) {
        this.year = year
        this.month = month
        this.day = day
        this.release = Boolean.FALSE
    }

    ProjectVersion(Integer year, Integer month, Integer day, Boolean release) {
        this.year = year
        this.month = month
        this.day = day
        this.release = release
    }

    static ProjectVersion read(Logger logger, File versionFile) {
        logger.quiet 'Reading the version file.'
        if(!versionFile.exists()) {
            throw new GradleException("Required version file does not exist: $versionFile.canonicalPath")
        }
        Properties versionProps = new Properties()
        versionFile.withInputStream { stream -> versionProps.load(stream) }
        ProjectVersion pv = new ProjectVersion(
                versionProps.year.toInteger(),
                versionProps.month.toInteger(),
                versionProps.day.toInteger(),
                versionProps.release.toBoolean())
        logger.quiet 'the version specified is: ' + pv.toString()
        return pv;
    }

    @Override
    String toString() {
        "$year.$month.$day${release ? '' : '-SNAPSHOT'}"
    }
}
