package com.jeanboy.converter.compiler;


import com.jeanboy.converter.annotation.Field;
import com.jeanboy.converter.annotation.Product;
import com.jeanboy.converter.annotation.Source;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * @author caojianbo
 * @since 2020/1/16 17:59
 */
public class ConverterProcessor extends AbstractProcessor {

    private Messager messager; // 输出日志
    private Elements elements;
    private Filer filer;

    private Map<String, TypeElement[]> elementTree = new HashMap<>();
    /**
     * {
     * key: classIdentity,
     * value: {
     * key： fieldName,
     * value: fieldMark
     * }
     * }
     */
    private Map<String, Map<String, String>> fieldTree = new HashMap<>();
    /**
     * {
     * key: classIdentity
     * value: {
     * key: methodName,
     * value: [setXXX, getXXX]
     * }
     * }
     */
    private Map<String, Map<String, String[]>> methodTree = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();

        messager.printMessage(Diagnostic.Kind.WARNING, "------TransformProcessor init------");

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        messager.printMessage(Diagnostic.Kind.WARNING, "------TransformProcessor process------");
        System.out.println("===========TransformProcessor process=========");

        Set<? extends Element> fromElements = roundEnv.getElementsAnnotatedWith(Source.class);
        Set<? extends Element> toElements = roundEnv.getElementsAnnotatedWith(Product.class);

        for (TypeElement element : ElementFilter.typesIn(fromElements)) {
            if (ElementKind.CLASS == element.getKind()) {
                // 返回指定类型注解
                String value = element.getAnnotation(Source.class).value();
                TypeElement[] elements = elementTree.get(value);
                if (elements == null) {
                    elements = new TypeElement[2];
                }
                elements[0] = element;
                elementTree.put(value, elements);
            }
        }

        for (TypeElement element : ElementFilter.typesIn(toElements)) {
            if (ElementKind.CLASS == element.getKind()) {
                String value = element.getAnnotation(Product.class).value();
                TypeElement[] elements = elementTree.get(value);
                if (elements == null) {
                    elements = new TypeElement[2];
                }
                elements[1] = element;
                elementTree.put(value, elements);
            }
        }

        for (String target : elementTree.keySet()) {
            TypeElement[] elements = elementTree.get(target);
            if (elements[0] != null && elements[1] != null) {
                TypeElement source = elements[0];
                TypeElement product = elements[1];

                String[] sourceValue = parseField(target, source, true);
                String sourcePackage = sourceValue[0];
                String sourceClass = sourceValue[1];

                String[] productValue = parseField(target, product, false);
                String productPackage = productValue[0];
                String productClass = productValue[1];

                createConverter(sourcePackage, sourceClass, productPackage, productClass);
            }
        }
        return false;
    }

    private String[] parseField(String identity, TypeElement element, boolean isSource) {
        // 包名
        String packageName = "";
        // 类名
        String className = element.getSimpleName().toString();
        if (element.getKind() == ElementKind.CLASS) {
            // 返回封装此元素的元素，比如类，包等
            PackageElement packageElement = (PackageElement) element.getEnclosingElement();
            packageName = packageElement.getQualifiedName().toString();

            // 如果封装元素是类，则返回类内部定义的所有变量，方法等
            List<? extends Element> childElements = element.getEnclosedElements();
            for (Element child : childElements) {
                if (child.getKind() == ElementKind.FIELD) {
                    Field annotation = child.getAnnotation(Field.class);
                    String fieldIdentity = annotation.identity();

                    TypeMirror typeMirror = child.asType();


                    // 获取成员变量字段名
                    String fieldName = child.getSimpleName().toString();
                    messager.printMessage(Diagnostic.Kind.WARNING, "========fieldName" +
                            "====" + fieldName);
                    messager.printMessage(Diagnostic.Kind.WARNING, "========fieldIdentity" +
                            "====" + fieldIdentity);

                    Map<String, String> fieldMap = fieldTree.get(identity);
                    if (fieldMap == null) {
                        fieldMap = new HashMap<>();
                    }

                    fieldMap.put(fieldName, fieldIdentity);
                    fieldTree.put(identity, fieldMap);
                }
            }

            for (Element child : childElements) {
                if (child.getKind() == ElementKind.METHOD) {
                    String methodName = child.getSimpleName().toString();
                    messager.printMessage(Diagnostic.Kind.WARNING, "========methodName" +
                            "====" + methodName);

                    String matchName = methodName.toLowerCase();
                    Map<String, String> fieldMap = fieldTree.get(identity);
                    if (fieldMap != null) {
                        for (String fieldName : fieldMap.keySet()) {
                            String fieldIdentity = fieldMap.get(fieldName);
                            messager.printMessage(Diagnostic.Kind.WARNING,
                                    "================================fieldIdentity" +
                                            "====" + fieldIdentity);
                            String matchKey = fieldName.toLowerCase();
                            if (isSource) { // 查找 get 方法
                                if ((matchName.contains("get") && matchName.contains(matchKey))
                                        || matchKey.startsWith("is") && matchName.contains(matchKey)) {
                                    messager.printMessage(Diagnostic.Kind.WARNING,
                                            "===match=get=" + methodName);

                                    String[] methodArray = getMethodArray(identity,
                                            fieldIdentity);
                                    methodArray[0] = methodName;
                                    updateMethodArray(identity, fieldIdentity,
                                            methodArray);
                                }
                            } else { // 查找 set 方法
                                if (matchKey.startsWith("is")) {
                                    matchKey = matchKey.replace("is", "");
                                }
                                if ((matchName.contains("set") && matchName.contains(matchKey))) {
                                    messager.printMessage(Diagnostic.Kind.WARNING,
                                            "===match=set=" + methodName);

                                    String[] methodArray = getMethodArray(identity,
                                            fieldIdentity);
                                    methodArray[1] = methodName;
                                    updateMethodArray(identity, fieldIdentity,
                                            methodArray);
                                }
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

    private void createConverter(String sourcePackage, String sourceClass, String productPackage,
                                 String productClass) {

        ClassName product = ClassName.get(productPackage, productClass);


        MethodSpec.Builder transformBuilder = MethodSpec.methodBuilder("transform");

        transformBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(productPackage, productClass))
                .addParameter(ClassName.get(sourcePackage, sourceClass), "source")
                .addStatement("$T product = new $T()", product, product);

        for (String classIdentify : methodTree.keySet()) {
            messager.printMessage(Diagnostic.Kind.WARNING,
                    "=====*******************==classIdentify======" + classIdentify);
            Map<String, String[]> fieldMap = methodTree.get(classIdentify);
            if (fieldMap != null) {
                for (String fieldIdentity : fieldMap.keySet()) {
                    messager.printMessage(Diagnostic.Kind.WARNING,
                            "====************===fieldIdentity======" + fieldIdentity);
                    String[] methodName = fieldMap.get(fieldIdentity);
                    transformBuilder.addStatement("product.$L(source.$L())", methodName[1],
                            methodName[0]);
                }
            }
        }

        MethodSpec transform = transformBuilder.addStatement("return product")
                .build();

        TypeSpec converter = TypeSpec.classBuilder(productClass + "Converter")
                .addModifiers(Modifier.PUBLIC)
                .addMethod(transform)
                .build();

        try {
            JavaFile javaFile = JavaFile.builder(productPackage + ".converter", converter)
                    .build();
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

