google.load('visualization', '1.0', {'packages':['corechart']});
google.setOnLoadCallback(drawChart);

function cb(response) {
	console.log("yes");
}

function chart(element, url, chartType, options) {
	new google.visualization.Query(url)
		.send(function (response) {
			var container = document.getElementById(element);
			console.log('toot');
			console.log(response.getDataTable());
			if (response.isError()) {
				google.visualization.errors.addErrorFromQueryResponse(container, response);
				return;
			}
		
			new chartType(container)
				.draw(response.getDataTable(), options);
		});
}

function drawChart() {
    chart('burndown', '/burndown/TC+Sharks/TC1211', google.visualization.AreaChart, {
	    title : 'Cumulative Flow - Story status over time',
	    isStacked: false,
	    areaOpacity: 0.0,
	    width: 600,
	    height: 400,
	    vAxis: {title: "Story Points"},
	    hAxis: {title: "Day"}
    });
    chart('cumulative', '/cumulative/TC+Sharks/TC1211', google.visualization.AreaChart, {
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
