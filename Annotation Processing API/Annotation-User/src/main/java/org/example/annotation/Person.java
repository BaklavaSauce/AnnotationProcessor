package org.example.annotation;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Person {
    private String name;
    private int height;
    private int age;
    private int id;

    public Person(){}

    @BuilderProperty
    public void setName(String name) {
        this.name = name;
    }
    @BuilderProperty
    public void setHeight(int height) {
        this.height = height;
    }
    @BuilderProperty
    public void setAge(int age) {
        this.age = age;
    }
}