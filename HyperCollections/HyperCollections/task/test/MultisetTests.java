import org.hyperskill.hstest.common.ReflectionUtils;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.exception.outcomes.UnexpectedError;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

class MultisetDelegateSearcher extends DelegateSearcher {
  void Initialize() {
    this.name = "Multiset";
  }

  void Validate() {
    if (original.getTypeParameters().length == 0) {
      throw new WrongAnswer(name + " Class must be a Generic Class.");
    }
    //Name, isStatic, returnType, argsType
    CustomMethod[] methods = new CustomMethod[]{
            new CustomMethod("add", false, void.class, new Class[]{Object.class, int.class}),
            new CustomMethod("add", false, void.class, new Class[]{Object.class}),
            new CustomMethod("contains", false, boolean.class, new Class[]{Object.class}),
            new CustomMethod("count", false, int.class, new Class[]{Object.class}),
            new CustomMethod("elementSet", false, Set.class, new Class[]{}),
            new CustomMethod("remove", false, void.class, new Class[]{Object.class}),
            new CustomMethod("remove", false, void.class, new Class[]{Object.class, int.class}),
            new CustomMethod("setCount", false, void.class, new Class[]{Object.class, int.class}),
            new CustomMethod("setCount", false, void.class, new Class[]{Object.class, int.class, int.class})
    };

    for (CustomMethod m : methods) {
      Method method;
      try {
        method = original.getMethod(m.getName(), m.getArgs());
      } catch (NoSuchMethodException e) {
        ArrayList<String> names = new ArrayList<>();
        for(Class<?> p : m.getArgs()){
          if(p.equals(Object.class)){
            names.add("E");
          }else if(p.equals(Object[].class)){
            names.add("E[]");
          }else{
            names.add(p.getSimpleName());
          }
        }
        if(names.isEmpty()){
          throw new WrongAnswer(name+"'s "+m.getName() + "() method without args is not found");
        }else{
          throw new WrongAnswer(name+"'s "+m.getName() + "() method with args " + names + " is not found");
        }
      }

      if (!method.getReturnType().equals(m.getReturnType())) {
        throw new WrongAnswer(name + "'s " + m.getName() + "() method must return " + m.getReturnType().getSimpleName());
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
      throw new WrongAnswer("Could not invoke " + name + "'s " + methodName + "() method correctly: " + e.getMessage());
    }
  }
}

class MultisetBridge extends MultisetDelegateSearcher {

  public MultisetBridge() {
    try {
      instance = original.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new WrongAnswer("Could not create new " + name + "'s instance");
    }
  }

  public void add(Object element, int occurrences) {
    invoke("add", new Class[]{Object.class, int.class}, element, occurrences);
  }

  public void add(Object element) {
    invoke("add", new Class[]{Object.class}, element);
  }

  public boolean contains(Object element) {
    return (boolean) invoke("contains", new Class[]{Object.class}, element);
  }

  public int count(Object element) {
    return (int) invoke("count", new Class[]{Object.class}, element);
  }

  public Set<?> elementSet() {
    return (Set<?>) invoke("elementSet", new Class[]{});
  }

  public void remove(Object element) {
    invoke("remove", new Class[]{Object.class}, element);
  }

  public void remove(Object element, int occurences) {
    invoke("remove", new Class[]{Object.class, int.class}, element, occurences);
  }

  public void setCount(Object element, int count) {
    invoke("setCount", new Class[]{Object.class, int.class}, element, count);
  }

  public void setCount(Object element, int oldCount, int newCount) {
    invoke("setCount", new Class[]{Object.class, int.class, int.class}, element, oldCount, newCount);
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

public class MultisetTests extends StageTest {
  Pattern pattern = Pattern.compile("\\[\\w+(?:, \\w+)*\\]|\\[\\]");

  String mapToString(Map<Object, Integer> map) {
    List<Object> list = new ArrayList<>();
    for (var it : map.entrySet()) {
      for (int i = 0; i < it.getValue(); i++) {
        list.add(it.getKey());
      }
    }
    return list.toString();
  }

  void Check(MultisetBridge collection, Map<Object, Integer> map, String methodName) {
    if (!pattern.matcher(collection.toString()).matches()) {
      throw new WrongAnswer("Instance of Multiset object should be printed as a list, just like in the example.");
    }
    if (!methodName.equals(""))
      if (!collection.toString().equals(mapToString(map))) {
        throw new WrongAnswer("Multiset's " + methodName + "() method not working correctly");
      }
    if (!map.keySet().equals(collection.elementSet())) {
      throw new WrongAnswer("Incorrect result from Multiset's elementSet() method.");
    }

    for (Object i : new Object[]{1, 'a', "ABC"}) {
      if (collection.contains(i)) {
        throw new WrongAnswer("Incorrect result from Multiset's contains() method.");
      }
    }
    for (Object i : map.keySet()) {
      if (map.containsKey(i) != collection.contains(i)) {
        throw new WrongAnswer("Incorrect result from Multiset's contains() method.");
      }
    }

    for (Object i : new Object[]{1, 'a', "ABC"}) {
      if (collection.count(i) != 0) {
        throw new WrongAnswer("Incorrect result from Multiset's count() method.");
      }
    }
    for (Object i : map.keySet()) {
      if (Optional.ofNullable(map.get(i)).orElse(0) != collection.count(i)) {
        throw new WrongAnswer("Incorrect result from Multiset's count() method.");
      }
    }
  }

  @DynamicTest(order = 1)
  CheckResult testEmpty() {
    MultisetBridge collection = new MultisetBridge();
    Map<Object, Integer> map = new HashMap<>();
    Check(collection, map, "");
    return CheckResult.correct();
  }

  Object[][] test_data = {
          {new Integer[]{2, 3, 4}},
          {new Character[]{'b', 'c', 'd'}},
          {new String[]{"DEF", "XYZ", "MNO"}},
          {new Object[]{2, 'b', "DEF"}}
  };

  @DynamicTest(data = "test_data", order = 2)
  CheckResult testAdd(Object[] array) {
    Map<Object, Integer> map = new HashMap<>();
    MultisetBridge collection = new MultisetBridge();
    for (Object i : array) {
      //Add zero elements
      collection.add(i, 0);
      Check(collection, map, "add");

      //Add one element
      map.put(i, 1);
      collection.add(i);
      Check(collection, map, "add");

      //Add zero elements again
      collection.add(i, 0);
      Check(collection, map, "add");

      //Add 3 more elements
      map.put(i, 4);
      collection.add(i, 3);
      Check(collection, map, "add");

      //Add -1 element
      collection.add(i,-1);
      Check(collection, map, "add");

      //Add 5 more elements
      map.put(i, 9);
      collection.add(i, 5);
      Check(collection, map, "add");
    }

    return CheckResult.correct();
  }


  @DynamicTest(data = "test_data", order = 3)
  CheckResult testRemove(Object[] array) {
    Map<Object, Integer> map = new HashMap<>();
    MultisetBridge collection = new MultisetBridge();

    for (Object i : array) {
      //Remove with 0 occurrences
      collection.remove(i);
      Check(collection, map, "remove");
      collection.remove(i, 5);
      Check(collection, map, "remove");

      //Add 5
      map.put(i, 5);
      collection.add(i, 5);

      //Remove 0
      collection.remove(i,0);
      Check(collection, map, "remove");

      //Remove 1
      map.put(i, 4);
      collection.remove(i);
      Check(collection, map, "remove");

      //Remove 2
      map.put(i, 2);
      collection.remove(i, 2);
      Check(collection, map, "remove");

      //Remove -1
      collection.remove(i, -1);
      Check(collection, map, "remove");

      //Remove last
      map.remove(i);
      collection.remove(i, 2);
      Check(collection, map, "remove");

      //Add 2 more
      map.put(i, 2);
      collection.add(i, 2);

      //Remove more than last
      map.remove(i);
      collection.remove(i, 5);
      Check(collection, map, "remove");

      //Add 5 more
      map.put(i, 5);
      collection.add(i, 5);
    }

    return CheckResult.correct();
  }

  @DynamicTest(data = "test_data", order = 4)
  CheckResult testSetCount(Object[] array) {
    Map<Object, Integer> map = new HashMap<>();
    MultisetBridge collection = new MultisetBridge();

    for (Object i : array) {
      //Set count with 0
      collection.setCount(i,5);
      Check(collection, map, "setCount");
      collection.setCount(i, 2,5);
      Check(collection, map, "setCount");

      //Add 3
      map.put(i, 3);
      collection.add(i, 3);

      //setCount, wrong oldCount
      collection.setCount(i,2, 5);
      Check(collection, map, "setCount");

      //setCount, -1
      collection.setCount(i,-1);
      Check(collection, map, "setCount");

      //setCount, -1 newCount
      collection.setCount(i,3,-1);
      Check(collection, map, "setCount");

      //setCount, from 3 to 5
      map.put(i,5);
      collection.setCount(i,3,5);
      Check(collection, map, "setCount");

      //setCount, to 1
      map.put(i,1);
      collection.setCount(i,1);
      Check(collection, map, "setCount");

      //setCount, to 0
      map.remove(i);
      collection.setCount(i,0);
      Check(collection, map, "setCount");

      //Add 3 more
      map.put(i, 3);
      collection.add(i, 3);

      //setCount, to 3 as well
      collection.setCount(i,3,3);
      Check(collection, map, "setCount");

      collection.setCount(i,3);
      Check(collection, map, "setCount");

      //setCount, to 0 newCount
      map.remove(i);
      collection.setCount(i,3,0);
      Check(collection, map, "setCount");

      //Add 3 more
      map.put(i, 3);
      collection.add(i, 3);
    }
    return CheckResult.correct();
  }
}