package com.example;

import com.example.entity.Account;

import javax.persistence.Transient;
import java.io.*;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * @author Yuanqiang.Zhang
 * @since 2023/7/5
 */
public class ClassUtils {

  public static void main(String[] args) throws Exception {
    // 指定要扫描的包名
    String packageName = "com.example.entity"; // 替换为实际的包路径
    // 指定输出的SQL文件路径
    String outputPath = "output.sql"; // 输出文件路径
    scanAndWriteTablesToFile(packageName, outputPath);
  }

  public static void scanAndWriteTablesToFile(String packageName, String outputPath)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
      for (File directory : dirs) {
        scanDirectoryForClassesAndWriteSQL(directory, packageName, writer, true);
      }
    }
  }

  private static void scanDirectoryForClassesAndWriteSQL(File dir, String packageName, BufferedWriter writer, boolean recursive)
          throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      if (file.isDirectory() && recursive) {
        scanDirectoryForClassesAndWriteSQL(file, packageName + "." + file.getName(), writer, recursive);
      } else if (file.getName().endsWith(".class")) {
        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
        Class<?> clazz = Class.forName(className);
        writeTableSqlToFile(clazz, writer);
      }
    }
  }

  private static void writeTableSqlToFile(Class<?> clazz, BufferedWriter writer)
          throws IllegalAccessException, InstantiationException, IOException {
    try {
      // 假设 getTable 方法存在于某个类中，并且是静态的，可以直接通过类名调用
      Table table = getTable(clazz); // 请根据实际情况调整方法名和类名
      writer.write(table.sql());
      writer.newLine(); // 写入换行符，以便于区分每个类的SQL
    } catch (Exception e) {
      System.err.println("Error generating SQL for class: " + clazz.getName());
      e.printStackTrace();
    }
  }

  /**
   * Java类转化为数据库表对象
   *
   * @param c   java实体类
   * @param <T> 泛型
   * @return 数据库表对象
   */
  private static <T> Table getTable(Class<T> c) {
    List<String> codes = getClassSourceCodes(c);
    Map<String, Column> columnMap = getFieldColumnMap(c, codes);
    Class<? super T> superclass = c.getSuperclass();
    if (Objects.nonNull(superclass)) {
      if (superclass != Object.class) {
        List<String> sourceCodes = getClassSourceCodes(c);
        Map<String, Column> superColumnMap = getFieldColumnMap(superclass, sourceCodes);
        columnMap.putAll(superColumnMap);
      }
    }
    if (columnMap.isEmpty()) {
      return null;
    }
    // 获取主键（如果包含名为 id 的属性，则以 id 为主键，否则，则取类中第一个属性为主键）
    Column key;
    if (columnMap.containsKey("id")) {
      key = columnMap.get("id");
    } else {
      key = columnMap.entrySet().stream().findFirst().get().getValue();
    }
    // 剩下的则是其他属性
    LinkedList<Column> columns = new LinkedList<>(columnMap.values());
    if (!columns.get(0).getName().equals(key.getName())) {
      Column copyKey = key.copy();
      columns.remove(key);
      columns.addFirst(copyKey);
    }
    Table table = new Table();
    table.setName(getSqlName(c.getSimpleName()));
    table.setComment(getTableComment(codes));
    table.setColumns(columns);
    return table;
  }

  private static <T> Map<String, Column> getFieldColumnMap(Class<T> c, List<String> codes) {
    Map<String, String> fieldCommentMap = getClassFieldComment(codes, c.getSimpleName());
    Field[] fields = c.getDeclaredFields();
    Map<String, Column> columnMap = new LinkedHashMap<>();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())) {
        String fieldName = field.getName();
        String columnName = getSqlName(fieldName);
        Column column = new Column();
        column.setName(columnName);
        column.setType(getSqlType(field));
        column.setComment(fieldCommentMap.get(fieldName));
        columnMap.put(fieldName, column);
      }
    }
    return columnMap;
  }

  /**
   * 获取 mysql 数据类型
   *
   * @param field java 属性
   * @return mysql的
   */
  private static String getSqlType(Field field) {
    Class<?> type = field.getType();
    boolean annotationPresent = field.isAnnotationPresent(Transient.class);
    if (annotationPresent) {
      return null;
    }
    String name = getSqlName(field.getName());
    if (type == String.class) {
      if (name.startsWith("is_") || name.endsWith("_type") || name.endsWith("_status") || name.endsWith("type") || name.endsWith("status")) {
        return "varchar(32)";
      }
      return "varchar(255)";
    } else if (type == int.class || type == Integer.class) {
      if (Constant.TINYINT_WORDS.contains(name)) {
        return "tinyint(4)";
      }
      if (name.startsWith("is_") || name.endsWith("_type") || name.endsWith("_status") || name.endsWith("type") || name.endsWith("status")) {
        return "tinyint(4)";
      }
      return "int(11)";
    } else if (type == long.class || type == Long.class) {
      return "bigint(20)";
    } else if (type == double.class || type == Double.class) {
      // 最大为 (53, 15)
      return "double(20,2)";
    } else if (type == boolean.class || type == Boolean.class) {
      return "boolean";
    } else if (type == Date.class || type == LocalDate.class || type == LocalDateTime.class) {
      return "datetime";
    } else if (type == BigDecimal.class) {
      // 最大为 decimal(65, 30)
      return "decimal(20,2)";
    } else {
      // 其他类型则不处理
      return null;
    }
  }

  /**
   * 获取类文件源码内容
   *
   * @param clazz 类对象
   * @return 类源码内容
   */
  private static List<String> getClassSourceCodes(Class<?> clazz) {
    String classPath = clazz.getName().replace(Constant.POINT, Constant.LEFT_LINE) + Constant.POINT_JAVA;
    String filePath;
    if (new File(Constant.POM_XML).exists()) {
      filePath = Constant.SRC_MAIN_JAVA + classPath;
    } else {
      filePath = Constant.SRC + classPath;
    }
    File file = new File(filePath);
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

  /**
   * 获取表备注（默认取类文档注释第一行文字）
   *
   * @param codes 类源码内容
   * @return 表备注
   */
  private static String getTableComment(List<String> codes) {
    if (codes.isEmpty()) {
      return null;
    }
    // 按规范来说，第一个格式为 [ * xxx] 的行，即为表的文档注释
    String comment = null;
    for (String line : codes) {
      String trimLine = line.trim();
      if (trimLine.startsWith(Constant.STAR) && line.length() > 1) {
        comment = trimLine.substring(1).trim();
        break;
      }
    }
    if (Objects.isNull(comment)) {
      return null;
    }
    // 按SQL表备注习惯来说，一般称为[xxx表]，因此如果备注不以"表"字结尾，我们这里补加上
    if (comment.endsWith(Constant.TABLE)) {
      return comment;
    }
    return comment + Constant.TABLE;
  }

  /**
   * 获取类属性注释（文档注释、多行注释、单行注释、行尾注释）
   *
   * @param codes 类源码内容
   * @return 表备注
   */
  private static Map<String, String> getClassFieldComment(List<String> codes, String className) {
    boolean fieldLineStart = false;
    Map<String, String> filedCommentMap = new LinkedHashMap<>();
    String comment = null;
    boolean commentStart = false;
    for (int i = 0; i < codes.size(); i++) {
      String code = codes.get(i).trim();
      if (!fieldLineStart) {
        if (code.contains(String.format("class %s", className))) {
          fieldLineStart = true;
        }
        continue;
      }
      if (commentStart) {
        String str = trim(code, Constant.CHAR_STAR).trim();
        if (!str.isEmpty()) {
          comment = str;
          commentStart = false;
          continue;
        }
      } else {
        if (code.startsWith("/*")) {
          commentStart = true;
          if (code.endsWith("*/")) {
            comment = getSingleLineDocComment(code);
            commentStart = false;
            continue;
          }
        }
        if (code.startsWith("//")) {
          comment = code.substring(2).trim();
          commentStart = false;
          continue;
        }
      }
      // 如果进入方法区域，则停止解析
      if (code.contains("(") && code.contains(")") && code.endsWith("{")) {
        return filedCommentMap;
      }
      // 属性判定
      if (code.contains(";") && !code.startsWith("//")) {
        String fileLine = code;
        if (code.contains("//")) {
          String[] arr = code.split("//");
          if (arr.length > 1 && Objects.isNull(comment)) {
            comment = arr[1].trim();
          }
          fileLine = arr[0];
        }
        String fieldName = getFieldName(fileLine);
        filedCommentMap.put(fieldName, comment);
        comment = null;
        commentStart = false;
      }
    }
    return filedCommentMap;
  }

  private static String getFieldName(String s) {
    String[] arr = s.split(" ");
    for (int i = arr.length - 1; i >= 0; i--) {
      String str = arr[i];
      if (!str.isEmpty() && !str.equals(";")) {
        return str.replace(";", "");
      }
    }
    return "";
  }

  /**
   * 获取单行文档注释
   *
   * @param code 单行注释
   * @return 注释内容
   */
  private static String getSingleLineDocComment(String code) {
    // code 应该长这样（/*xxx*/），我们可以先取中间的xxx
    String xxx = code.substring(2, code.length() - 2);
    // xxx 首尾可能还包含 *，需要进一步将 xxx 首尾多余的 * 去掉，剩下的就是注释
    return trim(xxx, Constant.CHAR_STAR).trim();
  }

  /**
   * 去掉首尾指定的符号（该方法参考的是String类中的trim()方法）
   *
   * @param s 字符串
   * @param c 去掉的指定符号
   * @return 不包含首尾指定字符
   */
  public static String trim(String s, char c) {
    int var1 = s.length();
    int var2 = 0;
    char[] var3;
    for (var3 = s.toCharArray(); var2 < var1 && var3[var2] <= c; ++var2) {
    }
    while (var2 < var1 && var3[var1 - 1] <= c) {
      --var1;
    }
    return var2 <= 0 && var1 >= s.length() ? s : s.substring(var2, var1);
  }

  /**
   * 根据驼峰命名获取下划线命名
   *
   * @param javaName java的驼峰命名
   * @return sql中的下划线命名
   */
  private static String getSqlName(String javaName) {
    StringBuilder sqlName = new StringBuilder();
    for (int i = 0; i < javaName.length(); i++) {
      char c = javaName.charAt(i);
      if (Character.isUpperCase(c)) {
        if (i > 0) {
          sqlName.append(Constant.UNDERLINE);
        }
        sqlName.append(Character.toLowerCase(c));
      } else {
        sqlName.append(c);
      }
    }
    return sqlName.toString();
  }

  /**
   * 常量类
   */
  static class Constant {
    private static final String POM_XML = "pom.xml";
    private static final String POINT = ".";
    private static final String LEFT_LINE = "/";
    private static final String POINT_JAVA = ".java";
    private static final String SRC = "src/";
    private static final String SRC_MAIN_JAVA = "src/main/java/";
    private static final String STAR = "*";
    private static final String TABLE = "表";
    private static final String UNDERLINE = "_";
    private static final char CHAR_STAR = '*';
    private static final List<String> TINYINT_WORDS = Arrays.asList("deleted", "sex", "age", "status", "type", "state");
  }

  /**
   * SQL表
   */
  static class Table {

    private String tablePrefix;
    private String columnPrefix;
    private String name;
    private String comment;
    private LinkedList<Column> columns;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    public List<Column> getColumns() {
      return columns;
    }

    public void setColumns(LinkedList<Column> columns) {
      this.columns = columns;
    }

    public String getTablePrefix() {
      return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
      this.tablePrefix = tablePrefix;
    }

    public String getColumnPrefix() {
      return columnPrefix;
    }

    public void setColumnPrefix(String columnPrefix) {
      this.columnPrefix = columnPrefix;
    }

    public String sql() {
      if (Objects.isNull(columns)) {
        columns = new LinkedList<>();
      }
      List<String> lines = new ArrayList<>();
      String tableName;
      if (Objects.nonNull(tablePrefix)) {
        tableName = tablePrefix + name;
      } else {
        tableName = name;
      }
      lines.add(String.format("CREATE TABLE `%s` (", tableName));
      Column key = columns.get(0);
      String type = key.getType();
      if (type.startsWith("tinyint") || type.startsWith("int") || type.startsWith("bigint")) {
        key.setIncreased(true);
        String comment = key.getComment();
        if (Objects.isNull(comment) || comment.isEmpty()) {
          key.setComment("主键");
        }
      }
      for (int i = 0; i < columns.size(); i++) {
        if (columns.get(i).isEffective()) {
          lines.add(columns.get(i).sql(columnPrefix));
        }
      }
      if (key.isEffective()) {
        lines.add(String.format("    PRIMARY KEY (`%s`)", key.getName()));
      }
      StringBuffer sb = new StringBuffer();
      sb.append(") ENGINE=InnoDB");
      if (key.isEffective() && key.isIncreased()) {
        sb.append(" AUTO_INCREMENT=1");
      }
      sb.append(" DEFAULT CHARSET=utf8");
      if (Objects.isNull(comment)) {
        sb.append(" COMMENT='';");
      } else {
        sb.append(" COMMENT='").append(comment).append("';");
      }
      lines.add(sb.toString());
      return String.join("\n", lines);
    }

    public <T, R> Table key(SerializableFunction<T, R> fn) {
      String newKeyName = getSqlName(getFieldName(fn));
      Column find = null;
      for (Column column : columns) {
        if (column.getName().equals(newKeyName)) {
          find = column;
        }
      }
      if (Objects.nonNull(find)) {
        Column copy = find.copy();
        columns.remove(find);
        columns.addFirst(copy);
      }
      return this;
    }

    public <T, R> Table column(SerializableFunction<T, R> fn, String type, String comment, String defaultValue) {
      String fieldName = getFieldName(fn);
      String columnName = getSqlName(fieldName);
      for (Column column : columns) {
        if (column.getName().equals(columnName)) {
          if (Objects.nonNull(type)) {
            column.setType(type);
          }
          if (Objects.nonNull(comment)) {
            column.setComment(comment);
          }
          if (Objects.nonNull(defaultValue)) {
            column.setDefaultValue(defaultValue);
          }
          break;
        }
      }
      return this;
    }

    public <T, R> Table invalid(SerializableFunction<T, R>... fns) {
      if (Objects.nonNull(fns)) {
        for (SerializableFunction<T, R> fn : fns) {
          String fieldName = getFieldName(fn);
          String columnName = getSqlName(fieldName);
          for (Column column : columns) {
            if (column.getName().equals(columnName)) {
              column.setEffective(false);
              break;
            }
          }
        }
      }
      return this;
    }

    private static <T> String getFieldName(SerializableFunction<T, ?> func) {
      // 通过获取对象方法，判断是否存在该方法
      String getter = null;
      try {
        Method method = func.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(Boolean.TRUE);
        // 利用jdk的SerializedLambda 解析方法引用
        SerializedLambda serializedLambda = (SerializedLambda) method.invoke(func);
        getter = serializedLambda.getImplMethodName();
      } catch (Exception e) {
        e.printStackTrace();
      }
      if (Objects.isNull(getter)) {
        return null;
      }
      String fieldName;
      if (getter.startsWith("get")) {
        fieldName = getter.substring(3);
      } else if (getter.startsWith("is")) {
        fieldName = getter.substring(2);
      } else {
        fieldName = getter;
      }
      return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

  }

  /**
   * SQL表字段
   */
  static class Column {

    private String name;
    private String type;
    private String comment;
    private String defaultValue;
    private boolean effective = true;
    private boolean increased;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getComment() {
      return comment;
    }

    public void setComment(String comment) {
      this.comment = comment;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    public boolean isEffective() {
      return effective;
    }

    public void setEffective(boolean effective) {
      this.effective = effective;
    }

    public boolean isIncreased() {
      return increased;
    }

    public void setIncreased(boolean increased) {
      this.increased = increased;
    }

    public Column copy() {
      Column copy = new Column();
      copy.setName(this.name);
      copy.setType(this.getType());
      copy.setComment(this.comment);
      copy.setDefaultValue(this.defaultValue);
      copy.setEffective(this.effective);
      copy.setIncreased(this.increased);
      return copy;
    }

    public String sql(String columnPrefix) {
      StringBuffer sb = new StringBuffer();
      // 字段名称
      sb.append("`");
      if (Objects.nonNull(columnPrefix)) {
        sb.append(columnPrefix);
      }
      sb.append(name).append("`");
      // 字段类型
      sb.append(" ").append(type);
      // 是否自增
      if (increased) {
        sb.append(" NOT NULL AUTO_INCREMENT");
      } else {
//        // 默认值
//        if (Objects.isNull(defaultValue)) {
//          if (type.startsWith("tinyint") || type.startsWith("int") || type.startsWith("bigint")) {
//            sb.append(" DEFAULT 0");
//          } else if (type.startsWith("varchar")) {
//            sb.append(" DEFAULT ''");
//          } else {
//            sb.append(" DEFAULT NULL");
//          }
//        } else {
//          sb.append(" DEFAULT '").append(defaultValue).append("'");
//        }
      }
      // 备注
      if (Objects.isNull(comment)) {
        sb.append(" COMMENT ''");
      } else {
        sb.append(" COMMENT '").append(comment).append("'");
      }
      return "    " + sb + ",";
    }
  }

  @FunctionalInterface
  public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
  }

}
