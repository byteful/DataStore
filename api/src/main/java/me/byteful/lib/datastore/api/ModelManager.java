package me.byteful.lib.datastore.api;

import me.byteful.lib.datastore.api.model.Model;
import me.byteful.lib.datastore.api.model.ModelStructure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class ModelManager {
  @NotNull
  private static final Map<Class<? extends Model>, ModelStructure<?>> mappedModels =
    new HashMap<>();

  public static void registerModelStructure(@NotNull ModelStructure<?> structure) {
    mappedModels.put(structure.getModelType(), structure);
  }

  @Nullable
  public static <T extends Model> ModelStructure<T> getStructureFromClass(@NotNull Class<T> type) {
    return (ModelStructure<T>) mappedModels.get(type);
  }
}
