package edu.mit.ll.nics.android.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;

import org.junit.Assert;
import org.junit.Test;

import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.MarkupFeature;

/**
 * Test deserializing a few classes that use NestedJSONTypeAdapter
 */
public class NestedJSONTypeAdapterTest {
    @Test
    public void deserialize_MarkupFeature() {
        MarkupFeature feature = new GsonBuilder().create().fromJson(
                "{ \"featureId\": \"123\", \"attributes\":\"{\\\"layerid\\\":456,\\\"comments\\\":\\\"some comments\\\",\\\"description\\\":\\\"some description\\\"}\" }",
                MarkupFeature.class);
        Assert.assertEquals("featureId", "123", feature.getFeatureId());

        MarkupFeature.Attributes attrs = feature.getAttributes();
        Assert.assertNotNull("attributes", attrs);
        Assert.assertEquals("layerId attribute", 456, attrs.getLayerId());
        Assert.assertEquals("comments attribute", "some comments", attrs.getComments());
        Assert.assertEquals("description attribute", "some description", attrs.getDescription());
    }

    @Test
    public void deserialize_MarkupFeature_NonString() {
        MarkupFeature feature = new GsonBuilder().create().fromJson(
                "{ \"featureId\": \"123\", \"attributes\": { \"layerid\":456,\"comments\":\"some comments\",\"description\":\"some description\"} }",
                MarkupFeature.class);
        Assert.assertEquals("featureId", "123", feature.getFeatureId());

        MarkupFeature.Attributes attrs = feature.getAttributes();
        Assert.assertNotNull("attributes", attrs);
        Assert.assertEquals("layerId attribute", 456, attrs.getLayerId());
        Assert.assertEquals("comments attribute", "some comments", attrs.getComments());
        Assert.assertEquals("description attribute", "some description", attrs.getDescription());
    }

    @Test
    public void deserialize_MarkupFeature_null() {
        MarkupFeature feature = new GsonBuilder().create().fromJson(
                "{ \"featureId\": \"123\", \"attributes\": \"\" }",
                MarkupFeature.class);
        Assert.assertEquals("featureId", "123", feature.getFeatureId());

        MarkupFeature.Attributes attrs = feature.getAttributes();
        Assert.assertNotNull("attributes", attrs);
        Assert.assertNull("comments attribute", attrs.getComments());
        Assert.assertNull("description attribute", attrs.getDescription());
    }

    @Test
    public void deserialize_EODReport() {
        EODReport report = new GsonBuilder().create().fromJson(
                "{ \"message\":\"{\\\"user\\\":\\\"some user\\\",\\\"desc\\\":\\\"some description\\\"}\" }",
                EODReport.class);
        Assert.assertEquals("user attribute", "some user", report.getUser());
        Assert.assertEquals("description attribute", "some description", report.getDescription());
    }

    @Test
    public void deserialize_EODReport_NonString() {
        EODReport report = new GsonBuilder().create().fromJson(
                "{ \"message\": {\"user\":\"some user\",\"desc\":\"some description\"} }",
                EODReport.class);
        Assert.assertEquals("user attribute", "some user", report.getUser());
        Assert.assertEquals("description attribute", "some description", report.getDescription());
    }

    @Test
    public void deserialize_EODReport_null() {
        EODReport report = new GsonBuilder().create().fromJson(
                "{ \"message\":\"\" }",
                EODReport.class);
        Assert.assertNull("layerId attribute", report.getUser());
        Assert.assertNull("description attribute", report.getDescription());
    }

    @Test
    public void deserialize_GeneralMessage() {
        GeneralMessage report = new GsonBuilder().create().fromJson(
                "{ \"message\":\"{\\\"user\\\":\\\"some user\\\",\\\"desc\\\":\\\"some description\\\"}\" }",
                GeneralMessage.class);
        Assert.assertEquals("user attribute", "some user", report.getUser());
        Assert.assertEquals("description attribute", "some description", report.getDescription());
    }

    @Test
    public void deserialize_GeneralMessage_NonString() {
        GeneralMessage report = new GsonBuilder().create().fromJson(
                "{ \"message\": {\"user\":\"some user\",\"desc\":\"some description\"} }",
                GeneralMessage.class);
        Assert.assertEquals("user attribute", "some user", report.getUser());
        Assert.assertEquals("description attribute", "some description", report.getDescription());
    }

    @Test
    public void deserialize_GeneralMessage_null() {
        GeneralMessage report = new GsonBuilder().create().fromJson(
                "{ \"message\":\"\" }",
                GeneralMessage.class);
        Assert.assertNull("layerId attribute", report.getUser());
        Assert.assertNull("description attribute", report.getDescription());
    }
}
