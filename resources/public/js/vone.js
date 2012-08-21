angular.module('vone', ['http-auth-interceptor'])
    .config(function ($routeProvider, $httpProvider) {
        $routeProvider
            .when("/about", {templateUrl: "/about", controller: AboutCtrl})
            .when("/login", {templateUrl: "/login", controller: LoginCtrl})
            .when("/retro", {templateUrl: "/retro", controller: RetroCtrl})
            .otherwise({redirectTo: "/about"});
    })
    .directive('authenticate', function($log) {
    	return function(scope, elem, attrs) {
    		$log.info("inauthenticate");
    		var login = elem.find('#loginbox'),
    			main = elem.find('#content');
    		
    		login.hide();
    		
    		scope.$on('event:auth-loginRequired', function() {
    			login.slideDown('slow');
    		});
    		scope.$on('event:auth-loginConfirmed', function() {
    			login.slideDown('slow', function(){
    				main.show();
    				login.slideUp();
    			});
    		});
    	};
    })
    .directive('chart', function($log) {
        return function(scope, elem, attrs) {
            elem[0].innerHTML = "Loading " + attrs.title + "...";
            var chart = new google.visualization[attrs.chart+"Chart"](elem[0]);
            function query(url) {
                // TODO: this options mapping is annoying...
                // how can I embed an options object nicely in html?
                // or make a clojure map that directly creates this object?
                var options = { 
                    title: attrs.title,
                    vAxis: {title: attrs.vtitle},
                    hAxis: {title: attrs.htitle},
                    isStacked: attrs.isstacked,
                    areaOpacity: attrs.areaopacity,
                    width: attrs.width || 800,
                    height: attrs.height || 400
                };
                // TODO: how come 404 isn't handled by response...
                new google.visualization.Query(url)
                    .send(function (response) {
                        if (response.isError()) {
                            google.visualization.errors
                                .addErrorFromQueryResponse(
                                    elem[0], response);
                            return;
                        }
                        chart.draw(response.getDataTable(), options);
                    });
            }
            query(attrs.source);
            //TODO: why does this pass undefined?
            //scope.$watch(attrs.source, query, true);
        };
    })
    .run(function () {
        //console.log("running");
    });

// manual bootstrap, when google api is loaded
google.load('visualization', '1.0', {'packages':['corechart']});
google.setOnLoadCallback(function() {
  angular.bootstrap(document.body, ['vone']);
});

