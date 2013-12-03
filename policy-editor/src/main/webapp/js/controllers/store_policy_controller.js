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
    }.property('model.storageProviders')
  
});



