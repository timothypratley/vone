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

function RetroCtrl($scope, $http, $log) {
	$http.get("/team-sprints", null)
		.success(function (data, status) {
			$log.info("Got team sprints");
			$scope.teamSprints = data;
		})
		.error($log.error);
}

