angular.module('charts', [])
	.value('options', {
		general: {
			width: 800,
			height: 400
		},
		burndown: {
			visualization: "AreaChart",
			title: "Burndown - Total ToDo Remaining",
			vAxis: {title: "ToDo Hours"},
		    hAxis: {title: "Day"},
			areaOpacity: 0.0
		},
		burndownComparison: {
			visualization: "AreaChart",
			title: "Burndown Comparison",
			vAxis: {title: "ToDo Hours"},
		    hAxis: {title: "Day"},
			areaOpacity: 0.0
		},
		cumulative: {
			visualization: "AreaChart",
			title: "Cumulative Flow - Story Status Over Time",
		    vAxis: {title: "Story Points"},
		    hAxis: {title: "Day"},
		    isStacked: true,
		    areaOpacity: 0.8
		},
		cumulativePrevious: {
			visualization: "AreaChart",
			title: "Previous Cumulative Flow",
		    vAxis: {title: "Story Points"},
		    hAxis: {title: "Day"},
		    isStacked: true,
		    areaOpacity: 0.8
		},
		velocity: {
			visualization: "ColumnChart",
		    title: "Velocity - Story Points per Sprint",
		    vAxis: {title: "Story Points"},
		    hAxis: {title: "Sprint"}
		},
		estimates: {
			visualization: "DataTable",
			title: "Estimates",
		},
		customers: {
			visualization: "PieChart",
			title: "Customer Focus - Points per Customer",
		},
		customersNext: {
			visualization: "PieChart",
			title: "Customer Focus Next Sprint"
		}
	})
	.directive('chart', function(options, $log) {
	    return function(scope, elem, attrs) {
	        var chart, query, o = {};
	    	$.extend(o, options.general);
	    	$.extend(o, options[attrs.chart]);
	        elem[0].innerHTML = "Loading " + o.title + "...";
	        
	    	// TODO: might be nicer to call the static drawchart method...
	    	// except it uses containerid...
	    	// https://developers.google.com/chart/interactive/docs/reference#google.visualization.drawchart
	        chart = new google.visualization[o.visualization](elem[0]);


	    	query = function() {
	    		if (!scope.team || !scope.sprint) {
	    			// TODO: how to clear an element?
	    			return;
	    		}
	    			
	    		var url = attrs.chart + '/' + scope.team + '/' + scope.sprint;
	    		$log.info("Quering " + url);
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
	        // TODO: this triggers twice
	        //scope.$watch("sprint", query, true);
	        //TODO: might be better off using a ChartWrapper
	        //https://developers.google.com/chart/interactive/docs/reference#chartwrapperobject	    };
	    };
	});
