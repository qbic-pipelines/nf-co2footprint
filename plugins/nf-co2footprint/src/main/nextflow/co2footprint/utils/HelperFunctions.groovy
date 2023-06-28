package nextflow.co2footprint

public class HelperFunctions {

    static public String convertToReadableUnits(Number value, int unitIndex=4) {
        def units = ['p', 'n', 'u', 'm', ' ', 'K', 'M', 'G', 'T', 'P', 'E']  // Units: pico, nano, micro, mili, 0, Kilo, Mega, Giga, Tera, Peta, Exa
        
        while (value >= 1000 && unitIndex < units.size() - 1) {
            value /= 1000
            unitIndex++
        }
        while (value <= 1 && unitIndex > 0) {
            value *= 1000
            unitIndex--
        }
        
        return "${value} ${units[unitIndex]}"
    }

}