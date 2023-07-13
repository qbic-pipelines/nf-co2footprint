/*
 * Copyright 2013-2023, Seqera Labs
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

import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import nextflow.trace.TraceRecord

/**
 * Model a process summary data used to render box-plots in the execution HTML report
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */

@Slf4j
@CompileStatic
class CO2FootprintReportSummary {

    private static Map<String,Closure<Double>> mappers = [:]

    static {
        mappers.co2e = { CO2Record co2record -> co2record.getCO2e() as Double }
        mappers.energy = { CO2Record co2record -> co2record.getEnergyConsumption() as Double }
    }



    /**
     * Hold the summary for each series ie. co2e
     */
    private Map<String, Summary> series

    /**
     * List of series names
     */
    private List<String> names = Collections.unmodifiableList(mappers.keySet() as List)

    CO2FootprintReportSummary() {
        this.series = new LinkedHashMap<>()
        for( String name : names ) {
            series[name] = new Summary(mappers[name])
        }
    }

    /**
     * @return The list of series names eg `co2e`
     */
    List<String> getNames() { names }

    /**
     * Add a {@link TraceRecord} for which compute the final summary
     *
     * @param record A {@link TraceRecord} representing a computed task
     */
    void add( CO2Record co2record ) {
        for( int i=0; i< names.size(); i++ ) {
            final name = names[i]
            series[name].add(co2record)
        }
    }

    /**
     * Compute the summary stats for the collected tasks
     *
     * @param
     *      name A series name eg {@code co2e} 
     * @return
     *      A {@link Map} holding the summary containing the following stats:
     *      - min: minimal value
     *      - q1: first quartile
     *      - q2: second quartile ie. median
     *      - q3: third quartile
     *      - max: maximum value
     *      - mean: average value
     *      - minLabel: label fot the task reporting the min value
     *      - q1Label: label fot the task reporting the q1 value
     *      - q2Label: label fot the task reporting the q2 value
     *      - q3Label: label fot the task reporting the q3 value
     *      - maxLabel: label fot the task reporting the max value
     */
    Map<String,?> compute(String name) {

        if( !names.contains(name) )
            throw new IllegalArgumentException("Invalid status field name: $name -- it must be one of the following: ${names.join(',')}")

        return series[name].compute()
    }

    /**
     * Model and compute a series summary
     */
    @Slf4j
    @CompileStatic
    static class Summary {

        private List<CO2Record> tasks

        private Closure<Double> metric

        private BigDecimal total = 0

        private int count

        @PackageScope Double min

        @PackageScope Double max

        @PackageScope String minLabel

        @PackageScope String maxLabel

        @PackageScope String q1Label

        @PackageScope String q2Label

        @PackageScope String q3Label

        Summary( Closure<Double> metric )  {
            this.metric = metric
            this.tasks = []
            this.min
        }

        private void label( def r, int q) {
            if( r instanceof CO2Record ) {
                switch(q) {
                    case 0: minLabel = r.getName(); break
                    case 25: q1Label = r.getName(); break
                    case 50: q2Label = r.getName(); break
                    case 75: q3Label = r.getName(); break
                    case 100: maxLabel = r.getName(); break
                    default:
                        log.debug "Invalid summary stats quantile: $q"
                }
            }
        }

        private BigDecimal round( double value ) {
            Math.round( value * 100 ) / 100
        }

        void add( CO2Record co2record ) {
            final Double value = metric.call(co2record)
            if( value == null )
                return
            count++
            tasks.add(co2record)
            total += value
            if( min==null || value<min ) {
                min = value
                minLabel = co2record.getName()
            }
            if( max==null || value>max ) {
                max = value
                maxLabel = co2record.getName()
            }
        }

        /**
         * Compute the stats for the collected tasks
         *
         * @return
         *      A {@link Map} holding the summary containing the following stats:
         *      - min: minimal value
         *      - q1: first quartile
         *      - q2: second quartile ie. median
         *      - q3: third quartile
         *      - max: maximum value
         *      - mean: average value
         *      - minLabel: label fot the task reporting the min value
         *      - q1Label: label fot the task reporting the q1 value
         *      - q2Label: label fot the task reporting the q2 value
         *      - q3Label: label fot the task reporting the q3 value
         *      - maxLabel: label fot the task reporting the max value
         */
        Map<String,?> compute() {
            if( count==0 )
                return null

            final result = new LinkedHashMap<String,?>(12)
            final sorted = tasks.sort( false, { CO2Record co2record -> metric.call(co2record) } )

            result.mean = total / count as double
            result.min = quantile(sorted, 0)
            result.q1 = quantile(sorted, 25)
            result.q2 = quantile(sorted, 50)
            result.q3 = quantile(sorted, 75)
            result.max = quantile(sorted, 100)

            // discard entry with all zero 
            //if( result.min == 0 && result.min == result.max  )
            //    return null

            result.minLabel = minLabel
            result.maxLabel = maxLabel
            result.q1Label = q1Label
            result.q2Label = q2Label
            result.q3Label = q3Label

            return result
        }

        /**
         * Calculate the q-th quantile in the same way as the R `summary` function does
         * See https://www.r-bloggers.com/exploratory-data-analysis-the-5-number-summary-two-different-methods-in-r/
         *
         * @param items A list of numbers
         * @param q The q-th quantile. It must be a number between 0 and 100
         * @return The q-th quantile value
         */
        protected double quantile(List items, int q) {
            assert items, 'Argument items cannot be empty'
            assert q>=0 && q<=100
            if( q==0 ) {
                final X = items[0]
                label(X,q)
                return metric.call(X)
            }
            if( q==100 ) {
                final X = items[items.size()-1]
                label(X,q)
                return metric.call(X)
            }

            int n = items.size()
            double j = (n-1) * q / 100
            if( j == Math.floor(j) ) {
                final item = items[(int)j]
                label(item,q)
                return metric.call(item)
            }
            else {
                int i = (int)Math.floor(j); int k = (int)Math.ceil(j)
                label(items[i],q)
                final Xi = metric.call(items[i])
                final Xk = metric.call(items[k])
                return Xi + (j-i) * (Xk-Xi)
            }
        }

        protected double[] quantiles(List items) {
            def result = new double[5]
            result[0] = quantile(items,0)
            result[1] = quantile(items,25)
            result[2] = quantile(items,50)
            result[3] = quantile(items,75)
            result[4] = quantile(items,100)
            return result
        }
    }

}
