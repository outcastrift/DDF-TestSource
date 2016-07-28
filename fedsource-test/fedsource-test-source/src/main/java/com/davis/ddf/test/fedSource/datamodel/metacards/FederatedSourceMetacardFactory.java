package com.davis.ddf.test.fedSource.datamodel.metacards;

import org.json.JSONObject;

import ddf.catalog.data.impl.MetacardImpl;

/**
 * Created by hduser on 9/21/15.
 */
public class FederatedSourceMetacardFactory implements MetacardFactory {
    /**
     * Called by Spring.
     */
    public void init(){
    }

    /**
     * Called by Spring.
     */
    public void destroy(){
    }

    /**
     * Sweet metacard metacard.
     *
     * @return the metacard
     */
    public MetacardImpl sweetMetacard() {
        return new MetacardImpl();
    }

    /**
     * {@inheritDoc}
     *
     * @return the metacard
     */
    @Override
    public MetacardImpl create() {
        UniversalFederatedSourceMetacard metacard = new UniversalFederatedSourceMetacard();
        MetacardUtil.setDefaults(metacard);
        return metacard;
    }

    /**
     * {@inheritDoc}
     *
     * @param json the json
     * @return the metacard
     */
    @Override
    public MetacardImpl createFromJson(String json) {
        MetacardImpl metacard = create();
        metacard = MetacardUtil.populate( metacard, json );
        return metacard;
    }

    /**
     * {@inheritDoc}
     *
     * @param json the json
     * @return the metacard
     */
    @Override
    public MetacardImpl createFromJson( JSONObject json ) {
        MetacardImpl metacard = create();
        metacard = MetacardUtil.populate( metacard, json );
        return metacard;
    }

    /**
     * To string string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return getClass().toString();
    }

    /**
     * Test.
     */
    public void test(){
        System.out.println( "We called test!" );
    }
}
