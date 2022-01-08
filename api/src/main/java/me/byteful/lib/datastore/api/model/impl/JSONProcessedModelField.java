package me.byteful.lib.datastore.api.model.impl;

import com.google.gson.Gson;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.model.ProcessedModelField;
import me.byteful.lib.datastore.api.model.ProcessedModelFieldType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class JSONProcessedModelField implements ProcessedModelField {
  @NotNull private final String key, value;
  @NotNull private final ProcessedModelFieldType type;

  private JSONProcessedModelField(
      @NotNull String key, @NotNull String value, @NotNull ProcessedModelFieldType type) {
    this.key = key;
    this.value = value;
    this.type = type;
  }

  public static JSONProcessedModelField of(
      @NotNull final String key,
      @NotNull final String value,
      @NotNull ProcessedModelFieldType type) {
    return new JSONProcessedModelField(key, value, type);
  }

  public static JSONProcessedModelField of(
      @NotNull final String key,
      @NotNull final Object value,
      @NotNull ProcessedModelFieldType type) {
    return new JSONProcessedModelField(key, DataStoreConstants.GSON.toJson(value), type);
  }

  public static JSONProcessedModelField of(
      @NotNull final String key,
      @NotNull final Object value,
      @NotNull ProcessedModelFieldType type,
      @NotNull final Gson gson) {
    return new JSONProcessedModelField(key, gson.toJson(value), type);
  }

  @Override
  public @NotNull String key() {
    return key;
  }

  @Override
  public @NotNull String value() {
    return value;
  }

  @Override
  public @NotNull ProcessedModelFieldType type() {
    return type;
  }

  @Override
  public String toString() {
    return "JSONProcessedModelField{"
        + "key='"
        + key
        + '\''
        + ", value='"
        + value
        + '\''
        + ", type="
        + type
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JSONProcessedModelField that = (JSONProcessedModelField) o;
    return key.equals(that.key) && value.equals(that.value) && type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value, type);
  }
}
