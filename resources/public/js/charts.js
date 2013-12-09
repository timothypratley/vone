angular.module('charts', [])
.value('options', {
  general: {
    height: 500,
    allowHtml: true
  },
  defectRate: {
    visualization: "AreaChart",
    title: "Defect Rate",
    vAxis: {title: "Defects", minValue: 0},
    hAxis: {title: "Date"},
    areaOpacity: 0.0
  },
  burndown: {
    visualization: "AreaChart",
    title: "Burndown - Total ToDo Remaining",
    vAxis: {title: "ToDo Hours", minValue: 0},
    hAxis: {title: "Day", maxValue: 14},
    areaOpacity: 0.0
  },
  burndownComparison: {
    visualization: "AreaChart",
    title: "Burndown Comparison",
    vAxis: {title: "ToDo Hours", minValue: 0},
    hAxis: {title: "Day", maxValue: 14},
    areaOpacity: 0.0
  },
  cumulative: {
    visualization: "AreaChart",
    title: "Cumulative Flow - Story Status Over Time",
    vAxis: {title: "Story Points", minValue: 0},
    hAxis: {title: "Day", maxValue: 14},
    isStacked: true,
    areaOpacity: 0.8
  },
  cumulativePrevious: {
    visualization: "AreaChart",
    title: "Previous Cumulative Flow",
    vAxis: {title: "Story Points", minValue: 0},
    hAxis: {title: "Day", maxValue: 14},
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
  failedReview: {
    visualization: "ColumnChart",
    title: "Failed Review Events per Sprint",
    vAxis: {title: "Count", minValue: 0},
    hAxis: {title: "Sprint"}
  },
  churnComparison: {
    visualization: "ColumnChart",
    title: "Churn - Stories Added and Removed per Sprint",
    vAxis: {title: "Count", minValue: 0},
    hAxis: {title: "Sprint"}
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
  churnStories: {
    visualization: "Table",
    title: "Churn",
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
  roadmap: {
    visualization: "Table",
    title: "Roadmap",
    height: 1000
  },
  fabel: {
    visualization: "Table",
    title: "Fabel",
    height: 1000
  },
  storyFullHistory: {
    visualization: "Table",
    title: "Story History",
    allowHtml: true,
    height: null
  },
  allocation: {
    visualization: "Table",
    title: "Allocation",
    height: 1000
  },
  quality: {
    visualization: "Table",
    title: "Quality",
    height: 1000
  },
  workitems: {
    visualization: "LineChart",
    title: "Workitems",
    vAxis: {
      viewWindowMode: "explicit",
      viewWindow: {min: 0, max: 300}},
    hAxis: {
      viewWindowMode: "explicit",
      viewWindow: {min: new Date(2012,0,1), max: new Date(2013,0,1)}}
  }
})
.directive('chart', function(options, $log) {
  return function(scope, elem, attrs) {
    var chart, query, o = {};
    angular.extend(o, options.general);
    angular.extend(o, options[attrs.chart]);
    angular.extend(o, _.pick(attrs, 'height', 'width'));
    elem[0].innerHTML = "Loading " + o.title + "...";
    chart = new google.visualization[o.visualization](elem[0]);
    query = function() {
      if (!scope.args) {
        return;
      }
      var url = 'datasource/' + attrs.chart + '/' + scope.args;
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
.directive('roadmap', function(options, $log, $http) {
  return function(scope, elem, attrs) {
    var update, chart, query, o = {}, rnd;
    rnd = function(x) {
      return Math.round(100*x)/100;
    };
    angular.extend(o, options.general);
    angular.extend(o, options.roadmap);
    elem[0].innerHTML = "Loading " + o.title + "...";
    chart = new google.visualization[o.visualization](elem[0]);
    update = function() {
      var ii, k, v, jj, header, m = {};
      if (scope.roadmap) {
        header = scope.roadmap[0].slice(3);
        for (ii=1; ii<scope.roadmap.length; ii++) {
          keys = scope.roadmap[ii].slice(0, 3);
          if (!scope.showTeam) {
            keys.splice(2, 1);
          }
          if (!scope.showCustomer) {
            keys.splice(1, 1);
          }
          if (!scope.showProject) {
            keys.splice(0, 1);
          }
          if (keys.length === 0) {
            keys.push("Total");
          }
          k = JSON.stringify(keys);
          v = scope.roadmap[ii].slice(3);
          if (!m[k]) {
            m[k] = v;
          } else {
            for (jj=0; jj<v.length; jj++) {
              m[k][jj] += v[jj];
            }
          }
        }
        pivot = [];
        if (scope.showTeam) {
          header.unshift("Team");
        }
        if (scope.showCustomer) {
          header.unshift("Customer");
        }
        if (scope.showProject) {
          header.unshift("Project");
        }
        if (!scope.showProject && !scope.showCustomer && !scope.showTeam) {
          header.unshift("Total");
        }
        pivot.push(header);
        // TODO: sort, or set column sort on chart
        for (k in m) {
          pivot.push(JSON.parse(k).concat(_.map(m[k], rnd)));
        }
        chart.draw(google.visualization.arrayToDataTable(pivot), o);
      }
    };
    scope.$watch('roadmap', update);
    scope.$watch('showProject', update);
    scope.$watch('showCustomer', update);
    scope.$watch('showTeam', update);
    $http.get("json/roadmap")
    .success(function (data) {
      scope.roadmap = data;
    })
    .error($log.error);
  };
});














