function AboutCtrl() {

}

function LoginCtrl($scope, $http, $log, $location) {
    $scope.submit = function () {
        $http.post("/login", null,
                {params: {username: $scope.username, password: $scope.password}})
        .success(function (data, status) {
            $log.info(data, status);
            $scope.$emit("LoginSuccessEvent", $scope.username);
            $location.path("/retro");
        })
        .error($log.error);
    }
}

function RetroCtrl($scope, $http, $log) {
    // TODO: $http callback
    $scope.teams = ["TC Sharks", "TC Jets"];
    $scope.sprints = ["TC1211", "TC1210"];
}

