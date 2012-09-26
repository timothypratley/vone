angular.module('charts', [])
	.value('options', {
		general: {
			width: 1000,
			height: 500
		},
		burndown: {
			visualization: "AreaChart",
			title: "Burndown - Total ToDo Remaining",
			vAxis: {title: "ToDo Hours", minValue: 0},
		    hAxis: {title: "Day"},
			areaOpacity: 0.0
		},
		burndownComparison: {
			visualization: "AreaChart",
			title: "Burndown Comparison",
			vAxis: {title: "ToDo Hours", minValue: 0},
		    hAxis: {title: "Day"},
			areaOpacity: 0.0
		},
		cumulative: {
			visualization: "AreaChart",
			title: "Cumulative Flow - Story Status Over Time",
		    vAxis: {title: "Story Points", minValue: 0},
		    hAxis: {title: "Day"},
		    isStacked: true,
		    areaOpacity: 0.8
		},
		cumulativePrevious: {
			visualization: "AreaChart",
			title: "Previous Cumulative Flow",
		    vAxis: {title: "Story Points", minValue: 0},
		    hAxis: {title: "Day"},
		    isStacked: true,
		    areaOpacity: 0.8
		},
		velocity: {
			visualization: "ColumnChart",
		    title: "Velocity - Story Points per Sprint",
		    vAxis: {title: "Story Points", minValue: 0},
		    hAxis: {title: "Sprint"}
		},
		estimates: {
			visualization: "Table",
			title: "Estimates",
            height: null
		},
		participants: {
			visualization: "Table",
			title: "Participants",
            height: null
		},
		stories: {
			visualization: "Table",
			title: "Stories",
            allowHtml: true,
            height: null
		},
		defects: {
			visualization: "Table",
			title: "Defects",
            allowHtml: true,
            height: null
		},
		testSets: {
			visualization: "Table",
			title: "Test Sets",
            allowHtml: true,
            height: null
		},
		splits: {
			visualization: "Table",
			title: "Splits",
            allowHtml: true,
            height: null
		},
		customers: {
			visualization: "PieChart",
			title: "Customer Focus - Points per Customer"
		},
		customersNext: {
			visualization: "PieChart",
			title: "Customer Focus Next Sprint"
		},
		projections: {
			visualization: "Table",
			title: "Projections",
            height: 1000
		}
	})
	.directive('chart', function(options, $log) {
	    return function(scope, elem, attrs) {
	        var chart, query, o = {};
	    	$.extend(o, options.general);
	    	$.extend(o, options[attrs.chart]);
	        elem[0].innerHTML = "Loading " + o.title + "...";
	        chart = new google.visualization[o.visualization](elem[0]);
	    	query = function() {
	    		if (!scope.team || !scope.sprint) {
	    			return;
	    		}
	    		var url = 'ds/' + attrs.chart + '/' + scope.team + '/' + scope.sprint;
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
            // TODO: don't really want to watch
            scope.$watch("sprint", query, true);
	    };
	})
	.directive('projections', function(options, $log) {
	    return function(scope, elem, attrs) {
	        var chart, query, o = {}, url = 'ds/projections';
	    	$.extend(o, options.general);
	    	$.extend(o, options.projections);
	        elem[0].innerHTML = "Loading " + o.title + "...";
	        chart = new google.visualization[o.visualization](elem[0]);
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
        };
    });

