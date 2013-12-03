/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */

/**
* DuraCloud Duplication Policy Editor 
* @author Daniel Bernstein 
*/


//creates the ember application
window.App = Ember.Application.create();




App.Credentials = Ember.Object.create({
	subdomain: null,
});



App.AuthManager = Ember.Object.extend({
	_token: null,
	_authenticated: false,
	
	isAuthenticated: function(){
		return this._authenticated;
	},
	
	authenticate: function(username, password,subdomain){
		var that = this;
		return new Ember.RSVP.Promise(function(resolve, reject){
			that._token =  btoa(username + ":" + password);
			App.Credentials.subdomain = subdomain;
			
			App.DuraStoreClient.listAccounts().then(function(){
				that._authenticated = true;
				resolve();
			}, reject);
		});
	},
	
	getToken: function(){
		return this._token;
	}
});

App.authManager = App.AuthManager.create();


App.DuraStoreClient = Ember.Object.create({
	
	_formatDuraStoreUrl:function(subdomain){
		return "https://"+subdomain + ".duracloud.org/durastore";
	},

	_formatDuplicationPolicyRepoUrl:function(){
		return this._formatDuraStoreUrl(App.Credentials.subdomain) + "/duplication-policy-repo/";
	},

	_formatDuplicationPolicyUrl:function(accountSubdomain){
		return this._formatDuplicationPolicyRepoUrl() 
				+ accountSubdomain + "-duplication-policy.json";
	},

	_formatDuplicationPoliciesUrl:function(){
		return  this._formatDuplicationPolicyRepoUrl() + "duplication-accounts.json";
	},

	_createOptions: function(options){

		var defaults = {
				beforeSend: App.beforeSend(),
				dataType : "json",
				mimeType : "application/json"
		};
	    
	    if(options){
	    	return $.extend(defaults,options);
	    }else{
	    	return defaults;
	    }
	},
	
	//this method expects an array of strings
	saveAccountsList:function(accounts){
		var url = this._formatDuplicationPoliciesUrl();
		//update the accounts file with the new id.
		var options = this._createOptions({
	    	url: url,
			type : 'PUT',
			data : JSON.stringify(accounts),
			dataType: "text"
		});

	    return $.ajax(options);		
	},

	savePolicy: function(subdomain, policy){
		var url =  this._formatDuplicationPolicyUrl(subdomain);
		var options = this._createOptions({
								url: url , 
								type:'PUT', 
								data: JSON.stringify(policy), 
								dataType:"text"});
		return $.ajax(options);
	},
	
	getPolicy: function(subdomain){
		var that = this;

		return $.ajax(this._createOptions({
					url:this._formatDuplicationPolicyUrl(subdomain)
				}));
	},
	
	deletePolicy: function(subdomain){
		var url = this._formatDuplicationPolicyUrl(subdomain);
		
		var options = this._createOptions({
			url: url,
			type : 'DELETE',
			dataType: "text"
		});

	    return $.ajax(options);	
	},

	
	listStoreProvidersBySubdomain: function(subdomain){
		var url = this._formatDuraStoreUrl(subdomain) + "/stores";
		
		return $.ajax({
  	  		url: url,
  	  		dataType: 'xml',
  	  		beforeSend: App.beforeSend()
  	  	});		
	},
	
	getSpacesBySubdomain: function(subdomain){
		var url = this._formatDuraStoreUrl(subdomain) + "/spaces"
		 
		return $.ajax({
			async:false,
			url: url,
			dataType: 'xml',
			beforeSend: App.beforeSend()
		});
	},

	
	checkIfSubdomainExists:function(subdomain){
		var url =  this._formatDuraStoreUrl(subdomain);

		return $.ajax(this._createOptions({
					url : url + "/spaces",
					dataType : "xml"
				})).then(function() {
					return true
				});
	},

	listAccounts: function(){
		var url = this._formatDuplicationPoliciesUrl();
		return $.ajax(this._createOptions({url: url}));		
	},
});

App.beforeSend = function(){
	return function(xhr){
		xhr.setRequestHeader("Authorization", "Basic " + App.authManager.getToken()); 
	};
};


App.SessionsNewController = Ember.ObjectController.extend({

	attemptedTransition : null,
	content: {},
	
	
	actions: {
		login: function(success, failure){
			var self = this;
			var router = this.get('target');
			var data = this.getProperties('username', 'password', 'subdomain');
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

App.LoginButtonView = Ember.View.extend({
	  tagName: 'button',
	  classNames: ['btn', 'btn-default', 'has-spinner'],
	  click: function(event){
		$(event.target).toggleClass('active'); 
		
		this.get('controller').send('login', function(){
			//do nothing on success - rerouting is handled by controller
		}, function(){
			$(event.target).toggleClass('active'); 
			$.bootstrapGrowl(
				'The username/password combination is not valid.',
				{
					align : 'center',
					type  : 'danger',
				});
		});
		return false;
	  },
	  
});

