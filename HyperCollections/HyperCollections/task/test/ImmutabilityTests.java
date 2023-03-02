import org.hyperskill.hstest.common.ReflectionUtils;
import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.exception.outcomes.UnexpectedError;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

class ImmutableDelegateSearcher extends DelegateSearcher {
  void Initialize() {
    this.name = "ImmutableCollection";
  }

  void Validate() {
    if (!Modifier.isFinal(original.getModifiers())) {
      throw new WrongAnswer(name + " Class must be a final Class");
    }
    if (original.getTypeParameters().length == 0) {
      throw new WrongAnswer(name + " Class must be a Generic Class.");
    }
    for (var c : original.getDeclaredConstructors()) {
      if (Modifier.isPublic(c.getModifiers())) {
        throw new WrongAnswer("None of the " + name + "'s constructors should be public");
      }
    }
    //Name, isStatic, returnType, argsType
    CustomMethod[] methods = new CustomMethod[]{
            new CustomMethod("of", true, original, new Class[]{}),
            new CustomMethod("of", true, original, new Class[]{Object[].class}),
            new CustomMethod("contains", false, boolean.class, new Class[]{Object.class}),
            new CustomMethod("size", false, int.class, new Class[]{}),
            new CustomMethod("isEmpty", false, boolean.class, new Class[]{})
    };

    for (CustomMethod m : methods) {
      Method method;
      try {
        method = original.getMethod(m.getName(), m.getArgs());
      } catch (NoSuchMethodException e) {

        ArrayList<String> names = new ArrayList<>();
        for (Class<?> p : m.getArgs()) {
          if (p.equals(Object.class)) {
            names.add("E");
          } else if (p.equals(Object[].class)) {
            names.add("E[]");
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

      if (m.isStatic() && !Modifier.isStatic(method.getModifiers())) {
        throw new WrongAnswer(name + "'s " + m.getName() + "() method must be static");
      }
      if (!m.isStatic() && Modifier.isStatic(method.getModifiers())) {
        throw new WrongAnswer(name + "'s " + m.getName() + "() method must not be static");
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
      if (e.getCause() != null && e.getCause().getClass().getSimpleName().equals("NullPointerException")) {
        throw new NullPointerException();
      }

      throw new WrongAnswer("Could not invoke " + name + "'s " + methodName + "() method correctly or it is throwing " +
              "wrong exception for a null element or invalid range");
    }
  }
}

class ImmutableBridge extends ImmutableDelegateSearcher {

  public void of() {
    instance = invoke("of", new Class[]{});
  }

  public void of(Object... objs) {
    instance = invoke("of", new Class[]{Object[].class}, new Object[]{objs});
  }

  public int size() {
    return (int) invoke("size", new Class[]{});
  }

  public boolean contains(Object element) {
    return (boolean) invoke("contains", new Class[]{Object.class}, element);
  }

  public boolean isEmpty() {
    return (boolean) invoke("isEmpty", new Class[]{});
  }
}

public class ImmutabilityTests extends StageTest {

  @DynamicTest()
  CheckResult testNPE() {
    ImmutableBridge collection = new ImmutableBridge();
    boolean c = false;
    try {
      collection.of(new Object[]{null});
    } catch (NullPointerException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("ImmutableCollection's of() method should throw a NullPointerException if " +
              "any of provided arguments is equal to 'null'");
    }

    c = false;
    try {
      collection.contains(null);
    } catch (NullPointerException e) {
      c = true;
    }
    if (!c) {
      return CheckResult.wrong("ImmutableCollection's contains() method should throw a NullPointerException if " +
              "any of provided arguments is equal to 'null'");
    }

    return CheckResult.correct();
  }

  //{<Array>, <!contains>}
  Object[][] test_data = {
          {new Object[]{}, new Object[]{"ABC", 1, 'a'}},
          {new Integer[]{1, 2, 3, 4}, new Object[]{"ABC", 5, 'a'}},
          {new Character[]{'a', 'b', 'c'}, new Object[]{"ABC", 1, 'd'}},
          {new String[]{"ABC", "XYZ", "MNO", "PQR"}, new Object[]{"Hello", 1, 'a'}},
  };

  @DynamicTest(data = "test_data", order = 1)
  CheckResult test(Object[] array, Object[] nContains) {

    ImmutableBridge collection = new ImmutableBridge();

    if (array.length == 0) {
      collection.of();
    } else {
      collection.of(array);
    }

    if (collection.size() != array.length) {
      return CheckResult.wrong("Incorrect result from ImmutableCollection's size() method.");
    }

    if (collection.isEmpty() != (array.length == 0)) {
      return CheckResult.wrong("Incorrect result from ImmutableCollection's isEmpty() method.");
    }

    for (Object i : array) {
      if (!collection.contains(i)) {
        return CheckResult.wrong("Incorrect result from ImmutableCollection's contains() method.");
      }
    }

    for (Object i : nContains) {
      if (collection.contains(i)) {
        return CheckResult.wrong("Incorrect result from ImmutableCollection's contains() method.");
      }
    }

    return CheckResult.correct();
  }
}