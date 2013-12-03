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
App.AccountsController = Ember.ArrayController.extend({
	actions : {
		createAccount : function() {
			var that = this;
			var id = this.get('newId').trim();
			if (!id) {
				return;
			}

			// Create the new Account model
			if (!this.store.hasRecordForId(id)) {
				var account = this.store.createRecord('account', {
					id : id,
				});

				// Clear the "New Account" text field
				this.set('newId', '');

				$.bootstrapGrowl('Opening policy...', {type:'warning', align: 'center'});

				// Save the new model
				account.save().then(function() {
					console.log('account saved');
					that.get('target').transitionTo('policy', id);
				});




			} else {
				$.bootstrapGrowl(id + " already exists", {type:'danger'});
			}
		},

		deleteAccount : function(account) {
			var id = account.id;
			account.deleteRecord();
			account.save();
			this.get('target').transitionTo('accounts');

		},

	},
});
