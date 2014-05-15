/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.contentindex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.duracloud.common.util.TagUtil;
import org.duracloud.contentindex.client.ContentIndexItem;

/**
 * @author Daniel Bernstein
 *	       Date: May 14, 2014
 */
public class ContentIndexItemUtil {
    /**
     * @param props
     * @param indexItem
     */
    public static void setProps(Map<String, String> props, ContentIndexItem indexItem) {
        Map<String, String> contentProps = null;
        if (props != null) {
            // remove the tags to ensure the tag
            // data is not duplicated in content index
            contentProps = new HashMap<>(props);
        }else{
            contentProps = new HashMap<>();
        }

        String tagString = contentProps.remove(TagUtil.TAGS);

        if (tagString != null) {
            indexItem.setTags(new ArrayList<String>(TagUtil
                    .parseTags(tagString)));
        }
        indexItem.setProps(contentProps);
    }

}
