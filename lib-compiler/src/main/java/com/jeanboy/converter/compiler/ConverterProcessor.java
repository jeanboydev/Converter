package com.jeanboy.converter.compiler;


import com.jeanboy.converter.annotation.Field;
import com.jeanboy.converter.annotation.Product;
import com.jeanboy.converter.annotation.Source;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * @author caojianbo
 * @since 2020/1/16 17:59
 */
public class ConverterProcessor extends AbstractProcessor {

    private static final String PREFIX_GET = "get";
    private static final String PREFIX_SET = "set";
    private static final String PREFIX_IS = "is";
    private static final String SUFFIX_CLASS = "Converter";
    private static final String SUFFIX_PACKAGE = ".converter";
    private static final String METHOD_NAME = "transform";

    private Messager messager; // 输出日志

    /**
     * [classIdentity, [sourceElement, produceElement]]
     */
    private Map<String, TypeElement[]> classTree = new HashMap<>();
    /**
     * [classIdentity, [fieldIdentity, [sourceElement, produceElement]]]
     */
    private Map<String, Map<String, VariableElement[]>> fieldTree = new HashMap<>();
    /**
     * [classIdentity, [methodIdentity, [getXXX, setXXX]]]
     */
    private Map<String, Map<String, String[]>> methodTree = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "------TransformProcessor init------");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.NOTE, "------TransformProcessor process------");

        Set<? extends Element> fromElements = roundEnv.getElementsAnnotatedWith(Source.class);
        Set<? extends Element> toElements = roundEnv.getElementsAnnotatedWith(Product.class);

        for (TypeElement element : ElementFilter.typesIn(fromElements)) { // 元素为 class/interface 类型
            // 返回指定类型注解
            String value = element.getAnnotation(Source.class).value();
            messager.printMessage(Diagnostic.Kind.WARNING, "Source->" + value);
            TypeElement[] elements = classTree.get(value);
            if (elements == null) {
                elements = new TypeElement[2];
            }
            elements[0] = element;
            classTree.put(value, elements);
        }

        for (TypeElement element : ElementFilter.typesIn(toElements)) {
            String value = element.getAnnotation(Product.class).value();
            messager.printMessage(Diagnostic.Kind.WARNING, "Product->" + value);
            TypeElement[] elements = classTree.get(value);
            if (elements == null) {
                elements = new TypeElement[2];
            }
            elements[1] = element;
            classTree.put(value, elements);
        }

        for (String target : classTree.keySet()) {
            messager.printMessage(Diagnostic.Kind.WARNING, "class target->" + target);
            TypeElement[] elements = classTree.get(target);
            if (elements[0] != null && elements[1] != null) {
                TypeElement source = elements[0];
                TypeElement product = elements[1];

                String[] sourceValue = parseField(target, source, true);
                String sourcePackage = sourceValue[0];
                String sourceClass = sourceValue[1];

                String[] productValue = parseField(target, product, false);
                String productPackage = productValue[0];
                String productClass = productValue[1];

                createClass(sourcePackage, sourceClass, productPackage, productClass);
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationSet = new HashSet<>();
        annotationSet.add(Source.class.getCanonicalName());
        annotationSet.add(Product.class.getCanonicalName());
        annotationSet.add(Field.class.getCanonicalName());
        return annotationSet;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


    private String[] parseField(String classIdentity, TypeElement element, boolean isSource) {
        messager.printMessage(Diagnostic.Kind.WARNING,
                "parseField------------->" + classIdentity + "--type--" + (isSource ? PREFIX_GET :
                        PREFIX_SET));
        String packageName = ""; // 包名
        String className = element.getSimpleName().toString(); // 类名
        if (element.getKind() == ElementKind.CLASS) { // 元素为 class 类型
            // 返回封装此元素的元素，比如类，包等
            PackageElement packageElement = (PackageElement) element.getEnclosingElement();
            packageName = packageElement.getQualifiedName().toString();

            // 如果封装元素是类，则返回类内部定义的所有变量，方法等
            List<? extends Element> childElements = element.getEnclosedElements();

            messager.printMessage(Diagnostic.Kind.WARNING, "parse field----------------->");
            for (VariableElement field : ElementFilter.fieldsIn(childElements)) { // 元素为 field 类型
                Field annotation = field.getAnnotation(Field.class);
                String fieldIdentity = annotation.identity();
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "\t|-fieldIdentity---->" + fieldIdentity);

                Map<String, VariableElement[]> fieldMap = fieldTree.get(classIdentity);
                if (fieldMap == null) {
                    fieldMap = new HashMap<>();
                }

                VariableElement[] fieldArray = fieldMap.get(fieldIdentity);
                if (fieldArray == null) {
                    fieldArray = new VariableElement[2];
                }

                fieldArray[isSource ? 0 : 1] = field;
                fieldMap.put(fieldIdentity, fieldArray);
                fieldTree.put(classIdentity, fieldMap);
            }


            messager.printMessage(Diagnostic.Kind.WARNING, "parse method-------------->");
            Map<String, VariableElement[]> fieldMap = fieldTree.get(classIdentity);
            if (fieldMap != null) {
                for (String fieldIdentity : fieldMap.keySet()) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                            "\t|-fieldIdentity->" + fieldIdentity);
                    VariableElement[] fieldArray = fieldMap.get(fieldIdentity);
                    VariableElement fieldElement = fieldArray[isSource ? 0 : 1];
                    String fieldName = fieldElement.getSimpleName().toString(); // 获取成员变量字段名
                    messager.printMessage(Diagnostic.Kind.WARNING,
                            "\t|-fieldName->" + fieldName);

                    TypeKind typeKind = fieldElement.asType().getKind();

                    /*
                     * can:{
                     *      set: setUpdate
                     *      get: isUpdate // 编译器特殊处理
                     * }
                     *
                     * isCan:{
                     *      set: setCan // 编译器特殊处理
                     *      get: isCan // 编译器特殊处理
                     * }
                     */
                    String matchKey = fieldName.toLowerCase();
                    if (isSource) { // 查找 get 方法
                        String prefix = PREFIX_GET;
                        if (TypeKind.BOOLEAN == typeKind) {
                            prefix = matchKey.startsWith(PREFIX_IS) ? "" : PREFIX_IS;
                        }
                        matchKey = prefix + matchKey;
                    } else { // 查找 set 方法
                        if (TypeKind.BOOLEAN == typeKind) {
                            matchKey = matchKey.replace(PREFIX_IS, "");
                        }
                        matchKey = PREFIX_SET + matchKey;
                    }

                    for (Element method : ElementFilter.methodsIn(childElements)) {
                        String methodName = method.getSimpleName().toString();
                        messager.printMessage(Diagnostic.Kind.WARNING,
                                "\t\t|-methodName->" + methodName);
                        String matchName = methodName.toLowerCase();

                        if ((isSource && (matchName.startsWith(PREFIX_GET) || matchName.startsWith(PREFIX_IS)))
                                || (!isSource && matchName.startsWith(PREFIX_SET))) {
                            messager.printMessage(Diagnostic.Kind.WARNING,
                                    "\t\t\t|-match->" + matchKey + "==" + matchName);
                            if (matchName.equals(matchKey)) {
                                messager.printMessage(Diagnostic.Kind.WARNING,
                                        "\t\t\t|-matched->" + methodName);
                                String[] methodArray = getMethodArray(classIdentity, fieldIdentity);
                                methodArray[isSource ? 0 : 1] = methodName;
                                updateMethodArray(classIdentity, fieldIdentity, methodArray);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return new String[]{packageName, className};
    }

    private String[] getMethodArray(String classIdentity, String fieldIdentity) {
        Map<String, String[]> nameTree = methodTree.get(classIdentity);
        if (nameTree == null) {
            nameTree = new HashMap<>();
        }
        String[] methodArray = nameTree.get(fieldIdentity);
        if (methodArray == null) {
            methodArray = new String[2];
        }
        nameTree.put(fieldIdentity, methodArray);
        methodTree.put(classIdentity, nameTree);
        return methodArray;
    }

    private void updateMethodArray(String classIdentity, String fieldIdentity,
                                   String[] methodArray) {
        Map<String, String[]> nameTree = methodTree.get(classIdentity);
        nameTree.put(fieldIdentity, methodArray);
        methodTree.put(classIdentity, nameTree);
    }

    private void createClass(String sourcePackage, String sourceClass,
                             String productPackage, String productClass) {

        ClassName sourceName = ClassName.get(sourcePackage, sourceClass);
        ClassName productName = ClassName.get(productPackage, productClass);

        // 创建 transform 方法
        MethodSpec.Builder transformBuilder = MethodSpec.methodBuilder(METHOD_NAME);

        // 生成 transform 方法体内容
        transformBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(productName)
                .addParameter(sourceName, "source")
                .addStatement("$T product = new $T()", productName, productName);

        for (String classIdentify : methodTree.keySet()) {
            Map<String, String[]> fieldMap = methodTree.get(classIdentify);
            if (fieldMap != null) {
                for (String fieldIdentity : fieldMap.keySet()) {
                    String[] methodName = fieldMap.get(fieldIdentity);
                    transformBuilder.addStatement("product.$L(source.$L())", methodName[1],
                            methodName[0]);
                }
            }
        }

        // 生成 transform 方法 return 语句
        MethodSpec transform = transformBuilder.addStatement("return product").build();


        /**
         * public List<UserModel> transform(List<UserEntity> fromList) {
         *     if (fromList == null) return null;
         *     List<UserModel> toList = new ArrayList<>();
         *     for (UserEntity from : fromList) {
         *       UserModel to = transform(from);
         *       toList.add(to);
         *     }
         *     return toList;
         *   }
         */


        // 创建 transform list 方法
        MethodSpec.Builder transformListBuilder = MethodSpec.methodBuilder(METHOD_NAME);

        ClassName list = ClassName.get("java.util", "List");
        ClassName arrayList = ClassName.get("java.util", "ArrayList");

        // 生成 transform list 方法体内容
        MethodSpec transformList = transformListBuilder.addModifiers(Modifier.PUBLIC,
                Modifier.STATIC)
                .returns(ParameterizedTypeName.get(list, productName))
                .addParameter(ParameterizedTypeName.get(list, sourceName), "sourceList")
                .addStatement("if (sourceList == null) return null")
                .addStatement("List<$T> productList = new $T<>()", productName, arrayList)
                .beginControlFlow("for ($T source : sourceList)", sourceName)
                .addStatement("$T produce = transform(source)", productName)
                .addStatement("productList.add(produce)", productName)
                .endControlFlow()
                .addStatement("return productList")
                .build();

        // 生成 XxxConverter 类
        TypeSpec converter = TypeSpec.classBuilder(productClass + SUFFIX_CLASS)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(transform)
                .addMethod(transformList)
                .build();

        try {
            JavaFile javaFile = JavaFile.builder(productPackage + SUFFIX_PACKAGE, converter)
                    .build();
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

