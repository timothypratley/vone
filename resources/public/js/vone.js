angular.module('vone', ['http-auth-interceptor', 'charts'])
.config(function ($routeProvider, $httpProvider) {
  $routeProvider
  .when("/about",
        {templateUrl: "about", controller: AboutCtrl})
  .when("/login",
        {templateUrl: "login", controller: LoginCtrl})
  .when("/logout",
        {templateUrl: "logout"})
  .when("/retro",
        {templateUrl: "selectRetro", controller: RetroCtrl})
  .when("/retro/:team",
        {templateUrl: "selectRetro", controller: RetroCtrl})
  .when("/retro/:team/:sprint",
        {templateUrl: "retro", controller: RetroCtrl})
  .when("/roadmap",
        {templateUrl: "roadmap", controller: RoadmapCtrl})
  .when("/fable",
        {templateUrl: "fable", controller: FableCtrl})
  .when("/status",
        {templateUrl: "status", controller: StatusCtrl})
  .when("/teamquality",
        {templateUrl: "teamquality", controller: TeamQualityCtrl})
  .when("/projectdefectrate",
        {templateUrl: "projectdefectrate", controller: ProjectDefectRateCtrl})
  .when("/projectopenitems",
        {templateUrl: "projectopenitems", controller: ProjectOpenItemsCtrl})
  .when("/overall",
        {templateUrl: "overall", controller: OverallCtrl})
  .when("/members",
        {templateUrl: "members", controller: MembersCtrl})
  .when("/member/:member",
        {templateUrl: "member", controller: MemberCtrl})
  .when("/rankings",
        {templateUrl: "rankings", controller: RankingsCtrl})
  .when("/history",
        {templateUrl: "history", controller: HistoryCtrl})
  .when("/history/:number",
        {templateUrl: "history", controller: HistoryCtrl})
  .otherwise({redirectTo: "/status"});
})
.directive('authenticate', function($rootScope, $http, $log) {
  $rootScope.logout = function() {
    $http.get("logout")
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
  $http.get("json/team-sprints")
  .success(function (data, status) {
    $log.info("Got team sprints");
    $log.info(data);
    $rootScope.teamSprints = data;
    $rootScope.teams = _.chain($rootScope.teamSprints)
    .map(function (sprints, team) {
      return {name: team, enabled: false};
    })
    .compact()
    .sortBy(function (x) { return x.name; })
    .value();
    $log.info($rootScope.teams);
  })
  .error($log.error);
  $http.get("json/current-sprints")
  .success(function (data, status) {
    $log.info("Got current sprints");
    $log.info(data);
    $rootScope.currentSprints = data;
  })
  .error($log.error);
  $http.get("json/projects")
  .success(function(data, status) {
    $log.info("Got projects");
    $log.info(data);
    $rootScope.projects = _.map(data, function (x) { return {name: x, enabled: false}; });
  })
  .error($log.errror);
});

// manual bootstrap, when google api is loaded
google.load('visualization', '1', {'packages':['corechart', 'table']});
google.setOnLoadCallback(function() {
  angular.bootstrap(document.body, ['vone']);
});
