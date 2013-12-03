App.generateUUID = function(){
	function s4() {
		  return Math.floor((1 + Math.random()) * 0x10000)
		             .toString(16)
		             .substring(1);
	}

	return function(){
	  return s4() + s4() +  s4() +  s4() + s4() + s4() + s4() + s4();
	};
	
}();

var BaseAdapter = DS.Adapter.extend({
	
	_savePolicy: function(subdomain, policy){
		return App.DuraStoreClient.savePolicy(subdomain,policy);
	},
	
	_serializePolicy: function(policy){
		var serialized = {};
		var storePoliciesMap = {};
			
		policy.get('spaces').forEach(function(space){
			var storePolicies = []; 
			storePoliciesMap[space.get('spaceId')] = storePolicies;
			space.get('storePolicies').forEach(function(storePolicy){
				storePolicies.push({
					srcStoreId: storePolicy.get('source').get('id'),
					destStoreId: storePolicy.get('destination').get('id'),
				});
				
			});
		});
		
		serialized.spaceDuplicationStorePolicies = storePoliciesMap;
		
		return serialized;
	},
	
	generateIdForRecord: function(store, record) {
	  return App.generateUUID();
	},
	
	_deserializePolicy: function(id, duplicationPolicy, store){
		var that = this;
		var deserialized = {id: id, spaces: []};
		var policies = duplicationPolicy.spaceDuplicationStorePolicies;
		if(policies){
			for(spaceId in policies){
				var spaceKey;
				spaceKey = this.generateIdForRecord();
				var emberSpace = {
				         id: spaceKey,
				         spaceId : spaceId,
				         storePolicies: []
				};

				$(policies[spaceId]).each(function(i,storePolicy){
					var storePolicyKey = that.generateIdForRecord();
					var emberStorePolicy = {
					   id: storePolicyKey,
					   source: storePolicy.srcStoreId,
					   destination: storePolicy.destStoreId,
					};

					emberSpace.storePolicies.push(storePolicyKey);
					store.push(App.StorePolicy, emberStorePolicy);
				});

				deserialized.spaces.push(spaceKey);
				store.push(App.Space, 
							emberSpace);
			}
		}
		
		return deserialized;
	},	
});



App.AccountAdapter = BaseAdapter.extend({

	find: function(store, type, id) {
		App.DuraStoreClient.checkIfSubdomainExists(id)
			.then(function(){
				store.push(type, {id: id});
			}, function(jqxhr){
							alert("Subdomain " + id 
									+ " does not exist.");
			});
	},


	findAll: function(store, type, since) {

		return App.DuraStoreClient.listAccounts().then(function(result){
			result = $.map(result, function(value, i){
				return { id: value};
			});
			return result;
		});
	},


	
	createRecord:function (store, type, record){
		var that = this;

		return new Ember.RSVP.Promise(function(resolve, reject){
				var serialized;
				
				that.findAll(store,type).then(function(accounts){
					serialized = $.map(accounts, function(value){
						return value.id;
					});
							
					serialized.push(record.id);
					//add a related policy but empty policy
					return that._savePolicy(record.id, {});
				}).then(function(){
					return App.DuraStoreClient.saveAccountsList(serialized);		
				}).then(function(){
					console.log("successfully created remote record for subdomain " + record.id);
					 resolve(record);
				},  function(jqxhr, textStatus, error){
						alert("failed to create subdomain " + record.id +"; error = " + error + "; status=" + jqxhr.status);
						reject(error);
	 		    });
		});
	},
	
	deleteRecord: function(store,type,record){
		var that = this;

		return new Ember.RSVP.Promise(function(resolve, reject){
				that.findAll(store,type).then(function(accounts){
					serialized = $.map(accounts, function(value){
						return value.id;
					}).filter(function(value){
						return record.id != value;
					});
					return serialized;
				}).then(function(serialized){
					return App.DuraStoreClient.saveAccountsList(serialized);
				}).then(function(){
					return App.DuraStoreClient.deletePolicy(record.id);
				}).then(function(){
					console.log("successfully deleted remote record for subdomain " + record.id);
					  resolve(record);
				},  function(jqxhr, textStatus, error){
						alert("failed to delete subdomain " + record.id +"; error = " + error + "; status=" + jqxhr.status);
						reject(error);
	 		    });
		});
	},
});

App.PolicyAdapter = BaseAdapter.extend({
	loadStorageProviders: function(id, store){
  	    return App.DuraStoreClient.listStoreProvidersBySubdomain(id).then(function(xml){
  	  		var storageProviders = $.xml2json(xml);
  	  		providers = storageProviders.storageAcct;
  	  		$(providers).map(function(i, sp){
  	  	  		store.push('storageProvider', sp);
  	  	  		return store.find('storageProvider',sp.id);
  	  		});
  	  		return providers;
  	  	});
	},
	
	
	find: function(store, type, id) {
		var that = this;
		storageProviders = null
		return App.DuraStoreClient.checkIfSubdomainExists(id)
		.then(function(){
			return that.loadStorageProviders(id,store);
		}).then(function(providers){
			storageProviders = providers;
			return App.DuraStoreClient.getPolicy(id);
		}).then(function(result){
			policy = that._deserializePolicy(id, result, store);
			policy.storageProviders = [];
			$(storageProviders).map(function(i,sp){
				policy.storageProviders.push(sp.id);
			});
			return policy;	
		}, function(jqxhr){
							alert("Subdomain " + id
									+ " could not be verified: http code: "
									+ jqxhr.status);
							return {spaceId: id, id:id};
		});
	},
	
	updateRecord:function (store, type, record){
		var that = this;
		
		var serialized = this._serializePolicy(record);
		
		return that._savePolicy(record.id, serialized);
	},
});
