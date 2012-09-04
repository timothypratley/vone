function AboutCtrl() {

}

function LoginCtrl($scope, $http, $log, authService) {
    $scope.submit = function () {
    	$log.info("called login");
        $http.post("/login", null,
                {params: {username: $scope.username, password: $scope.password}})
        .success(function (data, status) {
        	$log.info("Login Confirmed");
        	authService.loginConfirmed();
        })
        .error($log.error);
    }
}

function RetroCtrl($scope, $http, $routeParams, $location, global, $log) {
    $scope.team = null;
    $scope.sprint = null;
    $scope.sprints = [];
    // TODO: how to register when the http returns?
	$scope.teamSprints = global.teamSprints;
    if ($routeParams.sprint) {
        $scope.sprint = $routeParams.sprint;
    }
    if ($routeParams.team) {
        $scope.team = $routeParams.team;
        $scope.sprints = $scope.teamSprints[$scope.team];
        if (!$scope.sprints) {
            // TODO: this case doesn't make sense if we get the data correctly
            $scope.sprints = [];
        }
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

