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

    define(["angular", "./common.less", "./toolbar.html"], function(angular) {
        var module = angular.module('common', []);

        module.directive("bldToolbar", ["$mdDialog",
            function($mdDialog) {
                return {
                    templateUrl: "toolbar.html",
                    controller: function() {
                        this.configure = function() {
                            $mdDialog.show({
                                templateUrl: "config.html",
                                parent: angular.element(document.body)
                            });
                        };
                    },
                    controllerAs: "toolbar"
                };
            }
        ]);

        module.directive("bldIframe", ["$compile", "$timeout",
            function($compile, $timeout) {
                var isIE = (navigator.userAgent.indexOf("MSIE") !== -1) || (/rv:11.0/i.test(navigator.userAgent));
                var isEdge = navigator.userAgent.indexOf("Edge") !== -1;

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
                                iFrame.on('load', frameLoaded);
                            }

                            $compile($($element).contents())($scope);
                        }
                    }
                };
            }
        ]);

        return module;
    });
})();

