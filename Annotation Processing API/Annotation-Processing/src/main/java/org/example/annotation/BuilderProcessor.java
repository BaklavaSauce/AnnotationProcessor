package org.example.annotation;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import javax.lang.model.type.ExecutableType;
import javax.tools.JavaFileObject;

import com.google.auto.service.AutoService;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("org.example.annotation.BuilderProperty")
@AutoService(Processor.class)

public class BuilderProcessor extends AbstractProcessor{
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        /*loop through the set of unique annotations for which the annotation processor is invoked during the current
        round of annotation processing
        */
        for(TypeElement annotation: annotations){
            //set of elements annotated with @annotation annotation type
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            /*partitions the @annotated elements into two groups based on a condition. The condition is defined by the lambda expression
            * true -> list of setter methods
            * false -> list of other methods
            * */
            Map<Boolean, List<Element>> annotatedMethods = annotatedElements.stream()
                    .collect(Collectors.partitioningBy(element  -> ((ExecutableType)element.asType()).getParameterTypes().size()==1 &&
                    element.getSimpleName().toString().startsWith("set")));
            List<Element> setters = annotatedMethods.get(true);
            List<Element> otherMethods = annotatedMethods.get(false);
            //otherMethods.forEach(element -> processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@BuilderProperty must be applied to a setXxx method with a single argument", element));
            if(setters.isEmpty())//if the list that contains the setters is empty, proceed by getting the next annotation
                continue;
            /* in order to build the source file of the builder class inside the target/generated-sources folder it's good practise to create
            *  the same package hierarchy as in the Annotation-User source folder. In order to do that we simply get an element from the class,
            *  and call the getEnclosingElement() and then cast the element to a type element (the cast could be
            * considered safe as the enclosing element of an ExecutableElement is a TypeElement (class, interface etc.)).
            * Once we have the TypeElement we get the full name and store it inside a string */
            String className = ((TypeElement)setters.get(0).getEnclosingElement()).getQualifiedName().toString();
            /* this map is required to create the builder methods inside the builder class
            * (key)the simple name of the method -> (value) the parameter type in string format */
            Map<String,String> setterMap  = setters.stream()
                    .collect(Collectors.toMap(setter -> setter.getSimpleName()
                            .toString(), setter -> ((ExecutableType)setter.asType()).getParameterTypes().get(0).toString()));

            /* generates the source file of the builder class. */
            try {
                writeBuilderFile(className, setterMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void writeBuilderFile(String className, Map<String, String> setterMap) throws IOException {
        String packageName = null;
        /* the variable lastDot is used to divide the package structure from the simple name of the class*/
        int lastDot = className.lastIndexOf('.');
        /* if lastDot > 0 returns true that means a package hierarchy was specified */
        if (lastDot > 0)
            packageName = className.substring(0, lastDot);

        String simpleClassName = className.substring(lastDot + 1);
        String builderClassName = className + "Builder";
        String builderSimpleClassName = builderClassName.substring(lastDot + 1);
        //create the builder file
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);
        //start writing to file
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            //package declaration
            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }
            //class declaration
            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();
            //Person object as a private field
            out.print("    private ");
            out.print(simpleClassName);
            out.print(" object = new ");
            out.print(simpleClassName);
            out.println("();");
            out.println();
            //Build method
            out.print("    public ");
            out.print(simpleClassName);
            out.println(" build() {");
            out.println("        return object;");
            out.println("    }");
            out.println();
            //builder methods
            setterMap.entrySet().forEach(setter -> {
                String methodName = setter.getKey();
                String argumentType = setter.getValue();
                out.print("    public ");
                out.print(builderSimpleClassName);
                out.print(" ");
                out.print(methodName);
                out.print("(");
                out.print(argumentType);
                out.println(" value) {");
                out.print("        object.");
                out.print(methodName);
                out.println("(value);");
                out.println("        return this;");
                out.println("    }");
                out.println();
            });
            out.println("}");
        }
    }
}
