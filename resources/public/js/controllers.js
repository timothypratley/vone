function now() {
  return new Date().toDateString();
}


function AboutCtrl() {

}

function LoginCtrl($scope, $http, $log, authService, $rootScope) {
  $scope.submit = function () {
    $log.info("called login");
    $http.post("login", null,
               {params: {username: $scope.username, password: $scope.password}})
    .success(function (data, status) {
      $log.info("Login Confirmed");
      $rootScope.username = $scope.username;
      authService.loginConfirmed();
    })
    .error($log.error);
  }
}

// http://unixpapa.com/js/querystring.html
var decode= function(s)
{
  s= s.replace(/\+/g,' ');
  s= s.replace(/%([EF][0-9A-F])%([89AB][0-9A-F])%([89AB][0-9A-F])/g,
               function(code,hex1,hex2,hex3)
               {
                 var n1= parseInt(hex1,16)-0xE0;
                 var n2= parseInt(hex2,16)-0x80;
                 if (n1 == 0 && n2 < 32) return code;
                 var n3= parseInt(hex3,16)-0x80;
                 var n= (n1<<12) + (n2<<6) + n3;
                 if (n > 0xFFFF) return code;
                 return String.fromCharCode(n);
               });
  s= s.replace(/%([CD][0-9A-F])%([89AB][0-9A-F])/g,
               function(code,hex1,hex2)
               {
                 var n1= parseInt(hex1,16)-0xC0;
                 if (n1 < 2) return code;
                 var n2= parseInt(hex2,16)-0x80;
                 return String.fromCharCode((n1<<6)+n2);
               });
  s= s.replace(/%([0-7][0-9A-F])/g,
               function(code,hex)
               {
                 return String.fromCharCode(parseInt(hex,16));
               });
  return s;
};

function RetroCtrl($scope, $routeParams, $location, $rootScope, $http, $log) {
  var match = function(teamSprints) {
    if ($routeParams.sprint) {
      $scope.sprint = decode($routeParams.sprint);
    }
    if ($routeParams.team) {
      $scope.team = decode($routeParams.team);
    }
    if (teamSprints && $scope.team) {
      $scope.sprints = teamSprints[$scope.team];
    } else {
      $scope.sprints = [];
    }
    if ($scope.sprint) {
      $http.get("json/sprint-span/" + $scope.sprint)
      .success(function (data) {
        $scope.sprintBegin = data.BeginDate;
        $scope.sprintEnd = data.EndDate;
      })
      .error($log.error);
      $http.get("json/feedback/" + $scope.team + '/' + $scope.sprint)
      .success(function (data) {
        $scope.feedback = angular.fromJson(data);
      })
      .error($log.error);
      $scope.args = $scope.team + '/' + $scope.sprint;
    } else {
      $scope.args = null;
    }
  };
  $scope.team = null;
  $scope.sprint = null;
  $scope.sprints = [];
  if ($rootScope.teamSprints) {
    match($rootScope.teamSprints);
  } else {
    $rootScope.$watch('teamSprints', match);
  }
  $scope.$watch('team', function(newValue) {
    if (newValue) {
      $location.path('retro/' + newValue);
    }
  }, true);
  $scope.$watch('sprint', function(newValue) {
    if (newValue) {
      $location.path('retro/' + $scope.team + '/' + newValue);
    }
  }, true);
}

function StatusCtrl($scope, $routeParams, $location, $rootScope, $http, $log) {
  var match = function() {
    $scope.argss = _.chain($rootScope.teamSprints)
    .map(function (sprints, team) {
      var currentSprint = _.intersection(sprints, $rootScope.currentSprints)[0];
      if (_.isUndefined(currentSprint)) {
        return undefined;
      }
      return {name: team + '/' + currentSprint, enabled: false};
    })
    .compact()
    .sortBy(function (x) { return x.name; })
    .value();

    $scope.selectAll = function(enabled) {
      angular.forEach($scope.argss, function(x) { x.enabled = enabled;});
    };
  };

  $scope.today = now();
  if ($rootScope.teamSprints) {
    match($rootScope.teamSprints);
  } else {
    $rootScope.$watch('teamSprints', match);
  }
}

function RoadmapCtrl($scope, $http, $log) {

}

function FableCtrl($scope, $http, $log) {

}

function OverallCtrl($scope, $http, $log) {

}

function TeamQualityCtrl($scope, $http, $log, $rootScope) {
  $scope.today = now();

  $scope.selectAll = function(enabled) {
    angular.forEach($rootScope.teams, function(x) { x.enabled = enabled;});
  };
}

function ProjectDefectRateCtrl($scope, $http, $log, $rootScope) {
  $scope.today = now();
  $scope.selectAll = function(enabled) {
    angular.forEach($rootScope.projects, function(x) { x.enabled = enabled;});
  };
}

function ProjectOpenItemsCtrl($scope, $http, $log, $rootScope) {
  $scope.today = now();
  $scope.selectAll = function(enabled) {
    angular.forEach($rootScope.projects, function(x) { x.enabled = enabled;});
  };
}

function MembersCtrl($scope, $http, $log) {
  $scope.args = "2013";
}

function MemberCtrl($scope, $routeParams, $log) {
  $scope.args = $routeParams.member;
  $scope.member = $routeParams.member;
}

function RankingsCtrl($scope, $http, $log) {
  $http.get('json/rankings')
  .success(function (data) {
    $scope.members = angular.fromJson(data);
    $log.info($scope.members);
  })
  .error($log);
}

function HistoryCtrl($scope, $routeParams, $location, $rootScope, $http, $log) {
  if ($routeParams.number) {
    $scope.args = decode($routeParams.number);
  } else {
    $scope.args = null;
  }
  $scope.$watch('args', function(newValue) {
    if (newValue) {
      $location.path('history/' + newValue);
    }
  }, true);
}
