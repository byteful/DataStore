package me.byteful.lib.datastore.api.data;

import me.byteful.lib.datastore.api.ModelManager;
import me.byteful.lib.datastore.api.model.Model;
import me.byteful.lib.datastore.api.model.ModelId;
import me.byteful.lib.datastore.api.model.ModelStructure;
import me.byteful.lib.datastore.api.model.ProcessedModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface DataStore extends AutoCloseable {
  @NotNull <T extends Model> Optional<T> get(
    @NotNull Class<T> type, @NotNull ModelId id, @NotNull ModelId... ids);

  @NotNull <T extends Model> List<T> getAll(@NotNull Class<T> type, @NotNull ModelId... ids);

  void set(@NotNull ModelId id, @NotNull Model model);

  boolean exists(
    @NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids);

  void delete(@NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids);

  void clear(@NotNull Class<? extends Model> type);

  @NotNull
  default ProcessedModel serializeModel(@NotNull Model model) {
    final ModelStructure<Model> structure =
      ModelManager.getStructureFromClass((Class<Model>) model.getClass());

    if (structure == null) {
      throw new RuntimeException(
        "Model structure for model type ("
          + model.getClass().getName()
          + ") was not registered! Please use ModelManager to register structures for models.");
    }

    return structure.serialize(model);
  }

  @NotNull
  default <T extends Model> T deserializeModel(
    @NotNull Class<T> type, @NotNull ProcessedModel processed) {
    final ModelStructure<T> structure = ModelManager.getStructureFromClass(type);

    if (structure == null) {
      throw new RuntimeException(
        "Model structure for model type ("
          + type.getName()
          + ") was not registered! Please use ModelManager to register structures for models.");
    }

    return structure.deserialize(processed);
  }

  @NotNull
  default ModelId[] compile(@NotNull ModelId id, @NotNull ModelId... ids) {
    final ModelId[] array = new ModelId[1 + ids.length];
    array[0] = id;

    if (ids.length >= 1) {
      System.arraycopy(ids, 0, array, 1, ids.length);
    }

    return array;
  }

  @NotNull
  default String getStoredGroup(@NotNull Class<?> type) {
    final StoredGroup group = type.getAnnotation(StoredGroup.class);

    if (group != null) {
      return group.value();
    } else {
      System.out.println(
        "[WARNING] DataStore detected a class ("
          + type.getName()
          + ") that has no "
          + StoredGroup.class.getSimpleName()
          + " annotation! Using group 'default' instead...");

      return "default";
    }
  }
}
