package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.alibaba.fastjson2.JSONWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjectWriterProviderDisableListReferenceDetectTest {
    @Test
    public void testIsNotReferenceDetect() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        assertFalse(provider.isDisableListReferenceDetect());
        assertFalse(provider.isNotReferenceDetect0(ArrayList.class));

        provider.setDisableListReferenceDetect(true);
        assertTrue(provider.isDisableListReferenceDetect());
        assertTrue(provider.isNotReferenceDetect0(ArrayList.class));
        assertTrue(provider.isNotReferenceDetect0(List.class));
        // non-collection types keep the class-level exclusion behavior
        assertFalse(provider.isNotReferenceDetect0(Bean.class));
    }

    @Test
    public void testDefaultDisabled() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        Bean bean = beanWithSharedList();
        String json = JSON.toJSONString(bean, context);
        assertTrue(json.contains("$ref"), json);
        assertTrue(json.contains("$.items"), json);
    }

    @Test
    public void testEnabled() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        provider.setDisableListReferenceDetect(true);
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        Bean bean = beanWithSharedList();
        String json = JSON.toJSONString(bean, context);
        assertFalse(json.contains("$ref"), json);
        assertEquals("{\"items\":[\"a\",\"b\"],\"items2\":[\"a\",\"b\"]}", json);

        Bean parsed = JSON.parseObject(json, Bean.class);
        assertNotSame(parsed.items, parsed.items2);
        assertEquals(Arrays.asList("a", "b"), parsed.items);
        assertEquals(Arrays.asList("a", "b"), parsed.items2);
    }

    @Test
    public void testDefaultDisabledJSONB() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        byte[] bytes = JSONB.toBytes(beanWithSharedList(), context);
        assertTrue(JSONB.toJSONString(bytes).contains("$ref"));
    }

    @Test
    public void testEnabledJSONB() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        provider.setDisableListReferenceDetect(true);
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        byte[] bytes = JSONB.toBytes(beanWithSharedList(), context);
        assertFalse(JSONB.toJSONString(bytes).contains("$ref"));
    }

    @Test
    public void testNonCollectionReferenceDetectionStillEnabled() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        provider.setDisableListReferenceDetect(true);
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        Item shared = new Item();
        shared.id = 101;

        Holder holder = new Holder();
        holder.first = shared;
        holder.second = shared;

        String json = JSON.toJSONString(holder, context);
        assertTrue(json.contains("$ref"), json);
    }

    @Test
    public void testDefaultKeepsCollectionCircularReferenceProtection() {
        ObjectWriterProvider provider = new ObjectWriterProvider();
        JSONWriter.Context context = new JSONWriter.Context(provider, JSONWriter.Feature.ReferenceDetection);

        List<Object> self = new ArrayList<>();
        self.add(self);

        String json = JSON.toJSONString(self, context);
        assertTrue(json.contains("$ref"), json);
    }

    private static Bean beanWithSharedList() {
        List<String> shared = new ArrayList<>(Arrays.asList("a", "b"));

        Bean bean = new Bean();
        bean.items = shared;
        bean.items2 = shared;
        return bean;
    }

    public static class Bean {
        public List<String> items;
        public List<String> items2;
    }

    public static class Item {
        public int id;
    }

    public static class Holder {
        public Item first;
        public Item second;
    }
}
