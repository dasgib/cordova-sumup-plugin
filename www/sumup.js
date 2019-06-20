var Sumup = function() {};

Sumup.prototype.pay = function(success, failure, amount, currencycode, title, email, tel) {
  cordova.exec(success, failure, 'Sumup', 'pay', [amount, currencycode, title, email, tel]);
};

Sumup.prototype.prepareForCheckout = function(success, failure) {
  cordova.exec(success, failure, 'Sumup', 'prepareForCheckout', []);
};

Sumup.prototype.login = function(success, failure) {
  cordova.exec(success, failure, 'Sumup', 'login', []);
};

Sumup.prototype.logout = function(success, failure) {
  cordova.exec(success, failure, 'Sumup', 'logout', []);
};

Sumup.prototype.isLoggedIn = function(success, failure) {
  cordova.exec(success, failure, 'Sumup', 'isLoggedIn', []);
};

Sumup.prototype.settings = function(success, failure) {
  cordova.exec(success, failure, 'Sumup', 'settings', []);
};

if (! window.plugins) {
  window.plugins = {};
}

if (! window.plugins.sumup) {
  window.plugins.sumup = new Sumup();
}

if (module.exports) {
  module.exports = Sumup;
}
