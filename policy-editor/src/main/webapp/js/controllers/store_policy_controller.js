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
App.SpaceController = Ember.ObjectController.extend({

    srcStore: null,
    destStore: null,
    needs : [ 'policy' ], // indicates that space controller has access to
   						  // policy controller via the get() method.
    storageProviders: function(){
      return this.get('controllers.policy').get('model.storageProviders');
    }.property('model.storageProviders'),
  
    spaceId: function(){
        return this.get('controllers.policy').get('model.spaceId');
    }.property('model.spaceId'),

    watchIgnored: function(){
		console.debug("ignored changed! value=" + this.get("model.ignored"));
		this.save();

    }.observes('model.ignored'),

    save: function(){
        this.get('controllers.policy').get('model').save(function(){
            alert("saved policy!");
        }, function(){
            alert('failed to save policy');
        });
    },

    actions: {
 	   deleteStorePolicy: function(storePolicy){
		   this.get("model").get('storePolicies').removeObject(storePolicy);
		   storePolicy.deleteRecord();

		   
		   this.get('controllers.policy').get('model').save(null, function(){
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
		   
		   var storePolicies = that.get('model').get('storePolicies');

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
		   this.save();
	   },
     }
});



