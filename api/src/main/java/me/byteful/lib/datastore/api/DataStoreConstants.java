package me.byteful.lib.datastore.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class DataStoreConstants {
  public static final Gson GSON =
    new GsonBuilder().serializeNulls().disableHtmlEscaping().setLenient().create();
}
