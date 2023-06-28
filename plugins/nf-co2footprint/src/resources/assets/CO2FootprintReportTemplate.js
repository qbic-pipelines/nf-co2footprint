// JavaScript used to power the Nextflow Report Template output.
window.data_byprocess = {};

/* helper functions that takes an array of numbers each of each
   is a integer representing a number of bytes and normalise to base 2 scale */
function norm_mem( list ) {
  if( list == null ) return null;
  var result = new Array(list.length);
  for( i=0; i<list.length; i++ ) {
    var value = list[i];
    var x = Math.floor(Math.log10(value) / Math.log10(1024));
    if( x == 0 )
      value = value/1.024;
    else {
      for( j=0; j<x; j++ )
        value = value / 1.024;
    }
    result[i] = Math.round(value);
  }
  return result;
}

$(function() {
  // Script block clicked
  $('#tasks_table').on('click', '.script_block', function(e){
    e.preventDefault();
    $(this).toggleClass('short');
  });

  $(function() {
    $('[data-toggle="tooltip"]').tooltip()
  })

  // Completed date from now
  var completed_date = moment( $('#workflow_complete').text(), "ddd MMM DD HH:mm:ss .* YYYY" );
  if(completed_date.isValid()){
    $('#completed_fromnow').html('completed ' + completed_date.fromNow() + ', ');
  }

  // Collect metrics by process
  for(let i in window.data.summary){
    let metrics = window.data.summary[i];
    let proc = metrics.process;
    
    if(!window.data_byprocess.hasOwnProperty(proc)){
      window.data_byprocess[proc] = {};
    }

    for (let key in metrics) {
      if (metrics[key] != null) {
        window.data_byprocess[proc][key] = [];
        if( metrics[key].min == metrics[key].max ) {
            // min equals max ==> show just a value
            window.data_byprocess[proc][key].push(metrics[key].min);
        }
        else {
            // otherwise show all values
            window.data_byprocess[proc][key].push(metrics[key].min);
            window.data_byprocess[proc][key].push(metrics[key].q1);
            window.data_byprocess[proc][key].push(metrics[key].q1);
            window.data_byprocess[proc][key].push(metrics[key].q2);
            window.data_byprocess[proc][key].push(metrics[key].q3);
            window.data_byprocess[proc][key].push(metrics[key].q3);
            window.data_byprocess[proc][key].push(metrics[key].max);
        }
        if (key == "co2e") {
          window.data_byprocess[proc]['co2e_readable'] = window.data_byprocess[proc][key].map(function(value){
            [value_co2e, units_co2e] = readable_units_value(value, 4)
            return value_co2e;
          });
          window.data_byprocess[proc][key] = window.data_byprocess[proc][key].map(function(value){
            return Math.round(value);
          });
        } else if (key == "energy") {
          window.data_byprocess[proc]['energy_readable'] = window.data_byprocess[proc][key].map(function(value){
            [value_energy, units_energy] = readable_units_value(value, 5)
            return value_energy;
          });
          window.data_byprocess[proc][key] = window.data_byprocess[proc][key].map(function(value){
            return Math.round(value);
          });
        }
      }
    }
  }

  // Convert to readable units for plots
  function readable_units_value(value, unit_index) {
    units = ['p', 'n', 'u', 'm', ' ', 'K', 'M', 'G', 'T', 'P', 'E']  // Units: pico, nano, micro, mili, 0, Kilo, Mega, Giga, Tera, Peta, Exa
    
    while (value >= 1000 && unit_index < units.length - 1) {
        value /= 1000;
        unit_index++;
    }
    while (value <= 1 && unit_index > 0) {
        value *= 1000;
        unit_index--;
    }
    
    return [ value, units[unit_index] ];
  }

  // Plot histograms of resource usage
  //// Co2e
  var co2e_data = [];
  var energy_data = [];
  var co2e_data_read = [];
  var energy_data_read = [];
  for(var pname in window.data_byprocess){
    if( !window.data_byprocess.hasOwnProperty(pname) )
        continue;
    var smry = window.data_byprocess[pname];
    co2e_data.push({y: smry.co2e, name: pname, type:'box', boxmean: true, boxpoints: false});
    energy_data.push({y: smry.energy, name: pname, type:'box', boxmean: true, boxpoints: false});
    co2e_data_read.push({y: smry.co2e_readable, name: pname, type:'box', boxmean: true, boxpoints: false});
    energy_data_read.push({y: smry.energy_readable, name: pname, type:'box', boxmean: true, boxpoints: false});

  }

  Plotly.newPlot('co2eplot', co2e_data, { title: 'CO2 emission', yaxis: {title: 'CO2 emission (g)', tickformat: '.1f', rangemode: 'tozero'} });
  Plotly.newPlot('energyplot', energy_data, { title: 'Energy consumption', yaxis: {title: 'Energy consumption (KWh)', tickformat: '.1f', rangemode: 'tozero'} });
  
  // Only plot tabbed plots when shown
  $('#pctco2eplot_tablink').on('shown.bs.tab', function (e) {
    if($('#pctco2eplot').is(':empty')){
      Plotly.newPlot('pctco2eplot', co2e_data_read, { title: 'CO2 emission', yaxis: {title: 'CO2 emission ('+units_co2e+'g)', tickformat: '.1f', rangemode: 'tozero'} });
    }
  })
  $('#pctenergyplot_tablink').on('shown.bs.tab', function (e) {
    if($('#pctenergyplot').is(':empty')){
      Plotly.newPlot('pctenergyplot', energy_data_read, { title: 'Energy consumption', yaxis: {title: 'Energy consumption ('+units_energy+'Wh)', tickformat: '.1f', rangemode: 'tozero'} });
    }
  })

  // Convert to readable units
  function readable_units(value, unit_index) {
    units = ['p', 'n', 'u', 'm', ' ', 'K', 'M', 'G', 'T', 'P', 'E']  // Units: pico, nano, micro, mili, 0, Kilo, Mega, Giga, Tera, Peta, Exa
    
    while (value >= 1000 && unit_index < units.length - 1) {
        value /= 1000;
        unit_index++;
    }
    while (value <= 1 && unit_index > 0) {
        value *= 1000;
        unit_index--;
    }
    
    return Math.floor(value) + units[unit_index];
  }
  
  // Humanize duration
  function humanize(duration){
    if (duration > 24) {
      days = Math.floor(duration / 24);
      hours = Math.floor(duration % 24);
      return days + "d " + hours + "h";
    }
    if (duration >= 1) {
      minutes = Math.floor(hours % 60);
      return Math.floor(duration) + "h" + minutes + "m";
    }
    if (duration < 1) {
      minutes = Math.floor(duration * 60);
      if (minutes >= 1) {
        seconds = Math.floor(duration * 60 % 60);
        return minutes + "m " + seconds + "s";
      }
      seconds = Math.floor(duration * 60 * 60);
      return seconds.toFixed(1) + "s";
    }
    return Math.floor(duration * 60 * 60).toFixed(1) + "s";
  }

  // Build the trace table
  function make_index0(ms, type){
    if (type === 'sort') {
      return parseInt(ms);
    }
    if($('#nf-table-humanreadable').val() == 'false'){
      return ms;
    }
    if (ms == '-' || ms == 0){
      return ms;
    }
    return readable_units(ms, 4) + 'g';
  }
  function make_energy(ms, type){
    if (type === 'sort') {
      return parseInt(ms);
    }
    if($('#nf-table-humanreadable').val() == 'false'){
      return ms;
    }
    if (ms == '-' || ms == 0){
      return ms;
    }
    return readable_units(ms, 5) + 'Wh';
  }
  function make_time(ms, type){
    if (type === 'sort') {
      return parseInt(ms);
    }
    if($('#nf-table-humanreadable').val() == 'false'){
      return ms;
    }
    if (ms == '-' || ms == 0){
      return ms;
    }
    return humanize(ms);
  }
  function make_memory(ms, type){
    if (type === 'sort') {
      return parseInt(ms);
    }
    if($('#nf-table-humanreadable').val() == 'false'){
      return ms;
    }
    if (ms == '-' || ms == 0){
      return ms;
    }
    return readable_units(ms, 8) + 'B';
  }

  function make_tasks_table(){
    // reset
      if ( $.fn.dataTable.isDataTable( '#tasks_table' ) ) {
        $('#tasks_table').DataTable().destroy();
      }
      var table = $('#tasks_table').DataTable({
        data: window.data.trace,
        columns: [
          { title: 'task_id', data: 'task_id' },
          { title: 'process', data: 'process' },
          { title: 'tag', data: 'tag' },
          { title: 'status', data: 'status', render: function(data, type, row){
              var s = {
                COMPLETED: 'success',
                CACHED: 'secondary',
                ABORTED: 'danger',
                FAILED: 'danger'
              }
              return '<span class="badge badge-'+s[data]+'">'+data+'</span>';
            }
          },
          { title: 'hash', data: 'hash', render:  function(data, type, row){
              var script = '';
              var lines = data.split("\n");
              var ws_re = /^(\s+)/g;
              var flws_match = ws_re.exec(lines[1]);
              if(flws_match == null){
                script = data;
              } else {
                for(var j=0; j<lines.length; j++){
                  script += lines[j].replace(new RegExp('^'+flws_match[1]), '').replace(/\s+$/,'') + "\n";
                }
              }
              return '<code>'+script+'</code>';
            }
          },
          { title: 'CO2 emissions', data: 'co2e', render: make_index0 },
          { title: 'energy consumption', data: 'energy', render: make_energy },
          { title: 'Time', data: 'time', render: make_time },
          { title: 'Number of cores', data: 'cores' },
          { title: 'Power draw of a computing core', data: 'core_power' },
          { title: 'Core usage factor', data: 'core_usage' },
          { title: 'Size of memory available', data: 'memory', render: make_memory },
          //{ title: 'Power draw of memory', data: 'memory_power', render: make_index0 },
          //{ title: 'Efficiency coefficient of the data center', data: 'pue' },
          //{ title: 'Carbon intensity', data: 'ci', render: make_index0 },
        ],
        "deferRender": true,
        "lengthMenu": [[25, 50, 100, -1], [25, 50, 100, "All"]],
        "scrollX": true,
        "colReorder": true,
        "columnDefs": [
          { className: "id", "targets": [ 0,1,2,3 ] },
          { className: "meta", "targets": [ 4,7,8,9,10,11 ] },
          { className: "metrics", "targets": [ 5,6 ] }
        ],
        "buttons": [
          {
            extend: 'colvisGroup',
            text: 'Metrics',
            show: [ '.id', '.metrics' ],
            hide: [ '.meta' ],
          },
          {
            extend: 'colvisGroup',
            text: 'Metadata',
            show: [ '.id', '.meta'],
            hide: [ '.metrics' ],
          },
          {
            extend: 'colvisGroup',
            text: 'All',
            show: ':hidden',
          },
        ]
      });

      // Insert column filter button group
      table.buttons().container()
         .prependTo( $('#tasks_table_filter') );

      // Column filter button group onClick event to highlight active filter
      $('.buttons-colvisGroup').click(function(){
        var def = 'btn-secondary';
        var sel = 'btn-primary';
        $('.buttons-colvisGroup').removeClass(sel).addClass(def);
        $(this).addClass(sel).removeClass(def);
      });

      // Default filter highlight
      $(".buttons-colvisGroup:contains('All')").click();
    }

  if( window.data.trace==null ) {
      // nascondere
      $('#table-container').remove()
  }
  else {
      $('#no-table-container').remove()
      // Dropdown changed about raw / human readable values in table
      $('#nf-table-humanreadable').change(function(){
        make_tasks_table();
      });
      // Make the table on page load
      make_tasks_table();
  }


});
