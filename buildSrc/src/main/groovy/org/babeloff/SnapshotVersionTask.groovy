package org.babeloff

import java.time.LocalDate

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class SnapshotVersionTask extends DefaultTask {
    @OutputFile File propFile

    SnapshotVersionTask() {
        group = 'versioning'
        description = 'Makes project a release version.'
    }

    @TaskAction
    void start() {
        def date = LocalDate.now()
        ant.propertyfile(file: propFile) {
            entry(key: 'year', type: 'string', operation: '=', value: date.getYear().toString())
            entry(key: 'month', type: 'string', operation: '=', value: date.getMonthValue().toString())
            entry(key: 'day', type: 'string', operation: '=', value: date.getDayOfMonth())
            entry(key: 'release', type: 'string', operation: '=', value: 'false')
        }
    }
}
