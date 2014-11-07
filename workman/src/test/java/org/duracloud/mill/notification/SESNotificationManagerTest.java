/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.notification;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.config.PinningConfiguration.Store;

import org.duracloud.mill.bitlog.BitIntegrityResult;
import org.duracloud.mill.bitlog.BitLogItem;
import org.duracloud.mill.bitlog.jpa.JpaBitLogItem;
import org.duracloud.mill.db.model.BitIntegrityReport;
import org.duracloud.mill.test.AbstractTestBase;
import org.duracloud.storage.domain.StorageProviderType;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.junit.Test;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

import static org.easymock.EasyMock.*;

/**
 * @author Daniel Bernstein
 *	       Date: Jan 2, 2014
 */
public class SESNotificationManagerTest extends AbstractTestBase {


    @Mock
    private AmazonSimpleEmailServiceClient client;

    private SESNotificationManager notificationManager;

    /**
     * Test method for
     * {@link org.duracloud.mill.notification.SESNotificationManager#newSpace(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testNewSpace() {
        setupSubject();
        replayAll();
        notificationManager.newSpace("test", "storeId", "spaceId", "datetime", "username");
    }

    private void setupSubject() {
        SendEmailResult result = new SendEmailResult();
        EasyMock.expect(client.sendEmail(EasyMock.isA(SendEmailRequest.class))).andReturn(result);
        String[] recipients = new String[]{"test1@duracloud.org", "test2@duracloud.org"};
        notificationManager = new SESNotificationManager(recipients, client);
    }
    
    @Test
    public void testBitErrors() {
        setupSubject();
        BitIntegrityReport report = createMock(BitIntegrityReport.class);
        JpaBitLogItem item =                 new JpaBitLogItem();
        item.setResult(BitIntegrityResult.FAILURE);
        item.setModified(new Date());
        item.setStorageProviderType(StorageProviderType.AMAZON_S3  );
       
        List<BitLogItem> errors = Arrays.asList(new BitLogItem[]{
                item
        });
        expect(report.getAccount()).andReturn("account");
        expect(report.getStoreId()).andReturn("store-id");
        expect(report.getSpaceId()).andReturn("space-id");
        expect(report.getId()).andReturn(1l);

        replayAll();
        notificationManager.bitIntegrityErrors(report, errors);
    }

}
