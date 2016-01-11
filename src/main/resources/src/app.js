(function() {
    require('jquery');
    var angular = require('angular');
    require('angular-moment');

    angular.module("email-template", [
        require("angular-material"),
        "ngWebSocket",
        "angularMoment",
        require("./common.js").name,
        require("./inliner.js").name
    ]);

    require("angular-material/angular-material.css");
    require("./app.css");
})();