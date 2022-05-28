package me.byteful.lib.datastore.sqlite;

import com.google.gson.Gson;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.data.DataStore;
import me.byteful.lib.datastore.api.model.*;
import me.byteful.lib.datastore.api.model.impl.JSONProcessedModel;
import me.byteful.lib.datastore.api.model.impl.JSONProcessedModelField;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class SQLiteDataStore implements DataStore {
  private final Gson gson;
  private final Connection conn;

  public SQLiteDataStore(Gson gson, Path file) {
    this.gson = gson;
    this.conn = buildConnection(file);
  }

  public SQLiteDataStore(Path file) {
    this.conn = buildConnection(file);
    this.gson = DataStoreConstants.GSON;
  }

  private Connection buildConnection(Path file) {
    final Properties properties = new Properties();
    properties.setProperty("foreign_keys", "on");
    properties.setProperty("busy_timeout", "1000");

    try {
      return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath(), properties);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @NotNull <T extends Model> Optional<T> get(@NotNull Class<T> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String table = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);

    try {
      if (tableNotExists(conn, table)) {
        return Optional.empty();
      }

      final Map<String, String> data = runSelectQuery(conn, table, compiled);

      if (data == null) {
        return Optional.empty();
      }

      final JSONProcessedModel processed = new JSONProcessedModel(gson);
      Objects.requireNonNull(data)
        .forEach(
          (k, v) ->
            processed.append(
              JSONProcessedModelField.of(k, v, ProcessedModelFieldType.NORMAL)));

      return Optional.of(deserializeModel(type, processed));
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public @NotNull <T extends Model> List<T> getAll(@NotNull Class<T> type) {
    final String table = getStoredGroup(type);
    final List<T> list = new ArrayList<>();

    try {
      if (tableNotExists(conn, table)) {
        return list;
      }

      final List<Map<String, String>> data = runSelectAllQuery(conn, table);

      if (data == null) {
        return list;
      }

      for (Map<String, String> map : data) {
        final JSONProcessedModel processed = new JSONProcessedModel(gson);
        map.forEach((k, v) ->
          processed.append(
            JSONProcessedModelField.of(k, v, ProcessedModelFieldType.NORMAL)));
        list.add(deserializeModel(type, processed));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return list;
  }

  @Override
  public void set(@NotNull ModelId id, @NotNull Model model) {
    final ProcessedModel processed = serializeModel(model);
    final String table = getStoredGroup(model.getClass());

    createTableIfNotExists(conn, table, processed);
    runInsertSql(conn, table, processed);
  }

  @Override
  public boolean exists(@NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    return get(type, id, ids).isPresent(); // TODO: UPDATE TO A BETTER EXISTS METHOD
  }

  @Override
  public void delete(@NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String table = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);

    try {
      if (tableNotExists(conn, table)) {
        return;
      }

      runDeleteSql(conn, table, compiled);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void clear(@NotNull Class<? extends Model> type) {
    final String table = getStoredGroup(type);
    final String sql = String.format("delete from %s;", table);

    try {
      if (tableNotExists(conn, table)) {
        return;
      }

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.execute();
      }

      try (PreparedStatement statement = conn.prepareStatement("VACUUM;")) {
        statement.execute();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private void createTableIfNotExists(
    @NotNull Connection connection, @NotNull String tableName, @NotNull ProcessedModel model) {
    final List<String> list = new ArrayList<>(), indexes = new ArrayList<>(), uniqueIndexes = new ArrayList<>();
    for (ProcessedModelField field : model.values().values()) {
      String data = field.key() + " varchar(255)";

      if (field.type() == ProcessedModelFieldType.INDEXED) {
        indexes.add(field.key());
      } else if (field.type() == ProcessedModelFieldType.UNIQUE_INDEXED) {
        data += " unique";
        uniqueIndexes.add(field.key());
      }

      list.add(data);
    }

    String sql =
      String.format("create table if not exists %s (%s);", tableName, String.join(",", list));

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    if(indexes.isEmpty()) {
      return;
    }

    for (String index : indexes) {
      sql = String.format("create index if not exists index_%s on %s (%s);", index, tableName, index);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.execute();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    for (String index : uniqueIndexes) {
      sql = String.format("create unique index if not exists index_%s on %s (%s);", index, tableName, index);

      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.execute();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  private void runDeleteSql(
    @NotNull Connection connection, @NotNull String table, @NotNull ModelId[] ids) {
    final List<String> list = new ArrayList<>();
    for (ModelId id : ids) {
      list.add(id.key() + "=?");
    }

    final String sql = String.format("delete from %s where %s;", table, String.join(" and ", list));

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 1; i < ids.length + 1; i++) {
        statement.setString(i, ids[i - 1].value());
      }

      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private Map<String, String> runSelectQuery(
    @NotNull Connection connection, @NotNull String table, @NotNull ModelId[] ids) {
    final List<String> list = new ArrayList<>();
    for (ModelId id : ids) {
      list.add(id.key() + "=?");
    }

    final String sql =
      String.format("select * from %s where %s;", table, String.join(" and ", list));

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 1; i < ids.length + 1; i++) {
        statement.setString(i, ids[i - 1].value());
      }

      try (ResultSet rs = statement.executeQuery()) {
        if (rs.next()) {
          final Map<String, String> map = new HashMap<>();

          final ResultSetMetaData meta = rs.getMetaData();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getString(i));
          }

          return map;
        } else {
          return null;
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return null;
  }

  private List<Map<String, String>> runSelectAllQuery(
    @NotNull Connection connection, @NotNull String table) {
    final String sql =
      String.format("select * from %s;", table);

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      try (ResultSet rs = statement.executeQuery()) {
        final List<Map<String, String>> data = new ArrayList<>();
        while(rs.next()) {
          final Map<String, String> map = new HashMap<>();

          final ResultSetMetaData meta = rs.getMetaData();
          for (int i = 1; i <= meta.getColumnCount(); i++) {
            map.put(meta.getColumnName(i), rs.getString(i));
          }

          data.add(map);
        }

        return data;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return null;
  }

  private void runInsertSql(
    @NotNull Connection connection, @NotNull String table, @NotNull ProcessedModel model) {
    final List<String> keys = new ArrayList<>();

    model
      .values()
      .forEach(
        (k, v) -> keys.add(k));

    final String sql =
      String.format(
        "replace into %s (%s) values (%s);",
        table,
        String.join(",", keys),
        String.join(",", Collections.nCopies(keys.size(), "?")));

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 1; i < keys.size() + 1; i++) {
        int finalI = i;
        model
          .getField(keys.get(i - 1))
          .ifPresent(
            field -> {
              try {
                statement.setString(finalI, field.value());
              } catch (SQLException e) {
                e.printStackTrace();
              }
            });
      }

      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  private boolean tableNotExists(@NotNull Connection connection, @NotNull String table)
    throws SQLException {
    return !connection.getMetaData().getTables(null, null, table, null).next();
  }

  @Override
  public void close() throws Exception {
    conn.close();
  }
}