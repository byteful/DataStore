package me.byteful.lib.datastore.api.model;

import org.jetbrains.annotations.NotNull;

public interface ModelStructure<T extends Model> {
  @NotNull
  ProcessedModel serialize(@NotNull T t);

  @NotNull
  T deserialize(@NotNull ProcessedModel processed);

  @NotNull
  Class<T> getModelType();
}
