App.StorageProvider = DS.Model.extend({
	storageProviderType: DS.attr(),
});

App.StorePolicy = DS.Model.extend({
	source: DS.belongsTo(App.StorageProvider),
	destination: DS.belongsTo(App.StorageProvider),
});

App.Space = DS.Model.extend({
	spaceId: DS.attr(),
	storePolicies: DS.hasMany(App.StorePolicy),
});


App.Policy = DS.Model.extend({
	spaces: DS.hasMany(App.Space),
	storageProviders: DS.hasMany(App.StorageProviders)
});

App.Account = DS.Model.extend({
	
});
