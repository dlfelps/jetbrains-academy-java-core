import org.hyperskill.hstest.common.ReflectionUtils;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.exception.outcomes.UnexpectedError;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

class BiMapDelegateSearcher extends DelegateSearcher {
  void Initialize() {
    this.name = "BiMap";
  }

  void Validate() {
    if (original.getTypeParameters().length == 0) {
      throw new WrongAnswer(name + " Class must be a Generic Class.");
    }

    try {
      Constructor<?> publicConstr = original.getDeclaredConstructor();
      if (!Modifier.isPublic(publicConstr.getModifiers())) {
        throw new WrongAnswer(name + "'s constructor with no args should be public");
      }
    } catch (NoSuchMethodException e) {
      throw new WrongAnswer(name + "'s public constructor with no args is not found");
    }

    //Name, isStatic, returnType, argsType
    CustomMethod[] methods = new CustomMethod[]{
            new CustomMethod("forcePut", false, Object.class, new Class[]{Object.class, Object.class}),
            new CustomMethod("inverse", false, original, new Class[]{}),
            new CustomMethod("put", false, Object.class, new Class[]{Object.class, Object.class}),
            new CustomMethod("putAll", false, void.class, new Class[]{Map.class}),
            new CustomMethod("values", false, Set.class, new Class[]{})
    };

    for (CustomMethod m : methods) {
      Method method;
      try {
        method = original.getMethod(m.getName(), m.getArgs());
      } catch (NoSuchMethodException e) {
        ArrayList<String> names = new ArrayList<>();
        for (Class<?> p : m.getArgs()) {
          if (p.equals(Object.class)) {
            names.add("K");
            names.add("V");
            break;
          } else {
            names.add(p.getSimpleName());
          }
        }
        if (names.isEmpty()) {
          throw new WrongAnswer(name + "'s " + m.getName() + "() method without args is not found");
        } else {
          throw new WrongAnswer(name + "'s " + m.getName() + "() method with args " + names + " is not found");
        }
      }

      if (!method.getReturnType().equals(m.getReturnType())) {
        if (m.getReturnType().equals(Object.class)) {
          throw new WrongAnswer(name + "'s " + m.getName() + "() method must return V");
        } else {
          throw new WrongAnswer(name + "'s " + m.getName() + "() method must return " + m.getReturnType().getSimpleName());
        }
      }

      if (!Modifier.isPublic(method.getModifiers())) {
        throw new WrongAnswer(name + "'s " + m.getName() + "() method must be public");
      }
    }
  }

  @Override
  protected Object invoke(String methodName, Class<?>[] args, Object... objs) {
    try {
      if (objs.length == 0) {
        Method method = original.getMethod(methodName);
        return method.invoke(instance);
      } else {
        Method method = original.getMethod(methodName, args);
        return ReflectionUtils.invokeMethod(method, instance, objs);
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | UnexpectedError e) {
      if (e.getCause() != null && e.getCause().getClass().getSimpleName().equals("IllegalArgumentException")) {
        throw new IllegalArgumentException();
      }
      throw new WrongAnswer("Could not invoke " + name + "'s " + methodName + "() method correctly");
    }
  }
}

class BiMapBridge extends BiMapDelegateSearcher {
  public BiMapBridge() {
    try {
      instance = original.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new WrongAnswer("Could not create new " + name + "'s instance");
    }
  }

  public Object put(Object key, Object value) {
    return invoke("put", new Class[]{Object.class, Object.class}, key, value);
  }

  public void putAll(Map<Object, Object> m) {
    invoke("putAll", new Class[]{Map.class}, m);
  }

  public Set<?> values() {
    return (Set<?>) invoke("values", new Class[]{});
  }

  public Object forcePut(Object key, Object value) {
    return invoke("forcePut", new Class[]{Object.class, Object.class}, key, value);
  }

  public Object inverse() {
    return invoke("inverse", new Class[]{});
  }

  public String toString() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintStream old = System.out;
    System.setOut(ps);

    System.out.print(instance);

    System.out.flush();
    System.setOut(old);
    return baos.toString();
  }
}

public class BiMapTests extends StageTest {
  Pattern pattern = Pattern.compile("\\{(\\w+=\\w+)+(?:, (\\w+=\\w+)+)*\\}|\\{\\}");

  Map<Object, Object> collToMap(Object s, Class<?> k, Class<?> v) {
    String[] elements = s.toString().replaceAll("\\s|\\{|\\}", "").split(",");
    Map<Object, Object> map = new HashMap<>();
    if (elements[0].equals("")) {
      return map;
    }
    for (String e : elements) {
      String[] kv = e.split("=");
      Object kObj = null, vObj = null;
      switch (k.getSimpleName()) {
        case "Character" -> kObj = kv[0].charAt(0);
        case "Integer" -> kObj = Integer.parseInt(kv[0]);
        case "String" -> kObj = kv[0];
      }
      switch (v.getSimpleName()) {
        case "Character" -> vObj = kv[1].charAt(0);
        case "Integer" -> vObj = Integer.parseInt(kv[1]);
        case "String" -> vObj = kv[1];
      }
      map.put(kObj, vObj);
    }
    return map;
  }

  void Check(BiMapBridge collection, Map<Object, Object> map,
             Map<Object, Object> inverse, String methodName, Class<?> k, Class<?> v) {
    if (!pattern.matcher(collection.toString()).matches()) {
      throw new WrongAnswer("Instance of BiMap object should be printed as a map, just like in the example.");
    }
    if (!pattern.matcher(collection.inverse().toString()).matches()) {
      throw new WrongAnswer("Inverted instance of BiMap object should be printed as a map, just like in the example.");
    }

    if (!methodName.equals("")) {
      if (!collToMap(collection, k, v).equals(map)) {
        System.out.println(collToMap(collection, k, v));
        System.out.println(map);
        throw new WrongAnswer("BiMap's " + methodName + "() method not working correctly");
      }
    }

    if (!collToMap(collection.inverse(), v, k).equals(inverse)) {
      System.out.println(collToMap(collection.inverse(), v, k));
      System.out.println(inverse);
      throw new WrongAnswer("BiMap's inverse() method not working correctly");
    }

    if (!collection.values().equals(inverse.keySet())) {
      throw new WrongAnswer("Incorrect result from BiMap's values() method.");
    }
  }

  @DynamicTest(order = 1)
  CheckResult test_empty() {
    BiMapBridge collection = new BiMapBridge();

    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();

    //Check_empty
    Check(collection, map, inverse, "", Object.class, Object.class);
    return CheckResult.correct();
  }

  Object[][] test_data = {
          {new Object[][]{{'a', 3}, {'b', 4}, {'c', 5}, {'d', 6}},
                  Character.class, Integer.class},
          {new Object[][]{{'a', "ABC"}, {'b', "BCD"}, {'c', "CDE"}, {'d', "DEF"}}, Character.class, String.class},
          {new Object[][]{{1, 3}, {2, 4}, {3, 5}, {4, 6}}, Integer.class, Integer.class}
  };

  @DynamicTest(data = "test_data", order = 2)
  CheckResult test_put(Object[][] data, Class<?> k, Class<?> v) {
    BiMapBridge collection = new BiMapBridge();

    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();
    //Check_put
    for (Object[] d : data) {
      collection.put(d[0], d[1]);
      map.put(d[0], d[1]);
      inverse.put(d[1], d[0]);

      Check(collection, map, inverse, "put", k, v);
    }

    return CheckResult.correct();
  }

  @DynamicTest(order = 3)
  CheckResult test_put_illegal_key() {
    BiMapBridge collection = new BiMapBridge();

    collection.put('a', 3);
    collection.put('b', 4);

    boolean c = false;
    try {
      collection.put('a', 5);
    } catch (IllegalArgumentException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("BiMap's put() method should throw an IllegalArgumentException if " +
              "a key or a value already exists in the BiMap");
    }

    return CheckResult.correct();
  }

  @DynamicTest(order = 4)
  CheckResult test_put_illegal_value() {
    BiMapBridge collection = new BiMapBridge();

    collection.put('a', 3);
    collection.put('b', 4);

    boolean c = false;
    try {
      collection.put('c', 4);
    } catch (IllegalArgumentException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("BiMap's put() method should throw an IllegalArgumentException if " +
              "a key or a value already exists in the BiMap");
    }

    return CheckResult.correct();
  }

  Object[][] test_maps = {
          {
                  new Map[]{
                          Map.of('a', 3, 'b', 4, 'c', 5, 'd', 6),
                          Map.of('e', 7, 'f', 8, 'g', 9, 'h', 10)
                  },
                  Character.class, Integer.class
          },
          {
                  new Map[]{
                          Map.of('a', "ABC", 'b', "BCD", 'c', "CDE", 'd', "DEF"),
                          Map.of('e', "EFG", 'f', "FGH", 'g', "GHK", 'h', "HKL")
                  },
                  Character.class, String.class
          },
          {
                  new Map[]{
                          Map.of(1, 3, 2, 4, 3, 5, 4, 6),
                          Map.of(5, 7, 6, 8, 7, 9, 8, 10)
                  },
                  Integer.class, Integer.class
          },
  };

  @DynamicTest(data = "test_maps", order = 5)
  CheckResult test_putAll(Map<Object, Object>[] maps, Class<?> k, Class<?> v) {
    BiMapBridge collection = new BiMapBridge();

    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();

    //Check_putAll
    for (Map<Object, Object> m : maps) {
      for (Map.Entry<Object, Object> e : m.entrySet()) {
        map.put(e.getKey(), e.getValue());
        inverse.put(e.getValue(), e.getKey());
      }

      collection.putAll(m);

      Check(collection, map, inverse, "putAll", k, v);
    }

    return CheckResult.correct();
  }

  @DynamicTest(order = 6)
  CheckResult test_putAll_illegal_values() {
    BiMapBridge collection = new BiMapBridge();

    collection.putAll(Map.of('a', 3, 'b', 4, 'c', 5, 'd', 6));

    boolean c = false;
    try {
      collection.putAll(Map.of('e', 5, 'f', 6));
    } catch (IllegalArgumentException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("BiMap's putAll() method should throw an IllegalArgumentException if " +
              "a key or a value already exists in the BiMap");
    }

    return CheckResult.correct();
  }

  @DynamicTest(order = 7)
  CheckResult test_putAll_illegal_keys() {
    BiMapBridge collection = new BiMapBridge();

    collection.putAll(Map.of('a', 3, 'b', 4, 'c', 5, 'd', 6));

    boolean c = false;
    try {
      collection.putAll(Map.of('a', 7, 'b', 8));
    } catch (IllegalArgumentException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("BiMap's putAll() method should throw an IllegalArgumentException if " +
              "a key or a value already exists in the BiMap");
    }

    return CheckResult.correct();
  }

  @DynamicTest(data = "test_data", order = 8)
  CheckResult test_forcePut_as_put(Object[][] data, Class<?> k, Class<?> v) {
    BiMapBridge collection = new BiMapBridge();

    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();

    //Check_forcePut
    for (Object[] d : data) {
      if (map.containsKey(d[0])) {
        Object value = map.get(d[0]);
        map.remove(d[0]);
        inverse.remove(value);
      }
      map.put(d[0], d[1]);
      inverse.put(d[1], d[0]);

      collection.forcePut(d[0], d[1]);

      Check(collection, map, inverse, "forcePut", k, v);
    }

    return CheckResult.correct();
  }

  @DynamicTest(order = 9)
  CheckResult test_forcePut() {

    Object[][] data = new Object[][]{{'a', 3}, {'b', 4}, {'c', 5}, {'d', 6}, {'e', 7},
            {'f', 6}, {'g', 7}, {'h', 6},
            {'a', 8}, {'b', 9}, {'a', 10},
            {'c', 5}};

    BiMapBridge collection = new BiMapBridge();

    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();

    //Check_forcePut
    for (Object[] d : data) {
      Object value, key;

      value = map.get(d[0]);
      if(value!=null) {
        map.remove(d[0]);
        inverse.remove(value);
      }
      key = inverse.get(d[1]);
      if(key!=null) {
        map.remove(key);
        inverse.remove(d[1]);
      }

      map.put(d[0], d[1]);
      inverse.put(d[1], d[0]);

      collection.forcePut(d[0], d[1]);

      Check(collection, map, inverse, "forcePut", Character.class, Integer.class);
    }

    return CheckResult.correct();
  }

//
//  Object[][] test_combined = {
//          {
//                  new Object[][]{{'a', 3}, {'b', 4}, {'c', 5}, {'d', 6}, {'a', 7}, {'b', 3}, {'e', 5}, {'f', 6}},
//                  new Map[]{
//                          Map.of('a', 3, 'b', 4, 'c', 5, 'd', 6),
//                          Map.of('a', 7, 'e', 4, 'f', 8, 'd', 7),
//                          Map.of('a', 7, 'c', 2, 'f', 5, 'g', 3)
//                  },
//                  Character.class, Integer.class
//          },
//          {
//                  new Object[][]{{'a', "ABC"}, {'b', "BCD"}, {'c', "CDE"}, {'d', "DEF"}, {'a', "EFG"}, {'b', "ABD"},
//                          {'e',
//                                  "CDE"}, {'f', "DEF"}},
//                  new Map[]{
//                          Map.of('a', "ABC", 'b', "BCD", 'c', "CDE", 'd', "DEF"),
//                          Map.of('a', "EFG", 'e', "BCD", 'f', "FGH", 'd', "EFG"),
//                          Map.of('a', "EFG", 'c', "ABB", 'f', "CDE", 'g', "ABC")
//                  },
//                  Character.class, String.class
//          },
//          {
//                  new Object[][]{{1, 3}, {2, 4}, {3, 5}, {4, 6}, {1, 7}, {2, 3}, {5, 5}, {6, 6}},
//                  new Map[]{
//                          Map.of(1, 3, 2, 4, 3, 5, 4, 6),
//                          Map.of(1, 7, 5, 4, 6, 8, 4, 7),
//                          Map.of(1, 7, 3, 2, 6, 5, 7, 3)
//                  },
//                  Integer.class, Integer.class
//          },
//  };
//
//  @DynamicTest(data = "test_combined", order = 5, repeat = 5)
//  CheckResult test_everything(Object[][] data, Map<Object, Object>[] maps, Class<?> k, Class<?> v) {
//    BiMapBridge collection = new BiMapBridge();
//
//    Map<Object, Object> map = new HashMap<>(), inverse = new HashMap<>();
//
//    int putCase = 0; // 8
//    int mapCase = 0; // 3
//
//    Random rd = new Random();
//
//    //Check
//    while (putCase != 8 || mapCase != 3) {
//      boolean ch = rd.nextBoolean();
//      String methodName = "";
//      if (ch && putCase != 8 || mapCase == 3) {
//        ch = rd.nextBoolean();
//        Object kObj = data[putCase][0], vObj = data[putCase][1];
//
//        if (ch) {
//          methodName = "put";
//
//          map.put(kObj, vObj);
//          inverse.put(vObj, kObj);
//          collection.put(kObj, vObj);
//        } else {
//          methodName = "forcePut";
//
//          if (map.containsKey(kObj)) {
//            Object value = map.get(kObj);
//            map.remove(kObj);
//            inverse.remove(value);
//          }
//          map.put(kObj, vObj);
//          inverse.put(vObj, kObj);
//          collection.forcePut(kObj, vObj);
//        }
//        putCase++;
//      } else {
//        methodName = "putAll";
//
//        for (Map.Entry<Object, Object> e : maps[mapCase].entrySet()) {
//          map.put(e.getKey(), e.getValue());
//          inverse.put(e.getValue(), e.getKey());
//        }
//
//        collection.putAll(maps[mapCase]);
//        mapCase++;
//      }
//      Check(collection, map, inverse, methodName, k, v);
//    }
//    return CheckResult.correct();
//  }
}