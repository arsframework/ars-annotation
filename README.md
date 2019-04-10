# ars-annotation
Arsframework annotation模块采用纯Java语言编写，通过在代码编译期间动态修改语法树的方式增加针对方法输入参数的校验逻辑。
模块提供了多个用于参数校验的注解，通过这些注解可以对参数校验进行静态配置，使其在静态方法、构造方法、业务处理等不方便使用其他框架的地方编写方法参数校验更新方便。

## 1 环境依赖
JDK1.8+

## 2 部署配置
在Maven配置中添加如下依赖：
```
<dependency>
    <groupId>com.arsframework</groupId>
    <artifactId>ars-annotation</artifactId>
    <version>1.3.2</version>
</dependency>
```

## 3 功能描述
在该包提供的所有注解中，除了```@Global```注解以外的注解均可作用在类（含枚举）、方法（含构造方法、实例方法、静态方法）、方法参数上；
```@Global```注解只能作用于类和方法上，并为其作用范围内的其他注解提供全局配置。

#### 注意:
- 参数校验逻辑将动态添加到方法逻辑代码块前面；
- 作用在类上的注解将对整个类的所有方法参数生效；
- 作用在方法上的注解将对整个方法的所有参数生效；
- 作用在方法参数上的注解将只对指定参数生效；
- 注解处理器将按照方法参数、方法、类的顺序查找并使用注解；
- 如果方法没有参数或参数类型不被注解所支持，则注解自动被忽略；
  
### 3.1 @Global 注解
该注解可作用在类、方法上，用于对其他参数校验注解进行全局配置，其作用范围及优先使用顺序同注解一致。该注解需配合其他注解使用，否则被自动忽略。
参数校验注解的配置优先于全局注解的配置，即如果参数校验注解配置发生改变则使用参数校验注解配置，否则使用全局注解配置（如果没有全局注解则使用参数校验注解配置）。

#### 3.1.1 注解方法
- ```String exception()```方法统一设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.1.2 示例
```
import com.arsframework.annotation.*;

@Format("1")
@Global(exception = "java.lang.IllegalStateException")
public class Test {
    /**
     * @param s 使用java.lang.IllegalStateException
     */
    public static void a(String s) {
    }

    /**
     * @param s 使用java.lang.RuntimeException
     */
    @Global(exception = "java.lang.RuntimeException")
    public static void b(String s) {
    }

    /**
     * @param s 使用java.lang.RuntimeException
     */
    public static void c(@Format(value = "z", exception = "java.lang.RuntimeException") String s) {
    }
}
```

### 3.2 @Nonnull 注解
该注解用于对参数进行非Null（与空字符串进行区分）校验，适用于除基本数据类型以外的所有类型参数。

#### 3.2.1 注解方法
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must not be null```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.2.2 示例
```
import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s 生效
     * @param i 忽略
     * @param n 生效
     */
    @Nonnull
    public static void b(String s, int i, Integer n) {
    }
}
```
### 3.3 @Nonempty 注解
该注解除了对参数进行非Null校验以外，还将对```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```以及数组类型参数进行非空校验。

#### 3.3.1 注解方法
- ```boolean blank()```方法设置```java.lang.CharSequence```类型参数是否允许空白，默认为```true```。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must not be empty```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.3.2 示例
```
import java.util.Map;
import java.util.Collection;

import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s          生效
     * @param i          忽略
     * @param n          生效
     * @param array      生效
     * @param map        生效
     * @param collection 生效
     */
    @Nonempty
    public static void b(String s, int i, Integer n, Object[] array, Map map, Collection collection) {
    }
}
```

### 3.4 @Format 注解
该注解用于对```java.lang.Number```、```java.lang.CharSequence```和```byte```、```char```、```int```、```short```、```float```、```long```、```double```
及其包装类型参数在数据不为Null时作数据格式校验。

#### 3.4.1 注解方法
- ```String value()```方法设置数据格式匹配模式（正则表达式）。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 匹配模式)```的方式对其进行格式化，
默认为```The format of argument '%s' must be matched for '%s'```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.4.2 示例
```
import java.util.Map;
import java.util.Collection;

import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s          生效
     * @param i          生效
     * @param n          生效
     * @param array      忽略
     * @param map        忽略
     * @param collection 忽略
     */
    @Format("123")
    public static void b(String s, int i, Integer n, Object[] array, Map map, Collection collection) {
    }
}
```

### 3.5 @Min 注解
该注解用于参数值不为Null时的最小值校验，适用于数组、```java.lang.Enum```、```java.util.Date```、```java.math.BigInteger```、```java.math.BigDecimal```、
```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```和```byte```、```char```、```int```、```short```、```float```、
```long```、```double```及其包装类型参数。针对```java.lang.Enum```类型参数将调用```ordinal()```方法比较；针对```java.util.Date```类型参数将调用
```getTime()```方法比较；针对数组、```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```类型参数将对参数长度大小比较。

#### 3.5.1 注解方法
- ```long value()```方法设置参数最小值。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 参数最小值)```的方式对其进行格式化，
默认为```The size of argument '%s' must be greater than or equal to %d```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.5.2 示例
```
import com.arsframework.annotation.*;

public class Test {

    /**
     * @param s     生效
     * @param i     生效
     * @param array 生效
     * @param t     忽略
     */
    @Min(100)
    public static void b(String s, Integer i, Object[] array, Test t) {
    }
}
```

### 3.6 @Max 注解
该注解用于参数值不为Null时的最大值校验，适用于数组、```java.lang.Enum```、```java.util.Date```、```java.math.BigInteger```、```java.math.BigDecimal```、
```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```和```byte```、```char```、```int```、```short```、```float```、
```long```、```double```及其包装类型参数。针对```java.lang.Enum```类型参数将调用```ordinal()```方法比较；针对```java.util.Date```类型参数将调用
```getTime()```方法比较；针对数组、```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```类型参数将对参数长度大小比较。

#### 3.6.1 注解方法
- ```long value()```方法设置参数最大值。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 参数最大值)```的方式对其进行格式化，
默认为```The size of argument '%s' must be less than or equal to %d```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.6.2 示例
```
import com.arsframework.annotation.*;

public class Test {

    /**
     * @param s     生效
     * @param i     生效
     * @param array 生效
     * @param t     忽略
     */
    @Max(100)
    public static void b(String s, Integer i, Object[] array, Test t) {
    }
}
```

### 3.7 @Size 注解
该注解用于当参数值不为Null时的参数值范围校验，适用于数组、```java.lang.Enum```、```java.util.Date```、```java.math.BigInteger```、```java.math.BigDecimal```、
```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```和```byte```、```char```、```int```、```short```、```float```、
```long```、```double```及其包装类型参数。针对```java.lang.Enum```类型参数将调用```ordinal()```方法比较；针对```java.util.Date```类型参数将调用
```getTime()```方法比较；针对数组、```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```类型参数将对参数长度大小比较。

#### 3.7.1 注解方法
- ```long min()```方法设置参数最小值，默认为```Long.MIN_VALUE```。
- ```long max()```方法设置参数最大值，默认为```Long.MAX_VALUE```。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 参数最小值、参数最大值)```的方式对其进行格式化，
默认为```The size of argument '%s' must be in interval [%d, %d]```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.7.2 示例
```
import com.arsframework.annotation.*;

public class Test {

    /**
     * @param s     生效
     * @param i     生效
     * @param array 生效
     * @param t     忽略
     */
    @Size(min = 1, max = 100)
    public static void b(String s, Integer i, Object[] array, Test t) {
    }
}
```

### 3.8 @Option 注解
该注解用于当参数值不为Null时的参数值选项校验，适用于数组、```java.lang.Enum```、```java.util.Date```、```java.math.BigInteger```、```java.math.BigDecimal```、
```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```和```byte```、```char```、```int```、```short```、```float```、
```long```、```double```及其包装类型参数。针对```java.lang.Enum```类型参数将调用```ordinal()```方法比较；针对```java.util.Date```类型参数将调用
```getTime()```方法比较；针对数组、```java.lang.CharSequence```、```java.util.Map```、```java.util.Collection```类型参数将对参数长度大小比较。

#### 3.8.1 注解方法
- ```long[] value()```方法设置参数值选项数组，如果参数值选项数组为空则自动忽略该注解。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 参数选项数组字符串)```的方式对其进行格式化，
默认为```The value of argument '%s' must be in option %s```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.8.2 示例
```
import com.arsframework.annotation.*;

public class Test {

    /**
     * @param s     生效
     * @param i     生效
     * @param array 生效
     * @param t     忽略
     */
    @Option({1, 2, 3})
    public static void b(String s, Integer i, Object[] array, Test t) {
    }
}
```

### 3.9 @Lt 注解
该注解用于当两个参数值不为Null时的小于校验，适用于```java.lang.Comparable```和```byte```、```char```、```int```、```short```、```float```、```long```、
```double```及其包装类型参数。

#### 3.9.1 注解方法
- ```String value()```方法设置被比较参数名称。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 被比较参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must be less than argument '%s'```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.9.2 示例
```
import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s 忽略
     * @param i 忽略
     * @param n 生效
     */
    @Lt("i")
    public static void b(String s, int i, Integer n) {
    }
}
```

### 3.10 @Le 注解
该注解用于当两个参数值不为Null时的小于或等于校验，适用于```java.lang.Comparable```和```byte```、```char```、```int```、```short```、```float```、```long```、
```double```及其包装类型参数。

#### 3.10.1 注解方法
- ```String value()```方法设置被比较参数名称。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 被比较参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must be less than or equal to argument '%s'```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.10.2 示例
```
import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s 忽略
     * @param i 忽略
     * @param n 生效
     */
    @Le("i")
    public static void b(String s, int i, Integer n) {
    }
}
```

### 3.11 @Gt 注解
该注解用于当两个参数值不为Null时的大于校验，适用于```java.lang.Comparable```和```byte```、```char```、```int```、```short```、```float```、```long```、
```double```及其包装类型参数。

#### 3.11.1 注解方法
- ```String value()```方法设置被比较参数名称。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 被比较参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must be greater than argument '%s'```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.11.2 示例
```
import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s 忽略
     * @param i 忽略
     * @param n 生效
     */
    @Gt("i")
    public static void b(String s, int i, Integer n) {
    }
}
```

### 3.12 @Ge 注解
该注解用于当两个参数值不为Null时的大于或等于校验，适用于```java.lang.Comparable```和```byte```、```char```、```int```、```short```、```float```、```long```、
```double```及其包装类型参数。

#### 3.12.1 注解方法
- ```String value()```方法设置被比较参数名称。
- ```String message()```方法设置参数校验异常信息，其内部采用```String.format(message, 参数名称, 被比较参数名称)```的方式对其进行格式化，
默认为```The value of argument '%s' must be greater than or equal to argument '%s'```。
- ```String exception()```方法设置参数校验失败异常类，默认为```java.lang.IllegalArgumentException```。

#### 3.12.2 示例
```
import com.arsframework.annotation.*;

public class Test {
    /**
     * @param s 忽略
     * @param i 忽略
     * @param n 生效
     */
    @Ge("i")
    public static void b(String s, int i, Integer n) {
    }
}
```

### 3.13 @Assert 注解
该注解已不建议使用，请参考```@Nonnull```、```@Nonempty```注解。

### 3.14 @Nonblank 注解
该注解已不建议使用，请参考```@Nonempty```注解。

## 4 版本更新日志
### v1.3.2
1. 修复重复校验问题
2. 修复```@Format```注解针对```java.math.BigInteger```、```java.math.BigDecimal```类型参数不生效问题

### v1.3.3
1. 解决与```Lombok```包冲突问题
