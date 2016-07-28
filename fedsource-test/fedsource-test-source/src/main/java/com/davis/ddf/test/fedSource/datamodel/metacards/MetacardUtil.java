package com.davis.ddf.test.fedSource.datamodel.metacards;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.UUID;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

/**
 * Metacard Utility.
 *
 * @author Bob Harrod
 * @version 1.0
 */
public class MetacardUtil {
  /**
   * The constant logger.
   */
  private static final Logger logger = LoggerFactory.getLogger( MetacardUtil.class );
  /**
   * The constant dateFormatPattern.
   */
  private static final String dateFormatPattern = "MM/dd/yyyy HH:mm:ss";

  /**
   * Populate metacard.
   *
   * @param metacard     the metacard
   * @param metacardJson the metacard json
   * @return metacard
   */
  static public MetacardImpl populate( MetacardImpl metacard, String metacardJson ){
    JSONTokener tokenizer = new JSONTokener( metacardJson );
    JSONObject jsonObj = new JSONObject( tokenizer );
    String key = null;
    
    Iterator<String> keyItr = jsonObj.keys();
    
    while( keyItr.hasNext() ){
      key = keyItr.next();
      setAttributeValue( metacard, key, jsonObj.get( key ) );
    }
    
    if( metacard.getMetadata() == null || metacard.getMetadata().trim().length() == 0 ){
      metacard.setMetadata( "<metadata></metadata>");
    }
 
    return metacard;
  }

  /**
   * Populate metacard.
   *
   * @param metacard     the metacard
   * @param metacardJson the metacard json
   * @return metacard
   */
  static public MetacardImpl populate( MetacardImpl metacard, JSONObject metacardJson ){
    String key = null;
    
    if( metacardJson != null ){
      
      Iterator keyItr = metacardJson.keys();
      logger.debug("Got JSON keys");
      while( keyItr.hasNext() ){
        key = keyItr.next().toString();
          if(logger.isDebugEnabled()) {
              logger.debug("Setting " + key);
              logger.debug("Value: " + metacardJson.get( key ));
          }
        setAttributeValue( metacard, key, metacardJson.get( key ) );
      }
    }
    return metacard;
  }

  /**
   * Set attribute value.
   *
   * @param metacard the metacard
   * @param key      the key
   * @param value    the value
   */
  static public void setAttributeValue( MetacardImpl metacard, String key, Object value ){
    if( metacard != null && key != null && value != null ){
      Serializable attributeVal = null;
      MetacardType cardType = metacard.getMetacardType();
            
      AttributeDescriptor descriptor = cardType.getAttributeDescriptor( key );
      
      logger.debug( "Working with Proposed Attribute Values {} = {}", key, value );
      
      if( descriptor != null ){
        AttributeType attributeType = descriptor.getType();
        
        if( attributeType != null ){
          
          try{
            if( attributeType.equals( BasicTypes.DATE_TYPE ) ){
              String dateString = value.toString();
              SimpleDateFormat sdf = new SimpleDateFormat( dateFormatPattern );
              attributeVal = sdf.parse( dateString );
            }
            else if( attributeType.equals( BasicTypes.DOUBLE_TYPE ) ){
              String doubleString = value.toString();
              attributeVal = Double.parseDouble( doubleString );
            }
            else if (attributeType.equals( BasicTypes.INTEGER_TYPE ) ){
                String intString = value.toString();
                attributeVal = Integer.parseInt(intString);
            }
            else{
              attributeVal = value.toString();
            }
          }
          catch( Exception e ){
            logger.error( "Metacard Attribute [{}] could not be set: {}", key, e.getMessage(), e );
          }
          
        }
      }
      else{
        attributeVal = value.toString();
      }
      
      logger.debug( "Setting Metacard Attribute Values {} = {}", key, attributeVal );

      metacard.setAttribute( key, attributeVal );
    }
  }


  /**
   * Set defaults.
   *
   * @param metacard the metacard
   */
  static public void setDefaults( MetacardImpl metacard ){
    if( metacard != null ){
      MetacardDefaults metacardDefaults = new MetacardDefaults();
      
      if( metacard.getId() == null || metacard.getId().trim().length() == 0 ){
        metacard.setId( UUID.randomUUID().toString() );
      }
      metacard.setContentTypeName( metacardDefaults.getContentTypeName() );
      metacard.setContentTypeVersion( metacardDefaults.getContentTypeVersion() );
      metacard.setCreatedDate( metacardDefaults.getCreatedDate() );
      metacard.setEffectiveDate( metacardDefaults.getEffectiveDate() );
      metacard.setExpirationDate( metacardDefaults.getExpirationDate() );
      metacard.setModifiedDate( metacardDefaults.getModifiedDate() );
    }
  }

  /**
   * Print metacard.
   *
   * @param metacard the metacard
   */
  static public void printMetacard( MetacardImpl metacard ){
    if( metacard != null ){
      System.out.println( "Metacard..." );
      System.out.println( "Source Id: " + metacard.getSourceId() );
      System.out.println( "Title: " + metacard.getTitle() );
      System.out.println( "Created: " + metacard.getCreatedDate() );
      System.out.println( "Effective: " + metacard.getEffectiveDate() );
      System.out.println( "Modified: " + metacard.getModifiedDate() );
      System.out.println( "Expiration: " + metacard.getExpirationDate() );
      System.out.println( "Metadata: " + metacard.getMetadata() );
      System.out.println( "Type: " + metacard.getMetacardType() );
      System.out.println();
    }
  }
}
