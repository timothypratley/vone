google.load('visualization', '1.0', {'packages':['corechart']});
google.setOnLoadCallback(drawChart);

// Callback that creates and populates a data table, 
// instantiates the pie chart, passes in the data and
// draws it.
function drawChart() {

	  // BURNDOWN
	  var data = google.visualization.arrayToDataTable([
	    ['Month',   'Bolivia', 'Ecuador', 'Madagascar', 'Papua New Guinea', 'Rwanda'],
	    ['2004/05',    165,      938,         522,             998,           450],
	    ['2005/06',    135,      1120,        599,             1268,          288],
	    ['2006/07',    157,      1167,        587,             807,           397],
	    ['2007/08',    139,      1110,        615,             968,           215],
	    ['2008/09',    136,      691,         629,             1026,          366]
	  ]);

	  // Create and draw the visualization.
	  var burndown = new google.visualization.AreaChart(document.getElementById('burndown'));
	  burndown.draw(data, {
	    title : 'Cumulative Flow - Story status over time',
	    isStacked: false,
	    areaOpacity: 0.0,
	    width: 600,
	    height: 400,
	    vAxis: {title: "Story Points"},
	    hAxis: {title: "Day"}
	  });

	
	  // CUMULATIVE FLOW
	  var data = google.visualization.arrayToDataTable([
	    ['Month',   'Bolivia', 'Ecuador', 'Madagascar', 'Papua New Guinea', 'Rwanda'],
	    ['2004/05',    165,      938,         522,             998,           450],
	    ['2005/06',    135,      1120,        599,             1268,          288],
	    ['2006/07',    157,      1167,        587,             807,           397],
	    ['2007/08',    139,      1110,        615,             968,           215],
	    ['2008/09',    136,      691,         629,             1026,          366]
	  ]);

	  // Create and draw the visualization.
	  var cumulative = new google.visualization.AreaChart(document.getElementById('cumulative'));
	  cumulative.draw(data, {
	    title : 'Cumulative Flow - Story status over time',
	    isStacked: true,
	    width: 600,
	    height: 400,
	    vAxis: {title: "Story Points"},
	    hAxis: {title: "Day"}
	  });

	  
	  // PIE CHART
	  // Create the data table.
	  var data = new google.visualization.DataTable();
	  data.addColumn('string', 'Topping');
	  data.addColumn('number', 'Slices');
	  data.addRows([
	    ['Mushrooms', 3],
	    ['Onions', 1],
	    ['Olives', 1], 
	    ['Zucchini', 1],
	    ['Pepperoni', 2]
	  ]);

	  // Set chart options
	  var options = {title: "Customer story points",
	                 width: 400,
	                 height: 300};
	  // Instantiate and draw our chart, passing in some options.
	  var customers = new google.visualization.PieChart(document.getElementById('customers'));
	  customers.draw(data, options);
}
