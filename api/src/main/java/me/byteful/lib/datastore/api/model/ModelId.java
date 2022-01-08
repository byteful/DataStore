package me.byteful.lib.datastore.api.model;

import org.jetbrains.annotations.NotNull;

public interface ModelId {
  @NotNull
  String key();

  @NotNull
  String value();
}
