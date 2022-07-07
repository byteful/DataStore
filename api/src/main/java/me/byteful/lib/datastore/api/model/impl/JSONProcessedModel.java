package me.byteful.lib.datastore.api.model.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.model.ProcessedModel;
import me.byteful.lib.datastore.api.model.ProcessedModelField;
import me.byteful.lib.datastore.api.model.ProcessedModelFieldType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JSONProcessedModel implements ProcessedModel, Cloneable {
  private final Map<String, ProcessedModelField> data;
  private final Gson gson;

  public JSONProcessedModel() {
    this.gson = DataStoreConstants.GSON;
    this.data = new HashMap<>();
  }

  public JSONProcessedModel(@NotNull final Gson gson) {
    this.gson = gson;
    this.data = new HashMap<>();
  }

  public JSONProcessedModel(@NotNull String json) {
    this.gson = DataStoreConstants.GSON;
    this.data = new HashMap<>();

    convertFromJSON(json);
  }

  public JSONProcessedModel(@NotNull String json, @NotNull Gson gson) {
    this.gson = gson;
    this.data = new HashMap<>();

    convertFromJSON(json);
  }

  public JSONProcessedModel(@NotNull final Map<String, ProcessedModelField> data) {
    this.gson = DataStoreConstants.GSON;
    this.data = new HashMap<>(data);
  }

  public JSONProcessedModel(
    @NotNull final Map<String, ProcessedModelField> data, @NotNull final Gson gson) {
    this.gson = gson;
    this.data = new HashMap<>(data);
  }

  private void convertFromJSON(@NotNull String json) {
    final JsonObject obj = gson.fromJson(json, JsonObject.class);

    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
      append(entry.getKey(), ProcessedModelFieldType.NORMAL, entry.getValue());
    }
  }

  @Override
  public @NotNull Map<String, ProcessedModelField> values() {
    return data;
  }

  @Override
  public @NotNull ProcessedModel append(
    @NotNull String key, @NotNull ProcessedModelFieldType fieldType, @Nullable Object value) {
    data.put(key, JSONProcessedModelField.of(key, value, fieldType));

    return this;
  }

  @Override
  public @NotNull ProcessedModel append(@NotNull ProcessedModelField field) {
    if (field instanceof JSONProcessedModelField) {
      data.put(field.key(), field);
    } else {
      throw new IllegalArgumentException(
        field.getClass().getName()
          + " is not a subclass of "
          + JSONProcessedModelField.class.getName());
    }

    return this;
  }

  @Override
  public @NotNull <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type) {
    return Optional.ofNullable(gson.fromJson(data.get(key).value(), type));
  }

  @Override
  public <T> @Nullable T getNullable(@NotNull String key, @NotNull Class<T> type) {
    return get(key, type).orElse(null);
  }

  @Override
  public <T> @NotNull Optional<T> get(@NotNull String key, @NotNull Type type) {
    return Optional.ofNullable(gson.fromJson(data.get(key).value(), type));
  }

  @Override
  public <T> T getNullable(@NotNull String key, @NotNull Type type) {
    return (T) get(key, type).orElse(null);
  }

  @Override
  public @NotNull Optional<ProcessedModelField> getField(@NotNull String key) {
    return Optional.ofNullable(data.get(key));
  }

  @Override
  public @Nullable ProcessedModelField getFieldNullable(@NotNull String key) {
    return getField(key).orElse(null);
  }

  @Override
  public boolean has(@NotNull String key) {
    return data.containsKey(key);
  }

  @Override
  public boolean hasType(@NotNull String key, @NotNull Class<?> type) {
    boolean b = false;

    try {
      gson.fromJson(
        data.get(key).value(),
        type); // Yes, exception catching is bad... But I have not been able to find a valid
      // method in GSON that lets me check if it's the correct type. Please make a PR if
      // you do know of this method!

      b = true;
    } catch (Exception ignored) {
      //
    }

    return has(key) && b;
  }

  @NotNull
  public String toJSON() {
    final JsonObject obj = new JsonObject();

    for (ProcessedModelField value : data.values()) {
      obj.add(value.key(), gson.fromJson(value.value(), JsonElement.class));
    }

    return obj.toString();
  }

  @Override
  public @NotNull String toString() {
    return toJSON();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JSONProcessedModel that = (JSONProcessedModel) o;
    return Objects.equals(data, that.data) && Objects.equals(gson, that.gson);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, gson);
  }

  @Override
  public JSONProcessedModel clone() {
    final JSONProcessedModel clone;

    try {
      clone = (JSONProcessedModel) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new AssertionError();
    }

    return clone;
  }
}
