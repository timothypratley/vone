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
	$http.get("/teams", null)
	.success(function (data, status) {
		$log.info("Got teams");
		$scope.teams = data;
	})
	.error($log.error);
}

