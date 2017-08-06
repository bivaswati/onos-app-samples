/**
 * Copyright 2016 WIPRO Technologies Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* It defines the view controller, and provides callback functions to drive the view.
   File Name : qosui.js
   Author    : Bivas
   Date      : 2nd-April-2016
*/

(function () {
    'use strict';

    // injected refs
    var $log, $scope, fs, wss, ps;

    // constants
    var ACTIVE = 'ACTIVE',
        intentMgmtReq = 'intentManagementRequest',
        dialogId = 'app-dialog',
        dialogOpts = {
                    edge: 'right'
                },
        propOrder = ['sid', 'smac', 'cmac', 'qid', 'id', 'rid'],
        friendlyProps = ['SID', 'SMAC', 'CMAC', 'QID', 'FID', 'RID'];


    function addProp(tbody, index, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt) {
            tr.append('td').attr('class', cls).html(txt);
        }
        addCell('label', friendlyProps[index] + ' :');
        addCell('value', value);
    }
    angular.module('ovQosui', [])
        .controller('OvQosuiCtrl',
        ['$log', '$scope', 'TableBuilderService',
            'FnService', 'WebSocketService', 'DialogService',

            function (_$log_, _$scope_, tbs, _fs_, _wss_, ds) {
                $log = _$log_;
                $scope = _$scope_;
                fs = _fs_;
                wss = _wss_;

                $scope.ctrlBtnState = {};
                $scope.uninstallTip = 'Delete Intent Data Row';
                var handlers = {};

                // custom selection callback
                function selCb($event, row) {
                    $scope.ctrlBtnState.selection = !!$scope.selId;
                    refreshCtrls();
                    ds.closeDialog();
                    $log.debug('Got a click on:', row);
                }
                function refreshCtrls() {
                    var row, rowIdx;
                    if ($scope.ctrlBtnState.selection) {
                        rowIdx = fs.find($scope.selId, $scope.tableData);
                        row = rowIdx >= 0 ? $scope.tableData[rowIdx] : null;
                        $scope.ctrlBtnState.active = row && row.state === ACTIVE;
                    } else {
                        $scope.ctrlBtnState.installed = false;
                        $scope.ctrlBtnState.active = false;
                    }
                }

                // TableBuilderService creating a table for us
                tbs.buildTable({
                    scope: $scope,
                    tag: 'qosui',
                    selCb: selCb
                });

                function confirmAction(action) {
                    var itemId = $scope.selId;
                    function dOk() {
                        $log.debug('Initiating', action, 'of', itemId);
                        wss.sendEvent(intentMgmtReq, {
                            action: action,
                            name: itemId
                        });
                    }

                    function dCancel() {
                        $log.debug('Canceling', action, 'of', itemId);
                    }

                    ds.openDialog(dialogId, dialogOpts)
                        .setTitle('Are You Sure To : ')
                        .addContent(createConfirmationText(action, itemId))
                        .addOk(dOk)
                        .addCancel(dCancel)
                }

                function createConfirmationText(action, itemId) {
                    var content = ds.createDiv();
                    content.append('p').text(fs.cap(action));
                    return content;
                }


                $scope.deleteIntent = function (action) {
                    if ($scope.ctrlBtnState.selection) {
                        confirmAction(action);
                    }
                };
                // cleanup
                $scope.$on('$destroy', function () {
                    wss.unbindHandlers(handlers);
                });

                $log.log('OvQosuiCtrl has been created');
    }
        ]
            )
}());