function AboutCtrl() {

}

function LoginCtrl($scope, $http, $log, authService, $rootScope) {
    $scope.submit = function () {
    	$log.info("called login");
        $http.post("/login", null,
                {params: {username: $scope.username, password: $scope.password}})
        .success(function (data, status) {
        	$log.info("Login Confirmed");
            $rootScope.username = $scope.username;
        	authService.loginConfirmed();
        })
        .error($log.error);
    }
}

function RetroCtrl($scope, $routeParams, $location, $rootScope, $log) {
    var match = function(sprints) {
        if ($routeParams.sprint) {
            $scope.sprint = $routeParams.sprint;
        }
        if ($routeParams.team) {
            $scope.team = $routeParams.team;
        }
        if ($scope.team) {
            $scope.sprints = sprints[$scope.team];
        }
        if (!$scope.sprints) {
            $scope.sprints = [];
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
            if ($scope.sprints.indexOf($scope.sprint) < 0) {
                $location.path('/retro/' + newValue);
            } else {
                $location.path('/retro/' + newValue + '/' + $scope.sprint);
            }
        }
    }, true);
    $scope.$watch('sprint', function(newValue) {
        if (newValue) {
            $location.path('/retro/' + $scope.team + '/' + newValue);
        }
    }, true);
}

