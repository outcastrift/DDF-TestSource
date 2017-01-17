package com.davis.ddf.test.fedSource.datamodel.metacards;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;

/**
 * This software was created for the Open Architecture Distributed Common Ground System
 * Modernization All rights to this software belong to  appropriate licenses and restrictions apply.
 * Created by Samuel Davis on 3/21/16.
 */
public class UniversalFederatedSourceMetacardType implements QualifiedMetacardType {

    /**
     * The constant SUMMARY.
     */
    public static final String SUMMARY="Summary";
    /**
     * The constant LAT.
     */
    public static final String LAT="Latitude";
    /**
     * The constant LON.
     */
    public static final String LON="Longitude";
    /**
     * The constant CLASSIFICATION.
     */
    public static final String CLASSIFICATION="Classification";
    /**
     * The constant NAME.
     */
    public static final String NAME="Sigact";
    /**
     * The constant NAMESPACE.
     */
    public static final String NAMESPACE="";
    /**
     * The constant UNIT.
     */
    public static final String UNIT= "Unit";
    /**
     * The constant EVENT_TYPE.
     */
    public static final String EVENT_TYPE="EventType";
    /**
     * The constant REPORT_LINK.
     */
    public static final String REPORT_LINK="ReportLink";
    /**
     * The Descriptors.
     */
    private Set<AttributeDescriptor> descriptors;

    /**
     * Instantiates a new Test federated source metacard type.
     */
    public UniversalFederatedSourceMetacardType(){
        descriptors = new HashSet<AttributeDescriptor>();
        initAttributeDescriptors();
    }

    /**
     * Init attribute descriptors.
     */
    private void initAttributeDescriptors(){
        Set<AttributeDescriptor> baseDescriptors =  BasicTypes.BASIC_METACARD.getAttributeDescriptors();
        for (AttributeDescriptor descriptor : baseDescriptors) {
           descriptors.add(new AttributeDescriptorImpl(descriptor.getName(),descriptor.isIndexed(),descriptor.isStored(),descriptor.isTokenized(),descriptor.isMultiValued(),descriptor.getType()));
        }
        AttributeDescriptor summary = new AttributeDescriptorImpl(SUMMARY,true,true,true,false, BasicTypes.STRING_TYPE);
        descriptors.add(summary);
        AttributeDescriptor lat = new AttributeDescriptorImpl(LAT,true,true,true,false, BasicTypes.DOUBLE_TYPE);
        descriptors.add(lat);
        AttributeDescriptor lon = new AttributeDescriptorImpl(LON,true,true,true,false, BasicTypes.DOUBLE_TYPE);
        descriptors.add(lon);
        AttributeDescriptor classification = new AttributeDescriptorImpl(CLASSIFICATION,true,true,true,false, BasicTypes.STRING_TYPE);
        descriptors.add(classification);
        AttributeDescriptor unit = new AttributeDescriptorImpl(UNIT,true,true,true,false, BasicTypes.STRING_TYPE);

        descriptors.add(unit);
        AttributeDescriptor eventType = new AttributeDescriptorImpl(EVENT_TYPE,true,true,true,false, BasicTypes.STRING_TYPE);
        descriptors.add(eventType);
        AttributeDescriptor reportLink = new AttributeDescriptorImpl(REPORT_LINK,true,true,true,false, BasicTypes.STRING_TYPE);
        descriptors.add(reportLink);
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Get attribute descriptors set.
     *
     * @return the set
     */
    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors(){
        return Collections.unmodifiableSet(descriptors);

    }

    /**
     * Gets attribute descriptor.
     *
     * @param s the s
     * @return the attribute descriptor
     */
    @Override
    public AttributeDescriptor getAttributeDescriptor(String s) {
        if (s == null) {
            return null;
        }

        for (AttributeDescriptor descriptor : descriptors) {
            if (s.equals(descriptor.getName())) {
                return descriptor;
            }
        }
        return null;
    }

    /**
     * Gets namespace.
     *
     * @return the namespace
     */
    @Override
    public String getNamespace() {
        return NAMESPACE;
    }
}
