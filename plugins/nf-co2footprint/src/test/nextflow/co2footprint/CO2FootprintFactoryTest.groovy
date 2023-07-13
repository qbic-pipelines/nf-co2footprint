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

package nextflow.co2footprint

import nextflow.Session
import nextflow.trace.TraceRecord
import spock.lang.Specification

/**
 *
 * @author Sabrina Krakau <sabrinakrakau@gmail.com>
 */
class CO2FootprintFactoryTest extends Specification {

    private BigDecimal round( double value ) {
        Math.round( value * 100 ) / 100
    }

    def 'should return observer' () {
        when:
        def session = Mock(Session) { getConfig() >> [:] }
        def result = new CO2FootprintFactory().create(session)
        then:
        result.size()==2
        result[0] instanceof CO2FootprintFactory.CO2FootprintTextFileObserver
        result[1] instanceof CO2FootprintFactory.CO2FootprintReportObserver
    }

    def 'test co2e calculation' () {
        given:
        def traceRecord = new TraceRecord()
        traceRecord.realtime = (1 as Long) * (3600000 as Long)
        traceRecord.cpus = 1
        traceRecord.cpu_model = "Unknown model"
        traceRecord.'%cpu' = 100.0
        traceRecord.memory = (7 as Long) * (1000000000 as Long)

        def results = new CO2FootprintFactory().computeTaskCO2footprint(traceRecord)

        expect:
        // Energy consumption in mWh
        round(results[0]) == 24394.53
        // CO2 in mg
        round(results[1]) == 11587.40
    }
}
