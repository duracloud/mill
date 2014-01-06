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
 

	_getAllSpaces: function(){
		return App.DuraStoreClient.getSpacesBySubdomain(this.get('model.id'));
    },
    
    
	availableSpaces: function(){
	   var allSpaces = $.xml2json(this._getAllSpaces().responseText);
	   
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
   }
});




