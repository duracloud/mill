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

App.Router.map(function () {

  this.resource('accounts', { path: '/accounts' }, function(){
     this.resource('policy', {path: '/:account_id'}, function(){
       this.resource('space', {path: '/:space_id'});
     });
  });
  
  this.resource('sessions.new', {path: '/login'});

});

App.AuthenticatedRoute = Ember.Route.extend({
	  beforeModel: function(transition) {
	    if (!App.authManager.isAuthenticated()) {
	      this.redirectToLogin(transition);
	    }
	  },

	  // Redirect to the login page and store the current transition so we can
	  // run it again after login
	  redirectToLogin: function(transition) {
	    var sessionNewController = this.controllerFor('sessions.new');
	    sessionNewController.set('attemptedTransition', transition);
	    this.transitionTo('sessions.new');
	  },

	  actions: {
	    error: function(reason, transition) {
	      //this.redirectToLogin(transition);
	    }
	  }
	});

App.SessionsNewRoute = Ember.Route.extend({
	renderTemplate: function(){
		this.render({outlet:'left'});  
	},
});

App.IndexRoute = Ember.Route.extend({
	renderTemplate: function(){
		this.render({outlet:'left'});  
	},
});

App.LoadingRoute = Ember.Route.extend({
	renderTemplate: function(){
		this._super();
		var flickerAPI = "http://api.flickr.com/services/feeds/photos_public.gne?jsoncallback=?";
		  $.getJSON( flickerAPI, {
		    tags: "twins",
		    tagmode: "any",
		    format: "json"
		  }).done(function( data ) {
			  var div = $("#image"); 
			  div.find("img").remove();
			  var items = data.items;
			  var rand = Math.floor((Math.random()*items.length));
		      $( "<img>" ).attr( "src", items[rand].media.m ).appendTo( div );
		      div.fadeIn();
	      });
	},
});

var BaseRoute = App.AuthenticatedRoute.extend({
});




App.AccountsRoute = BaseRoute.extend({
  model: function () {
    return this.store.find('account');
  },

	renderTemplate: function(){
		this.render({outlet:'left'});  
	},

});


App.LoaderModal = function(){
	
	var timer = null;
	
	return {
		show:function(){
			timer = setTimeout(function(){
				$("#loading-modal").modal("show");
			},200);
		},
	
		hide: function(){
			if(timer){
				clearTimeout(timer);
				timer = null;
				$("#loading-modal").modal("hide");

			}
		}
	}
}();

App.PolicyRoute = BaseRoute.extend({
	
  
  
  beforeModel: function(){
	console.log("before model");
  },

  afterModel: function(){
	console.log("after model");
  },

  renderTemplate: function(){
		this.render({outlet:'center'});  
  },

  
  model: function(params) {
	  return this.store.find('policy', params.account_id);
  },
  
  serialize: function(model) {
    return {
		account_id : model.id
	};
  },
  
  
  actions: {

  },
});

App.PolicyView = Ember.View.extend({
	templateName:"policy",
	didInsertElement:function(){
	}
});

App.FadingView = Ember.View.extend({
	didInsertElement:function(){
		 this.$().hide().fadeIn(1000);
	}
});

App.SpaceItemView = App.FadingView.extend({
});

App.StorePolicyView = App.FadingView.extend({
});

App.SpaceRoute = BaseRoute.extend({
  model: function(params) {
	  var spaces = this.modelFor('policy').get('spaces');
	  var space = null;
	  
	  spaces.forEach(function(s){
		  if(s.get('spaceId') == params.space_id){
			  space = s;
		  }
	  });
	  
	  console.log("model = " + space);
      return space;
  },
  

  renderTemplate: function(){
		this.render({outlet:'right'});  
  },
  
  serialize : function(model) {
	return {
		space_id : model.spaceId,
	};
  },
  
  actions: {
	  
	   deleteStorePolicy: function(storePolicy){
		   this.modelFor('space').get('storePolicies').removeObject(storePolicy);
		   storePolicy.deleteRecord();

		   this.modelFor('policy').save(null, function(){
			   alert('failed to delete policy');
		   });
	   },
	   
	   addStorePolicy: function(srcStore,destStore){
		   var that = this;
		   console.log("addStore clicked: srcStoreId=" + srcStore.id + ", destStoreId="+destStore.id);

		   if(srcStore.id == destStore.id){
			   alert("The source and destination cannot be identical.");
			   return;
		   }
		   
		   var storePolicies = this.modelFor('space').get('storePolicies');

		   //check for duplications
		   var duplicate = false;
		   storePolicies.forEach(function(element){
			   if(element.srcStoreId == srcStore.id && element.destStoreId == destStore.id){
				   duplicate = true;
			   }
		   });
		   
		   if(duplicate){
			   alert("A policy for the specified source and destination already exists.");
			   return;
		   }

		   var record = this.store.push('storePolicy', {
				id : App.generateUUID(),
				source: srcStore,
				destination: destStore,
			});
		   
		   storePolicies.pushObject(record);
		   var policy = this.modelFor('policy');
		   policy.save(function(){
			   console.log('saved ' + record  +' into ' + policy.id)
		   }, function(text){ 
			   alert("failed to save store policy :" + text);
		   });
	   }
  },
  
});




