/*
 * Copyright 2021, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Create a CSV file containing the CO2 footprint information
 *
 * @author Sabrina Krakau <sabrinakrakau@gmail.com>
 */

package nextflow.co2footprint

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.agent.Agent
import groovy.transform.PackageScope
import nextflow.Session
import nextflow.trace.TraceObserver
import nextflow.trace.TraceRecord
import nextflow.trace.TraceHelper
import nextflow.processor.TaskHandler
import nextflow.processor.TaskId
import nextflow.processor.TaskProcessor


@Slf4j
@CompileStatic
class CO2FootprintObserver implements TraceObserver {

    // TODO
    // onComplete: into same or separate file: sum of CO2 emissions etc.

    public static final String DEF_FILE_NAME = "co2footprint-${TraceHelper.launchTimestampFmt()}.txt"

    // TODO add class member variables? 
    // region, cpu model energy consumption (for beginning) etc. ....

    /**
     * Overwrite existing trace file (required in some cases, as rolling filename has been deprecated)
     */
    boolean overwrite = true

    /**
     * The path where the file is created. It is set by the object constructor
     */
    private Path co2ePath

    /**
     * The actual file object
     */
    private PrintWriter co2eFile

    /**
     * Holds the the start time for tasks started/submitted but not yet completed
     */
    @PackageScope Map<TaskId,TraceRecord> current = new ConcurrentHashMap<>()
    @PackageScope float total_co2 = 0.0

    private Agent<PrintWriter> writer


    /**
     * Create the trace observer
     *
     * @param co2eFile A path to the file where save the CO2 emission data
     */
    CO2FootprintObserver( Path co2eFile ) {
        this.co2ePath = co2eFile
    }

    /** ONLY FOR TESTING PURPOSE */
    protected CO2FootprintObserver( ) {}

    /**
     * Create the trace file, in file already existing with the same name it is
     * "rolled" to a new file
     */
    @Override
    void onFlowCreate(Session session) {
        log.debug "Workflow started -- co2e file: ${co2ePath.toUriString()}"

        // make sure parent path exists
        def parent = co2ePath.getParent()
        if( parent )
            Files.createDirectories(parent)

        // create a new trace file
        co2eFile = new PrintWriter(TraceHelper.newFileWriter(co2ePath,overwrite, 'co2footprint'))

        // launch the agent
        writer = new Agent<PrintWriter>(co2eFile)
        //writer.send { co2eFile.println("Test 0"); co2eFile.flush() }
    }

    /**
     * Save the pending processes and close the trace file
     */
    @Override
    void onFlowComplete() {
        log.debug "Workflow completed -- saving trace file"

        // wait for termination and flush the agent content
        writer.await()

        //writer.send { co2eFile.println("Test CO2 emission is:"); co2eFile.flush() }
        //writer.send { PrintWriter it -> it.println("Test CO2 emission is:"); it.flush() }
        co2eFile.println("The total CO2 emission is: ${co2}")

        // write the remaining records
        // current.values().each { record -> co2eFile.println(render(record)) }
        co2eFile.flush()
        co2eFile.close()
    }

    @Override
    void onProcessCreate(TaskProcessor process) {

    }

    /**
     * This method is invoked before a process run is going to be submitted
     * @param handler
     */
    @Override
    void onProcessSubmit(TaskHandler handler, TraceRecord trace) {
        current[ trace.taskId ] = trace
    }

    /**
     * This method is invoked when a process run is going to start
     * @param handler
     */
    @Override
    void onProcessStart(TaskHandler handler, TraceRecord trace) {
        current[ trace.taskId ] = trace
    }

    /**
     * This method is invoked when a process run completes
     * @param handler
     */
    // TODO write footprint for each process?
    @Override
    void onProcessComplete(TaskHandler handler, TraceRecord trace) {
        final taskId = handler.task.id
        if( !trace ) {
            log.debug "[WARN] Unable to find record for task run with id: ${taskId}"
            return
        }

        // remove the record from the current records
        current.remove(taskId)

        //
        total_co2 += computeTaskCO2footprint(trace)

        // save to the file
        // writer.send { PrintWriter it -> it.println(render(trace)); it.flush() }
    }


    // TODO write footprint for each process?
    @Override
    void onProcessCached(TaskHandler handler, TraceRecord trace) {
        // event was triggered by a stored task, ignore it
        if( trace == null ) {
            return
        }

        //
        total_co2 += computeTaskCO2footprint(trace)

        // save to the file
        // writer.send { PrintWriter it -> it.println(render( trace )); it.flush() }
    }


    // Core function to compute CO2 emissions for each task
    float computeTaskCO2footprint(TraceRecord trace) {
        // C = t * (nc * Pc * uc * nm * Pm) * PUE * CI * 0.001
        // as in https://doi.org/10.1002/advs.202100707
        // TODO factor 0.001 ?

        // Pc: power draw of a computing core
        def pc = 12.0 // TODO does this need to be converted?
        // Pm: power draw of memory (Watt)
        def pm  = 0.3725
        // PUE: efficiency coefficient of the data centre
        def pue = 1.67
        // CI: carbon intensity
        def ci  = 475

        // t: runningtime in hours
        def t  = (trace.get('realtime') as Double)/3600000
        log.info "t: $t"
        // nc: number of cores
        def nc = trace.get('cpus') as Integer
        log.info "nc: $nc"

        // nm: size of memory available (gigabytes) -> requested memory
        if ( trace.get('memory') == null ) {
            // TODO if 'memory' not set, returns null, hande somehow?
            log.error "TraceRecord field 'memory' is not set!"
            System.exit(1)
        }
        def nm = (trace.get('memory') as Long)/1000000000
        log.info "nm: $nm"

        // TODO handle if more memory/cpus used than requested?

        // uc: core usage factor (between 0 and 1)
        // TODO if requested more than used, this is not taken into account, right?
        def cpu_usage = trace.get('%cpu') as Double
        log.info "cpu_usage: $cpu_usage"
        if ( cpu_usage == null ) {
            log.info "cpu_usage is null"
            // TODO why is value null, because task was finished so fast that it was not captured? Or are there other reasons?
            // Assuming requested cpus were used with 100%
            cpu_usage = nc * 100
        }
        // TODO how to handle double, Double datatypes for ceiling?
        def cpus_ceil = Math.ceil( cpu_usage / 100.0 as double )
        def uc = cpu_usage / (100.0 * cpus_ceil)
        log.info "uc: $uc"

        def c = (t * nc * pc * uc * nm * pm * pue * ci * 0.001)
        log.info "CO2: $c"

        return c
    }
}