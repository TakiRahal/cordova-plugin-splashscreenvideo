var exec = require('cordova/exec');

var splashscreenvideo = {
    show:function() {
        exec(null, null, "SplashScreenVideo", "show", []);
    },
    hide:function() {
        exec(null, null, "SplashScreenVideo", "hide", []);
    }
};

module.exports = splashscreenvideo;