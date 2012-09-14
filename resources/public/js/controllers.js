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

function RetroCtrl($scope, $routeParams, $location, $rootScope, $http, $log) {
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
            $http.get("/json/sprint-span/" + $scope.sprint)
                .success(function (data) {
                    $scope.sprintBegin = data.BeginDate;
                    $scope.sprintEnd = data.EndDate;
                })
                .error($log.error);
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

function ProjectionsCtrl($scope, $log) {

}

