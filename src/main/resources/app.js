require('jquery');
var angular = require('angular');
require('angular-material');
var moment = require('moment');
require('moment-timezone');
require('angular-moment');
require('angular-material/angular-material.css');
require('./app.css');

(function() {
    var module = angular.module("email-template", [
        "ngMaterial",
        "ngAnimate",
        "ngWebSocket",
        "angularMoment"
    ]);

    module.service("api", ["$rootScope", "$location", "$websocket", "$http", "$interval",
        function($root, $location, $websocket, $http, $interval) {
            var url = $location.host();
            if($location.port() !== 80 && $location.port() !== 443)
                url += ":" + $location.port();

            var httpUrl = $location.protocol() + "://" + url;
            var wsUrl = $location.protocol() === "http" ? "ws://" + url : "wss://" + url;

            var socket = $websocket(wsUrl + "/connect");

            socket.onMessage(function(event) {
                $root.$broadcast("file", angular.fromJson(event.data));
            });

            $interval(function() {
                socket.send("KEEPALIVE");
            }, 60000);

            this.getHttpUrl = function() {
                return httpUrl;
            };

            this.getFiles = function() {
                return $http.get(httpUrl + "/api/files").then(function(rsp) {
                    return rsp.data;
                });
            };

            this.sendTestEmail = function(id, emails) {
                return $http.put(httpUrl + "/api/files/" + id + "/sendtest",
                    null,
                    {
                        params: {
                            email: emails
                        }
                    }
                ).then(function(rsp) {
                    return rsp.data;
                });
            };
        }
    ]);

    module.controller("inlineCtrl", ["$scope", "$timeout", "$mdToast", "api",
        function($scope, $timeout, $mdToast, $api) {

            $scope.files = {};

            $scope.selected = undefined;

            $api.getFiles().then(function(files) {
                files.sort(function(a, b) {
                    return a.name.localeCompare(b.name);
                });
                files.forEach(function(file) {
                    $scope.files[file.id] = file;
                    file.created = moment(file.created);
                    file.modified = moment(file.modified);
                    file.viewed = moment();
                });


                selectFirst();
            });

            $scope.$on("file", function(event, file) {
                if(!$scope.files[file.id]) {
                    $scope.files[file.id] = file;
                    file.created = moment(file.created);
                    file.modified = moment(file.modified);
                    file.viewed = moment();

                    if(!$scope.selected)
                        selectFirst();
                } else {
                    var f = $scope.files[file.id];
                    angular.extend(f, file);
                    f.created = moment(file.created);
                    f.modified = moment(file.modified);

                    if($scope.selected.id === file.id) {
                        var viewPath = $scope.selected.viewPath;
                        $scope.selected.viewPath = undefined;
                        $timeout(function() {
                            $scope.selected.viewPath = viewPath;
                        }, 1);
                    }
                }
            });

            $scope.doSelect = function(id) {
                $scope.selected = undefined;

                $timeout(function() {
                    $scope.selected = $scope.files[id];
                    $scope.selected.viewed = moment();
                }, 1);
            };

            $scope.isUpdated = function(id) {
                var file = $scope.files[id];
                return !file.viewed || file.viewed.isBefore(file.modified);
            };

            $scope.hasErrors = function(id) {
                return Object.getOwnPropertyNames($scope.files[id].errors).length !== 0;
            };

            $scope.sendTestEmail = function() {
                if($scope.selected) {
                    $api.sendTestEmail($scope.selected.id, ["gdoerr@gmail.com", "greg@doerr.ws"]).then(function(rsp) {
                        var msg = "";
                        rsp.forEach(function(r) {
                            if(msg.length !== 0)
                                msg += "\n";

                            msg += r.email + " : " + r.status;
                        });
                            $mdToast.show(
                                $mdToast.simple()
                                .textContent(msg)
                                .position("bottom right")
                                .hideDelay(3000)
                            );
                    });
                }
            };

            function selectFirst() {
                var keys = Object.keys($scope.files);
                if(keys.length) {
                    $scope.selected = $scope.files[keys[0]];
                    $scope.selected.viewed = moment();
                }
            }
        }
    ]);

    module.directive("clrIframe", ["$compile", "$timeout",
        function($compile, $timeout) {
            var isIE = (navigator.userAgent.indexOf("MSIE") != -1) || (/rv:11.0/i.test(navigator.userAgent));
            var isEdge = navigator.userAgent.indexOf("Edge") != -1;

            return {
                restrict: "E",
                link: function($scope, $element, $attrs) {
                    $attrs.$observe("src", function(src) {
                        if(!src)
                            clear();
                        else
                            load(src);
                    });

                    function clear() {
                        $element.html("<div style='position: absolute;margin: auto;top: 0;right: 0;bottom: 0;left: 0;width: 60px;height: 60px;'>"
                        + "<md-progress-circular md-mode='indeterminate' md-diameter='96'></md-progress-circular>"
                        + '</div>');
                    }

                    function load(src) {
                        clear();

                        var iFrame = angular.element("<iframe>", {
                            src: src,
                            style: "display: none;"
                        }).appendTo($element);

                        function frameLoaded(e) {
                            $element.find("div").attr("style", "display: none;");
                            iFrame.removeAttr("style");
                        };

                        if(isIE || isEdge) {
                            $timeout(frameLoaded, 1000);
                        } else {
                            iFrame.load(frameLoaded);
                        }

                        $compile($($element).contents())($scope);
                    }
                }
            }
        }
    ]);
})();