package me.byteful.lib.datastore.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

public interface ProcessedModel {
  @NotNull
  Map<String, ProcessedModelField> values();

  @NotNull
  ProcessedModel append(
      @NotNull String key, @NotNull ProcessedModelFieldType fieldType, @Nullable Object value);

  @NotNull
  ProcessedModel append(@NotNull ProcessedModelField field);

  @NotNull
  <T> Optional<T> get(@NotNull String key, @NotNull Class<T> type);

  @Nullable
  <T> T getNullable(@NotNull String key, @NotNull Class<T> type);

  @NotNull
  <T> Optional<T> get(@NotNull String key, @NotNull Type type);

  @Nullable
  <T> T getNullable(@NotNull String key, @NotNull Type type);

  @NotNull
  Optional<ProcessedModelField> getField(@NotNull String key);

  @Nullable
  ProcessedModelField getFieldNullable(@NotNull String key);

  boolean has(@NotNull String key);

  boolean hasType(@NotNull String key, @NotNull Class<?> type);

  @NotNull
  String toString();
}
