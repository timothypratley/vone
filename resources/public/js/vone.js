angular.module('vone', ['http-auth-interceptor', 'charts'])
    .config(function ($routeProvider, $httpProvider) {
        $routeProvider
            .when("/about",
                {templateUrl: "/about", controller: AboutCtrl})
            .when("/login",
                {templateUrl: "/login", controller: LoginCtrl})
            .when("/logout",
                {templateUrl: "/logout"})
            .when("/retro",
                {templateUrl: "/retro", controller: RetroCtrl})
            .when("/retro/:team",
                {templateUrl: "/retro", controller: RetroCtrl})
            .when("/retro/:team/:sprint",
                {templateUrl: "/retro", controller: RetroCtrl})
            .otherwise({redirectTo: "/about"});
    })
    .directive('authenticate', function($rootScope, $http, $log) {
        $rootScope.logout = function() {
            $http.get("/logout")
                .success(function () {
                    $rootScope.username = null;
                })
                .error($log.error);
        }
    	return function(scope, elem, attrs) {
    		var login = elem.find('#loginbox');
    		scope.$on('event:auth-loginRequired', function() {
                $rootScope.username = null;
    			login.modal('show');
    		});
    		scope.$on('event:auth-loginConfirmed', function() {
    			login.modal('hide');
    		});
    	};
    })
    .run(function ($http, $rootScope, $log) {
        $http.get("/ping")
            .success(function (data) {
                $rootScope.username = angular.fromJson(data);
            })
            .error($log.error);
        // TODO: not sure this belongs here,
        // but I want to avoid calling it everytime retro is visited
        $http.get("/team-sprints")
            .success(function (data, status) {
                $log.info("Got team sprints");
                $log.info(data);
                $rootScope.teamSprints = data;
            })
            .error($log.error);
    });

// manual bootstrap, when google api is loaded
google.load('visualization', '1.0', {'packages':['corechart', 'table']});
google.setOnLoadCallback(function() {
  angular.bootstrap(document.body, ['vone']);
});

