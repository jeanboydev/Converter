# Converter

JavaBean 转换工具。

## 使用场景

例如下面两个 JavaBean 对象 UserEntity 和 UserModel。

- UserEntity

```java
public class UserEntity {
    private String userName;
    private boolean isCanDo;
    private int ageCount;
    private boolean updatedState;
    private Long createAtTime;
    private List<String> stringList;
    // ...   
}
```

- UserModel

```java
public class UserModel {
    private String name;
    private boolean isCan;
    private int age;
    private boolean updated;
    private Long createAt;
    private List<String> strings;
    // ...   
}
```

UserEntity 可能是服务器端返回的 json 解析后的对象，当客户端需要缓存数据时，如果直接缓存可能字段不是想要的。UserModel 是客户端自定义的缓存对象，这时我们需要将两个对象进行转换。

通常情况下，我们需要写很多转换工具类。

```java
public class UserModelConverter {
    public static UserModel transform(UserEntity source) {
        UserModel product = new UserModel();
        product.setCan(source.isCanDo());
        product.setName(source.getUserName());
        product.setStrings(source.getStringList());
        product.setUpdated(source.isUpdatedState());
        product.setAge(source.getAgeCount());
        product.setCreateAt(source.getCreateAtTime());
        return product;
    }

    public static List<UserModel> transform(List<UserEntity> sourceList) {
        if (sourceList == null) return null;
        List<UserModel> productList = new ArrayList<>();
        for (UserEntity source : sourceList) {
            UserModel produce = transform(source);
            productList.add(produce);
        }
        return productList;
    }
}
```

可以看到代码逻辑很简单，就是各种 get()、set() 方法的操作。

## 使用方式

有了 Converter 我们就不需要再写重复的代码了，只需要使用注解就可自动帮我们生成 Converter 工具类。

- UserEntity

```java
@Source("user") // 这是源，也就是从哪里转换
public class UserEntity {
    @Field(identity = "haha") // 标名字段的唯一性
    private String userName;
    @Field(identity = "can")
    private boolean isCanDo;
    @Field(identity = "age")
    private int ageCount;
    @Field(identity = "updated")
    private boolean updatedState;
    @Field(identity = "createAt")
    private Long createAtTime;
    @Field(identity = "list")
    private List<String> stringList;
    // ...   
}
```

- UserModel

```java
@Product("user") // 这是产品，也就是要转换成的类
public class UserModel {
    @Field(identity = "haha") // 标名字段的唯一性，与源一一对应
    private String name;
    @Field(identity = "can")
    private boolean isCan;
    @Field(identity = "age")
    private int age;
    @Field(identity = "updated")
    private boolean updated;
    @Field(identity = "createAt")
    private Long createAt;
    @Field(identity = "list")
    private List<String> strings;
    // ...   
}
```

在我们项目的 build.gradle 文件中添加依赖。

```groovy
dependencies {
    implementation project(path: ':lib-annotation')
    annotationProcessor project(path: ':lib-compiler')
}
```

最后，找到 Android Studio -> build -> Make Project 执行就生成 Converter 工具类了，在 `/app/build/generated/ap_generated_sources/debug/out` 目录下可以看到生成的工具类。

```java
// 使用
UserModel userModel = UserModelConverter.transform(UserEntity entity);
List<UserModel> userModelList = UserModelConverter.transform(List<UserEntity> entityList);
```