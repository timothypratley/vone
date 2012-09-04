// TODO: how to put this in the module?
function createChart(scope, elem, attrs, options) {
    var chart, query, o = {};
    elem[0].innerHTML = "Loading " + o.title + "...";
    
	//$.extend(o, attrs);
	$.extend(o, options);
    
	// TODO: might be nicer to call the static drawchart method...
	// except it uses containerid...
	// https://developers.google.com/chart/interactive/docs/reference#google.visualization.drawchart
    chart = new google.visualization[o.chart](elem[0]);

    // TODO: merge general options
	o.width=800;
	o.height=400;

	query = function() {
		if (!scope.team || !scope.sprint) {
			// TODO: how to clear an element?
			return;
		}
			
		var url = o.api + '/' + scope.team + '/' + scope.sprint;
		
		// TODO: remove
    	console.log("Query " + url);

    	// TODO: how come 404 isn't handled by response...
        new google.visualization.Query(url)
            .send(function (response) {
                if (response.isError()) {
                    google.visualization.errors
                        .addErrorFromQueryResponse(
                            elem[0], response);
                } else {
                	chart.draw(response.getDataTable(), o);
                }
            });
    }
    scope.$watch("team", query, true);
    scope.$watch("sprint", query, true);
    //TODO: might be better off using a ChartWrapper
    //https://developers.google.com/chart/interactive/docs/reference#chartwrapperobject
}

angular.module('charts', [])
	.value('options', {
		general: {
			width: 800,
			height: 400
		},
		burndown: {
			chart: "AreaChart",
			title: "Title",
			api: "/burndown",
			vAxis: {title: "ToDo Hours"},
		    hAxis: {title: "Day"},
			areaOpacity: 0.0
		},
		cumulative: {
			chart: "AreaChart",
			title: "Title",
			api: "/cumulative",
		    vAxis: {title: "Story Points"},
		    hAxis: {title: "Day"},
		    isStacked: true,
		    areaOpacity: 0.8
		},
		velocity: {
		    chart: "ColumnChart",
		    title: "Title",
		    api: "/velocity",
		    vAxis: {title: "Story Points"},
		    hAxis: {title: "Sprint"}
		},
		estimates: {
			chart: "Table",
			title: "Title",
			api: "/estimates"
		}
	})
	.directive('chart', function() {
	    return function(scope, elem, attrs) {
	    	createChart(scope, elem, attrs);
	    };
	})
	.directive('burndown', function(options) {
	    return function(scope, elem, attrs) {
	    	createChart(scope, elem, attrs, options.burndown);
	    };
	})
	.directive('cumulative', function(options) {
		return function(scope, elem, attrs) {
	    	createChart(scope, elem, attrs, options.cumulative);
	    };
	})
	.directive('velocity', function(options) {
		return function(scope, elem, attrs) {
	    	createChart(scope, elem, attrs, options.velocity);
		};
	})
	.directive('estimates', function(options) {
		return function(scope, elem, attrs) {
	    	createChart(scope, elem, options.estimate);
		};
	})
	.directive('pie', function() {
	    return function(scope, elem, attrs) {
	    	createChart(scope, elem, attrs);
	    };
	});
