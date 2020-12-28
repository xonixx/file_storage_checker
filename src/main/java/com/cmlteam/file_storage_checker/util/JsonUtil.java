package com.cmlteam.file_storage_checker.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public final class JsonUtil {

  private static final int MAX_LENGTH = 100;

  private JsonUtil() {}

  private static final ObjectMapper OBJECT_MAPPER = prepareObjectMapper();

  private static ObjectMapper prepareObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    mapper.setDateFormat(df);
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  public static String toJsonString(Object object) {
    try {
      return OBJECT_MAPPER.writeValueAsString(object);
    } catch (IOException e) {
      throw new JsonParseException(e);
    }
  }

  public static List<?> parseList(String json) {
    try {
      return OBJECT_MAPPER.readValue(json, List.class);
    } catch (IOException e) {
      throw new JsonParseException("Unable to parse json to list: " + trim(json, MAX_LENGTH), e);
    }
  }

  public static Map parseJson(String json) {
    try {
      return OBJECT_MAPPER.readValue(json, Map.class);
    } catch (IOException e) {
      throw new JsonParseException("Unable to parse json to map: " + trim(json, MAX_LENGTH), e);
    }
  }

  public static <T> T parseJson(String json, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(json, clazz);
    } catch (IOException e) {
      throw new JsonParseException("Unable to parse json to class: " + trim(json, MAX_LENGTH), e);
    }
  }

  public static <T> List<T> parseJsonList(String json, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(
          json, OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    } catch (IOException e) {
      throw new JsonParseException("Unable to parse json to class: " + trim(json, MAX_LENGTH), e);
    }
  }

  public static String prettyPrintJson(String json) {
    if (json == null) {
      return null;
    }
    try {
      Object parsed = OBJECT_MAPPER.readValue(json, Object.class);
      return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
    } catch (IOException e) {
      log.trace("ignored error", e);
      return json;
    }
  }

  private static String trim(String str, int maxLen) {
    if (str == null) {
      return null;
    }

    if (str.length() <= maxLen) {
      return str;
    }

    return str.substring(0, maxLen) + "...";
  }

  public static class JsonParseException extends RuntimeException {
    JsonParseException(Throwable cause) {
      super(cause);
    }

    JsonParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public interface JsonBuilder {
    Object getValue();
  }

  public static class JsonMapBuilder implements JsonBuilder {
    private final Map<String, Object> map = new LinkedHashMap<>();

    public JsonMapBuilder add(String key, Object value) {
      map.put(key, value);
      return this;
    }

    public JsonMapBuilder add(String key, JsonBuilder value) {
      map.put(key, value.getValue());
      return this;
    }

    @Override
    public Object getValue() {
      return map;
    }

    @Override
    public String toString() {
      return toJsonString(map);
    }
  }

  public static class JsonListBuilder implements JsonBuilder {
    private final List<Object> list = new ArrayList<>();

    public JsonListBuilder add(Object value) {
      list.add(value);
      return this;
    }

    public JsonListBuilder add(JsonBuilder value) {
      list.add(value.getValue());
      return this;
    }

    @Override
    public Object getValue() {
      return list;
    }

    @Override
    public String toString() {
      return toJsonString(list);
    }
  }

  public static JsonMapBuilder json() {
    return new JsonMapBuilder();
  }

  public static JsonListBuilder jsonList(Collection<?> values) {
    JsonListBuilder l = jsonList();
    for (Object value : values) {
      l.add(value);
    }
    return l;
  }
  public static JsonListBuilder jsonList() {
    return new JsonListBuilder();
  }
}
