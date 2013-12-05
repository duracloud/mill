/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

/**
 * @author Daniel Bernstein
 */

App.SessionsNewController = Ember.ObjectController.extend({

	attemptedTransition : null,
	content: {},
	
	
	actions: {
		login: function(success, failure){
			var self = this;
			var router = this.get('target');
			var data = this.getProperties('username', 'password', 'subdomain','spacePrefix');
			App.loginOptions.spacePrefix = data.spacePrefix;
			var attemptedTrans = this.get('attemptedTransition');

			App.authManager.authenticate(data.username, data.password,
					data.subdomain).then(function() {
				success();
				if (attemptedTrans) {
					attemptedTrans.retry();
					self.set('attemptedTransition', null);
				} else {
					router.transitionTo('index');
				}
			}, failure);
		}
	}
});