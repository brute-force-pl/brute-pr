log('brute-pr-hook.js');

define('brute-pr-refresh', [
  'jquery',
  'bitbucket/util/events',
  'bitbucket/util/state',
  'bitbucket/internal/feature/pull-request/can-merge',
  'exports'
], function ($, events, state, pr, exports) {

  exports.init = function () {
    log('PR refresh hook initializing');
    events.on("bitbucket.internal.widget.approve-button.added", function () {
      log('Initiating merge check');
      pr();
    });
  };
});

AJS.$(document).ready(function ($) {
  return function () {
    log('Reguiring PR refresh hook');
    require("brute-pr-refresh").init();
  };
}(AJS.$));

function log() {
  var args = [].slice.apply(arguments);
  args.unshift('[BRUTE PR]:');
  AJS.log.apply(this, args);
}