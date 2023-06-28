package nextflow.co2footprint

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.trace.TraceRecord
import groovy.json.StringEscapeUtils


import nextflow.co2footprint.HelperFunctions

@Slf4j
@CompileStatic
class CO2Record extends TraceRecord {

    private Double energy
    private Double co2e
    private String name
    // final? or something? to make sure for key value can be set only once?

    CO2Record(Double energy, Double co2e, String name, Map store=[:]) {
        this.energy = energy
        this.co2e = co2e
        this.name = name
        this.store = store
    }

    final public static Map<String,String> FIELDS = [
        co2e:         'num',
        energy:       'num',
        time:         'num',
        cores:        'num',
        core_power:   'num',
        core_usage:   'num',
        memory:       'num',
        memory_power: 'num',
        pue:          'num',
        ci:           'num',
        name:         'str'
    ]

    // TODO implement accordingly to TraceRecord
    Double getEnergyConsumption() { energy }
    String getEnergyConsumptionReadable() { HelperFunctions.convertToReadableUnits(energy,5) }
    String getCO2eReadable() { HelperFunctions.convertToReadableUnits(co2e) }
    Double getCO2e() { co2e }
    String getName() { name }

    //@PackageScope
    Map<String,Object> store

    @Override
    String toString() {
        "${this.class.simpleName} ${store}"
    }

    @Override
    CharSequence renderJson(StringBuilder result, List<String> fields, List<String> formats) {
        final QUOTE = '"'
        final NA = '-'
        if( result == null ) result = new StringBuilder()
        result.deleteCharAt(result.length() - 1); // remove the last character "}"
        result << ','
        for( int i=0; i<fields.size(); i++ ) {
            if( i ) result << ','
            String name = fields[i]
            if ( name == 'name' ) continue // skip the name field (it's already in the key)
            String format = i<formats?.size() ? formats[i] : null
            String value = StringEscapeUtils.escapeJavaScript(getFmtStr(name, format) ?: NA)
            result << QUOTE << name << QUOTE << ":" << QUOTE << value << QUOTE
        }
        result << "}"
        return result
    }

    /**
     * Get a trace field value and apply a conversion rule to it
     *
     * @param name The field name e.g. task_id, status, etc.
     * @param converter A converter string
     * @return A string value formatted according the specified converter
     */
    @Override
    String getFmtStr( String name, String converter = null ) {
        assert name
        def val = store.get(name)

        String sType=null
        String sFormat=null
        if( converter ) {
            int p = converter.indexOf(':')
            if( p == -1 ) {
                sType = converter
            }
            else {
                sType = converter.substring(0,p)
                sFormat = converter.substring(p+1)
            }
        }

        def type = sType ?: FIELDS.get(name)
        if( !type )
            throw new IllegalArgumentException("Not a valid trace field name: '$name'")


        def formatter = FORMATTER.get(type)
        if( !formatter )
            throw new IllegalArgumentException("Not a valid trace formatter for field: '$name' with type: '$type'")

        try {
            return formatter.call(val,sFormat)
        }
        catch( Throwable e ) {
            log.debug "Not a valid trace value -- field: '$name'; value: '$val'; format: '$sFormat'"
            return null
        }
    }
}
