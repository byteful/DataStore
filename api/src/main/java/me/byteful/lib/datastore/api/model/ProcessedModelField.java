package me.byteful.lib.datastore.api.model;

import org.jetbrains.annotations.NotNull;

public interface ProcessedModelField {
  @NotNull
  String key();

  @NotNull
  String value();

  @NotNull
  ProcessedModelFieldType type();
}
