package me.byteful.lib.datastore.mysql;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.pool.HikariPool;
import me.byteful.lib.datastore.api.DataStoreConstants;
import me.byteful.lib.datastore.api.data.DataStore;
import me.byteful.lib.datastore.api.model.*;
import me.byteful.lib.datastore.api.model.impl.JSONProcessedModel;
import me.byteful.lib.datastore.api.model.impl.JSONProcessedModelField;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MySQLDataStore implements DataStore {
  private final HikariPool pool;
  private final Gson gson;

  public MySQLDataStore(@NotNull HikariConfig hikariConfig) {
    this.pool = new HikariPool(hikariConfig);
    this.gson = DataStoreConstants.GSON;
  }

  public MySQLDataStore(@NotNull HikariPool pool) {
    this.pool = pool;
    this.gson = DataStoreConstants.GSON;
  }

  public MySQLDataStore(
      @NotNull String host,
      int port,
      @NotNull String user,
      @NotNull String password,
      @NotNull String database) {
    this(String.format("mysql://%s:%s@%s:%s/%s", user, password, host, port, database), DataStoreConstants.GSON);
  }

  public MySQLDataStore(@NotNull String uri, @NotNull Gson gson) {
    final HikariConfig config = new HikariConfig();
    config.setJdbcUrl(uri);

    this.pool = new HikariPool(config);
    this.gson = gson;
  }

  public MySQLDataStore(@NotNull HikariConfig hikariConfig, @NotNull Gson gson) {
    this.pool = new HikariPool(hikariConfig);
    this.gson = gson;
  }

  public MySQLDataStore(@NotNull HikariPool pool, @NotNull Gson gson) {
    this.pool = pool;
    this.gson = gson;
  }

  public MySQLDataStore(
      @NotNull String host,
      int port,
      @NotNull String user,
      @NotNull String password,
      @NotNull String database,
      @NotNull Gson gson) {
    this(String.format("mysql://%s:%s@%s:%s/%s", user, password, host, port, database), gson);
  }

  @Override
  public @NotNull <T extends Model> Optional<T> get(
      @NotNull Class<T> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String table = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);

    try (Connection conn = pool.getConnection()) {
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
      e.printStackTrace();
    }

    return Optional.empty();
  }

  @Override
  public void set(@NotNull ModelId id, @NotNull Model model) {
    final ProcessedModel processed = serializeModel(model);
    final String table = getStoredGroup(model.getClass());

    try (Connection conn = pool.getConnection()) {
      createTableIfNotExists(conn, table, processed);

      runInsertSql(conn, table, processed);
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  @Override
  public boolean exists(
      @NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    return get(type, id, ids).isPresent(); // TODO: UPDATE TO A BETTER EXISTS METHOD
  }

  @Override
  public void delete(
      @NotNull Class<? extends Model> type, @NotNull ModelId id, @NotNull ModelId... ids) {
    final String table = getStoredGroup(type);
    final @NotNull ModelId[] compiled = compile(id, ids);

    try (Connection conn = pool.getConnection()) {
      if (tableNotExists(conn, table)) {
        return;
      }

      runDeleteSql(conn, table, compiled);
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

    try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
      for (int i = 1; i < ids.length + 1; i++) {
        statement.setString(i, ids[i - 1].value());
      }

      try (ResultSet rs = statement.executeQuery()) {
        if (rs.first()) {
          final Map<String, String> map = new HashMap<>();

          for (String s : list) {
            map.put(s, rs.getString(s));
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

  private void runInsertSql(
      @NotNull Connection connection, @NotNull String table, @NotNull ProcessedModel model) {
    final List<String> keys = new ArrayList<>(), combined = new ArrayList<>();

    model
        .values()
        .forEach(
            (k, v) -> {
              keys.add(k);
              combined.add(k + "=?");
            });

    final String sql =
        String.format(
            "insert into %s (%s) values (%s) on duplicate key update %s;",
            table,
            String.join(",", keys),
            String.join(",", Collections.nCopies(keys.size(), "?")),
            String.join(",", combined));

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
    pool.shutdown();
  }
}
