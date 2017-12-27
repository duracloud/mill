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

App.PolicyController = Ember.ObjectController.extend({
 

    srcStore: null,
    destStore: null,

	_getAllSpaces: function(){
		return App.DuraStoreClient.getSpacesBySubdomain(this.get('model.id'));
    },
    
    
	availableSpaces: function(){
	   var allSpaces = $.xml2json(this._getAllSpaces().responseText);
       
           if(!allSpaces.space.map) { //just one space - not an array
               //create an array
               allSpaces.space = [allSpaces.space];
           }
	   
	   allSpaces = allSpaces.space.map(function(element){
		   return element.id.trim();
	   });
	   
	   var configuredSpaces = this.get('model.spaces')
	   							  .map(function(element){
									   return element.get('spaceId');
								   });
	   
	   var filtered = $.grep(allSpaces, function(element){
		   return !($.inArray(element, configuredSpaces) > -1);
	   });
	   
	   return filtered;
	   
   }.property('model.spaces'),
   
   storePolicies: function(){
       return this.get('defaultPolicies');
   }.property('model.storePolicies'),
   
   actions: {
	   addSpace: function(spaceId){
		   var that = this;
		   var id = App.generateUUID();
		   this.store.push('space',{
			    id: id,
			    spaceId : spaceId,
				storePolicies : []
		   });

		   var policy = that.get('model');
		   this.store.find('space', id).then(function(space){
 			    policy.get('spaces').pushObject(space);	
			    policy.save().then(function() {
					that.get('target').transitionTo('space', spaceId);
				}, function(text) {
					$.bootstrapGrowl("failed to save policy:" + text, {
						type : 'danger',
						align : 'center'
					});
				});
		    });

	   },
	   
		deleteSpace: function(space){
			var that = this;
			var policy = this.get('model');
			policy.get('spaces').removeObject(space);
			space.deleteRecord();
			this.get('target').transitionTo('policy');
			policy.save().then(function() {
				console.log("deleted space " + space + " in account "
						+ policy.id);
			}, function() {
				alert('failed to save policy')
			});
			
		},
		
  	    addStorePolicy: function(srcStore,destStore){
		   var that = this;
		   console.log("addStore clicked: srcStoreId=" + srcStore.id + ", destStoreId="+destStore.id);

		   if(srcStore.id == destStore.id){
			   alert("The source and destination cannot be identical.");
			   return;
		   }
		   
		   var policy = that.get('model');

		   var storePolicies = policy.get('defaultPolicies');

		   //check for duplications
		   var duplicate = false;
		   storePolicies.forEach(function(element){
			   if(element.srcStoreId == srcStore.id && element.destStoreId == destStore.id){
				   duplicate = true;
			   }
		   });
		   
		   if(duplicate){
			   alert("A default policy for the specified source and destination already exists.");
			   return;
		   }

		   var record = this.store.push('storePolicy', {
				id : App.generateUUID(),
				source: srcStore,
				destination: destStore,
			});
		   
		   storePolicies.pushObject(record);
		   policy.save(function(){
			   console.log('saved ' + record  +' into ' + policy.id)
		   }, function(text){ 
			   alert("failed to save default store policy :" + text);
		   });
	    },
	    
 	   deleteStorePolicy: function(storePolicy){
		   var policy = this.get('model');

		   policy.get('defaultPolicies').removeObject(storePolicy);
		   storePolicy.deleteRecord();

		   policy.save(function(){
			   console.log('saved policy: ' + policy.id)
		   }, function(){
			   alert('failed to delete policy');
		   });
	   },
		
   }
});




