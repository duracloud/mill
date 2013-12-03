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

  attemptedTransition: null,

  loginUser: function() {
    var self = this;
    var router = this.get('target');
    var data = this.getProperties('username', 'password');
    var attemptedTrans = this.get('attemptedTransition');

      App.AuthManager.authenticate(data.username, password)
		 .then(function(){
			 alert("success");
		      if (attemptedTrans) {
		          attemptedTrans.retry();
		          self.set('attemptedTransition', null);
		        } else {
		          router.transitionTo('index');
		        }
		 }, function(reason){
			 alert("failure:  "+ reason);
		 });
  }
});