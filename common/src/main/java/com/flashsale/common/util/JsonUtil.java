package com.flashsale.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonUtil() {
        // Utility class: prevent instantiation
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize object to JSON", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    String.format("Failed to deserialize JSON to class '%s'", clazz.getName()), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize JSON with TypeReference", e);
        }
    }
}
/*
* Yes, **`JsonUtil.java` is useful and appropriate for your project**, especially because your microservices will exchange JSON events through Kafka and store JSON payloads in the Outbox.

### Simple Hinglish

Is file ka main kaam hai:

> **Java objects ↔ JSON conversion ko ek common place par handle karna.**

Without `JsonUtil`:

```text
ProductReservedEvent
       ↓
har service apna ObjectMapper banaye
       ↓
different configurations
       ↓
inconsistent JSON
```

With `JsonUtil`:

```text
Any Event/Object
      ↓
   JsonUtil
      ↓
Common ObjectMapper
      ↓
Consistent JSON
```

### Tumhare project mein kahan use hoga?

#### 1. Transactional Outbox

`ProductReservedEvent` ko database ke `payload` field mein JSON ke form mein store karna:

```text
ProductReservedEvent
       ↓
JsonUtil.toJson()
       ↓
"{ ...JSON... }"
       ↓
OutboxEventRecord.payload
```

#### 2. Kafka

Kafka se JSON receive hone ke baad:

```text
Kafka JSON
   ↓
JsonUtil.fromJson()
   ↓
ProductReservedEvent
```

#### 3. Redis

Agar future mein Redis mein event/cache data store karoge:

```text
Java Object
   ↓
JsonUtil
   ↓
JSON
   ↓
Redis
```

---

### `JavaTimeModule` kyu important hai?

Tumhare events mein:

```java
private Instant expiresAt;
private Instant occurredAt;
```

jaise fields hain.

`JavaTimeModule` ensure karta hai ki date/time properly JSON mein serialize ho:

```json
{
  "expiresAt": "2026-08-28T13:15:30Z"
}
```

Instead of unwanted numeric timestamp/array formats.

---

### `FAIL_ON_UNKNOWN_PROPERTIES = false` kyu?

Suppose Event v1:

```json
{
  "orderId": "ORD-1",
  "amount": 500
}
```

Later producer adds:

```json
{
  "orderId": "ORD-1",
  "amount": 500,
  "currency": "INR"
}
```

Agar older consumer ko `currency` field ke baare mein pata nahi hai, to it can ignore the unknown field instead of failing deserialization.

Ye **event schema evolution** mein useful hai.

---

### `toJson()`

```java
JsonUtil.toJson(event);
```

Java object ko JSON String mein convert karta hai.

Example:

```text
PaymentSuccessEvent
        ↓
     toJson()
        ↓
JSON String
```

---

### `fromJson()`

```java
JsonUtil.fromJson(json, PaymentSuccessEvent.class);
```

JSON ko Java object mein convert karta hai.

```text
JSON
 ↓
fromJson()
 ↓
PaymentSuccessEvent
```

---

### Ek important point

`ObjectMapper` ko static shared instance rakhna generally theek hai **agar usko runtime par modify/configure nahi kiya ja raha**. Tumhare code mein configuration initialization ke time ho rahi hai, so normal concurrent read/use ke liye suitable hai.

### Short mein

Tumhari architecture:

```text
Events
  ↓
JsonUtil
  ↓
JSON
  ↓
Outbox / Kafka / Redis
```

Isliye **`JsonUtil.java` common module mein rakhna sensible hai**.

Aur ye `BaseEvent` ya `EventTopics` jaisa business event nahi hai—ye simply **shared infrastructure utility** hai jo different microservices mein JSON conversion ko consistent rakhti hai.
*/