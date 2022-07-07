package me.byteful.lib.datastore.sqlite;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.data.DataStore;
import me.byteful.lib.datastore.api.model.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MongoDBDataStore implements DataStore {
  private static final JsonWriterSettings SETTINGS = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

  private final Gson gson;
  private final MongoDatabase database;
  private final MongoClient client;

  public MongoDBDataStore(Gson gson, String uri, String database) {
    this.gson = gson;
    this.client = new MongoClient(new MongoClientURI(uri));
    this.database = client.getDatabase(database);
  }

  public MongoDBDataStore(String uri, String database) {
    this(DataStoreConstants.GSON, uri, database);
  }

  @Override
  public @NotNull <T extends Model> Optional<T> get(@NotNull Class<T> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String group = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);
    final MongoCollection<Document> col = database.getCollection(group);
    FindIterable<Document> find = col.find();
    for (ModelId modelId : compiled) {
      find = find.filter(Filters.eq(modelId.key(), modelId.value()));
    }

    final Document document = find.first();
    if (document == null) {
      return Optional.empty();
    }

    return Optional.of(gson.fromJson(document.toJson(SETTINGS), type));
  }

  @Override
  public @NotNull <T extends Model> List<T> getAll(@NotNull Class<T> type) {
    final String group = getStoredGroup(type);
    final List<T> list = new ArrayList<>();
    final MongoCollection<Document> col = database.getCollection(group);
    col.find().forEach((Consumer<? super Document>) doc -> {
      if (doc == null) {
        return;
      }

      list.add(gson.fromJson(doc.toJson(SETTINGS), type));
    });


    return list;
  }

  @Override
  public void set(@NotNull ModelId id, @NotNull Model model) {
    final ProcessedModel processed = serializeModel(model);
    final String group = getStoredGroup(model.getClass());
    final MongoCollection<Document> col = database.getCollection(group);
    final Bson fID = Filters.eq(id.key(), id.value());
    Document doc = new Document();
    for (ProcessedModelField field : processed.values().values()) {
      doc = doc.append(field.key(), field.value());

      if (field.type() == ProcessedModelFieldType.INDEXED) {
        col.createIndex(Filters.eq(field.key(), field.value()));
      } else if (field.type() == ProcessedModelFieldType.UNIQUE_INDEXED) {
        col.createIndex(Filters.eq(field.key(), field.value()), new IndexOptions().unique(true));
      }
    }

    if (col.find(fID).first() == null) {
      col.insertOne(doc);
    } else {
      col.replaceOne(fID, doc);
    }
  }

  @Override
  public boolean exists(@NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    return get(type, id, ids).isPresent(); // TODO: UPDATE TO A BETTER EXISTS METHOD
  }

  @Override
  public void delete(@NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String group = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);
    final MongoCollection<Document> col = database.getCollection(group);
    final Bson[] filters = new Bson[compiled.length];
    for (int i = 0; i < compiled.length; i++) {
      final ModelId modelId = compiled[i];
      filters[i] = Filters.eq(modelId.key(), modelId.value());
    }

    col.deleteOne(Filters.and(filters));
  }

  @Override
  public void clear(@NotNull Class<? extends Model> type) {
    final String group = getStoredGroup(type);
    database.getCollection(group).deleteMany(new Document());
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}