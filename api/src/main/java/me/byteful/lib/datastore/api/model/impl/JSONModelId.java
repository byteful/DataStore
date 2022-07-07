package me.byteful.lib.datastore.api.model.impl;

import com.google.gson.Gson;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.model.ModelId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class JSONModelId implements ModelId, Cloneable {
  @NotNull
  private final String key, value;

  private JSONModelId(@NotNull final String key, @NotNull final String value) {
    this.key = key;
    this.value = value;
  }

  public static JSONModelId of(@NotNull final String key, @NotNull final String value) {
    return new JSONModelId(key, value);
  }

  public static JSONModelId of(@NotNull final String key, @NotNull final Object value) {
    return new JSONModelId(key, DataStoreConstants.GSON.toJson(value));
  }

  public static JSONModelId of(
    @NotNull final String key, @NotNull final Object value, @NotNull final Gson gson) {
    return new JSONModelId(key, gson.toJson(value));
  }

  @NotNull
  public String key() {
    return key;
  }

  @NotNull
  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JSONModelId that = (JSONModelId) o;
    return key.equals(that.key) && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public String toString() {
    return "JSONModelId{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
  }

  @Override
  public JSONModelId clone() {
    final JSONModelId clone;

    try {
      clone = (JSONModelId) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new AssertionError();
    }

    return clone;
  }
}
