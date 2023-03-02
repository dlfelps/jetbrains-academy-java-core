import org.hyperskill.hstest.common.ReflectionUtils;
import org.hyperskill.hstest.exception.outcomes.WrongAnswer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class CustomMethod {
  private final String name;
  private final boolean isStatic;
  private final Class<?> returnType;
  private final Class<?>[] args;

  public CustomMethod(String name, boolean isStatic, Class<?> returnType, Class<?>[] args) {
    this.name = name;
    this.isStatic = isStatic;
    this.returnType = returnType;
    this.args = args;
  }

  public String getName() {
    return name;
  }

  public Class<?> getReturnType() {
    return returnType;
  }

  public Class<?>[] getArgs() {
    return args;
  }

  public boolean isStatic() {
    return isStatic;
  }
}

abstract class DelegateSearcher {
  Class<?> original;
  Object instance;
  String name;

  public DelegateSearcher() {
    Initialize();
    for (Class<?> c : ReflectionUtils.getAllClassesFromPackage("collections")) {
      if (c.getSimpleName().equals(name)) {
        original = c;
        Validate();
        return;
      }
    }
    throw new WrongAnswer("Could not Find "+name+" Class");
  }

  abstract void Initialize();
  abstract void Validate();

  protected Object invoke(String methodName, Class<?>[] args, Object... objs) {
    try {
      if (objs.length == 0) {
        Method method = original.getMethod(methodName);
        return method.invoke(instance);
      } else {
        Method method = original.getMethod(methodName, args);
        return ReflectionUtils.invokeMethod(method, instance, objs);
      }
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new WrongAnswer("Could not invoke " +name+"'s "+ methodName + "() method correctly.");
    }
  }
}