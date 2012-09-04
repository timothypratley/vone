angular.module('vone', ['http-auth-interceptor', 'charts'])
    .value('global', {})
    .config(function ($routeProvider, $httpProvider) {
        $routeProvider
            .when("/about",
                {templateUrl: "/about", controller: AboutCtrl})
            .when("/login",
                {templateUrl: "/login", controller: LoginCtrl})
            .when("/retro",
                {templateUrl: "/retro", controller: RetroCtrl})
            .when("/retro/:team",
                {templateUrl: "/retro", controller: RetroCtrl})
            .when("/retro/:team/:sprint",
                {templateUrl: "/retro", controller: RetroCtrl})
            .otherwise({redirectTo: "/about"});
    })
    .directive('authenticate', function($log) {
    	return function(scope, elem, attrs) {
    		var login = elem.find('#loginbox');
    		scope.$on('event:auth-loginRequired', function() {
    			login.modal('show');
    		});
    		scope.$on('event:auth-loginConfirmed', function() {
    			login.modal('hide');
    		});
    	};
    })
    .run(function ($http, global, $log) {
        //console.log("running");
        $http.get("/team-sprints", null)
            .success(function (data, status) {
                $log.info("Got team sprints");
                $log.info(data);
                global.teamSprints = data;
            })
            .error($log.error);
    });

// manual bootstrap, when google api is loaded
google.load('visualization', '1.0', {'packages':['corechart']});
google.setOnLoadCallback(function() {
  angular.bootstrap(document.body, ['vone']);
});

