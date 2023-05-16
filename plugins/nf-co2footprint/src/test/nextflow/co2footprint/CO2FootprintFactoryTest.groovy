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
import spock.lang.Specification

/**
 *
 * @author Sabrina Krakau <sabrinakrakau@gmail.com>
 */
class CO2FootprintFactoryTest extends Specification {

    def 'should return observer' () {
        when:
        def session = Mock(Session) { getConfig() >> [:] }
        def result = new CO2FootprintFactory().create(session)
        then:
        result.size()==2
        result[0] instanceof CO2FootprintObserver
        result[1] instanceof CO2FootprintReportObserver
    }

}
