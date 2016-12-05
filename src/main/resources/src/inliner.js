/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Greg Doerr
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
(function() {
    "use strict";

    define([ "angular", "angular-local-storage", "moment",
            "./config.html", "./templates.html", "./preview.html"],

        function(angular, storage, moment) {

            var module = angular.module("inliner", [
                storage
            ]);

            module.config(function(localStorageServiceProvider) {
                localStorageServiceProvider.setPrefix("cssinliner");
            });

            module.controller("ConfigCtrl", ["$scope", "$mdDialog", "api", "localStorageService",
                function($scope, $mdDialog, $api, $storage) {
                    var self = this;
                    self.provider = "";
                    self.providers = [];

                    this.emails = $storage.get("emails") || [];
                    this.prefix = $storage.get("prefix") || "";

                    $api.getEmailConfig().then(function(cfg) {
                        self.provider = cfg.current;
                        self.providers = cfg.available;
                    });

                    this.chipChange = function() {
                        $scope.configForm.$setDirty();
                    };

                    this.cancel = function() {
                        $mdDialog.cancel();
                    };

                    this.save = function() {
                        $storage.set("emails", self.emails);
                        $storage.set("prefix", self.prefix);

                        $mdDialog.hide();
                    };
                }
            ]);

            module.service("selectSvc", [
                function() {
                    var callback;

                    this.setPreview = function(p) {
                        if(callback) callback(p);
                    };

                    this.setCallback = function(c) {
                        callback = c;
                    };
                }
            ]);

            module.directive("bldTemplates", ["$timeout", "templates", "selectSvc",
                function($timeout, $templates, $select) {
                    return {
                        templateUrl: "templates.html",
                        controllerAs: "tmpl",
                        controller: function($scope) {
                            var self = this;

                            this.files = $templates.get();
                            this.selected = undefined;

                            this.doSelect = function(id) {
                                self.selected = self.files[id];
                                $select.setPreview(self.selected);
                                self.selected.viewed = self.selected.lastMod;
                            };

                            $scope.$on("template", function(event, id) {
                                if(!self.selected)
                                    self.selected = self.files[id];

                                if(self.selected.id === id) {
                                    $select.setPreview(undefined);

                                    $timeout(function() {
                                        self.selected.viewed = moment();
                                        $select.setPreview(self.selected);
                                    }, 1);
                                } else
                                    self.files[id].viewed = moment(0);
                            });

                            this.isUpdated = function(id) {
                                var file = self.files[id];
                                return file.viewed && file.viewed.isBefore(file.lastMod);
                            };

                            this.hasErrors = function(id) {
                                //return Object.getOwnPropertyNames(self.files[id].errors).length !== 0;
                                return false;
                            };
                        }
                    };
                }
            ]);

            module.directive("bldPreview", ["$mdToast",  "api", "selectSvc", "localStorageService",
                function($mdToast, $api, $select, $storage) {
                    return {
                        templateUrl: "preview.html",
                        controllerAs: "preview",
                        controller: function() {
                            var self = this;

                            this.selected = undefined;

                            $select.setCallback(function(preview) {
                                self.selected = preview;
                            });

                            this.sendTestEmail = function() {
                                var emails = $storage.get("emails") || [];
                                if(self.selected && emails.length > 0) {
                                    $api.sendTestEmail(self.selected.id, emails).then(function(rsp) {
                                        var msg = "";

                                        rsp.forEach(function(r) {
                                            msg += r.email + ": " + r.status + "\n";
                                        });

                                        $mdToast.show(
                                            $mdToast.simple()
                                            .textContent(msg)
                                            .position("bottom right")
                                            .hideDelay(3000)
                                        );
                                    });
                                } else {
                                    $mdToast.show(
                                        $mdToast.simple()
                                        .textContent("No Email Addresses Configured")
                                        .position("bottom right")
                                        .hideDelay(3000)
                                    );
                                }
                            };
                        }
                    };
                }
            ]);

            module.service("templates", ["$rootScope", "api",
                function($root, $api) {
                    var svc = {
                        files: {}
                    };

                    this.get = function() {
                        return svc.files;
                    };

                    $api.getFiles().then(function(files) {
                        files.sort(function(a, b) {
                            return a.name.localeCompare(b.name);
                        });
                        files.forEach(function(file) {
                            svc.files[file.id] = file;
                            file.created = moment(file.created);
                            file.modified = moment(file.modified);

                            file.lastMod = getNewest(file);
                        });
                    });

                    $root.$on("file", function(event, file) {
                        if(!svc.files[file.id]) {
                            svc.files[file.id] = file;
                            file.created = moment(file.created);
                            file.modified = moment(file.modified);
                        } else {
                            var f = svc.files[file.id];
                            angular.extend(f, file);
                            f.created = moment(file.created);
                            f.modified = moment(file.modified);
                        }

                        f.lastMod = getNewest(file);

                        $root.$broadcast("template", file.id);
                    });

                    function getNewest(file) {
                        if(file.dependencies.length === 0)
                            return file.modified;

                        file.dependencies.sort(function(a, b) {
                            return b.path.modified - a.path.modified;
                        });

                        var newest = file.modified.valueOf() > file.dependencies[0].path.modified
                            ? file.modified
                            : moment(file.dependencies[0].path.modified);

                        if(!file.data)
                            return newest;

                        return newest.valueOf() > file.data.modified
                            ? newest
                            : moment(file.data.modified);
                    }
                }
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

                    this.getEmailConfig = function() {
                        return $http.get(httpUrl + "/api/config/email/providers").then(function(rsp) {
                            return rsp.data;
                        });
                    };
                }
            ]);

            return module;
        }
    );
})();